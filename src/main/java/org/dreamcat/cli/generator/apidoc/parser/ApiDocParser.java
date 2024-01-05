package org.dreamcat.cli.generator.apidoc.parser;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.Http;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentClassDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentMethodDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentParameterDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.reflect.ObjectMethod;
import org.dreamcat.common.reflect.ObjectParameter;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Slf4j
public class ApiDocParser extends BaseParser {

    final ApiParamParser apiParamParser;

    public ApiDocParser(ApiDocParserConfig config) {
        this(config, null, new ObjectRandomGenerator());
    }

    public ApiDocParser(ApiDocParserConfig config, ClassLoader classLoader, ObjectRandomGenerator randomGenerator) {
        super(config, classLoader, randomGenerator);
        // this.config.afterPropertySet(classLoader);
        this.apiParamParser = new ApiParamParser(this);
    }

    @SneakyThrows
    public ApiDoc parse() {
        List<String> javaFileDirs = config.getJavaFileDirs();
        List<String> srcDirs = config.getSrcDirs();

        String userDir = System.getProperty("user.dir");
        boolean relative = config.isUseRelativeJavaFilePath();
        if (relative) {
            javaFileDirs = PathUtil.crossJoin(srcDirs, javaFileDirs);
        }

        ApiDoc apiDoc = new ApiDoc();
        Map<String, ApiGroup> groupMap = new LinkedHashMap<>();
        for (String javaFileDir : javaFileDirs) {
            File dir = relative ? new File(userDir, javaFileDir) : new File(javaFileDir);
            File[] files;
            if (!dir.exists() || ObjectUtil.isEmpty(files = dir.listFiles())) {
                log.warn("no java files in {}", dir.getAbsolutePath());
                continue;
            }

            for (File file : files) {
                if (!file.isFile() || !file.getName().endsWith(".java")) {
                    log.info("skip file {} since not a java file", file.getAbsolutePath());
                    continue;
                }
                List<CommentClassDef> classDefs = CommentClassDef.parse(file.getCanonicalPath(), srcDirs);
                if (classDefs.size() != 1) {
                    throw new IllegalArgumentException(
                            "support one class defined in one java file, but got " +
                                    classDefs.size() + " class in " + file.getAbsolutePath());
                }
                CommentClassDef classDef = classDefs.get(0);
                parseClass(classDef, groupMap);
            }
        }
        apiDoc.setGroups(new ArrayList<>(groupMap.values()));
        return apiDoc;
    }

    private void parseClass(CommentClassDef classDef, Map<String, ApiGroup> groupMap) {
        String type = classDef.getType();
        Class<?> serviceType = ReflectUtil.forName(type, classLoader);

        ApiGroup apiGroup = groupMap.computeIfAbsent(type, it -> new ApiGroup());
        apiGroup.setName(type);
        apiGroup.setComment(classDef.getComment());

        for (CommentMethodDef methodDef : classDef.getMethods()) {
            ApiFunction apiFunction = parseMethod(methodDef, serviceType);
            if (apiFunction == null) continue;

            apiGroup.getFunctions().add(apiFunction);
        }
    }

    private ApiFunction parseMethod(CommentMethodDef methodDef, Class<?> serviceType) {
        String methodName = methodDef.getName();
        Method method = ReflectUtil.retrieveNoStaticMethods(serviceType).stream()
                .filter(it -> it.getName().equals(methodName)).findAny().orElse(null);
        if (method == null) {
            throw new RuntimeException("method " + methodName + " is not found in " + serviceType);
        }
        if (!Modifier.isPublic(method.getModifiers())) return null; // public only

        ApiFunction apiFunction = new ApiFunction();
        apiFunction.setName(methodName);
        apiFunction.setServiceName(serviceType.getName());
        apiFunction.setComment(methodDef.getComment());

        if (ObjectUtil.isNotEmpty(config.getHttp())) {
            apiFunction.setPath(parseMethodPath(method, serviceType));
            apiFunction.setAction(parseMethodAction(method, serviceType));
        }

        // output
        ApiOutputParam outputParam = apiParamParser.parseOutputParam(methodDef, method);
        apiFunction.setOutputParam(outputParam);

        // input
        ObjectMethod objectMethod = ObjectMethod.fromMethod(method);
        List<ObjectParameter> objectParameters = objectMethod.getParameters();
        boolean inputParamsMerged = config.needMergeInputParam(serviceType, method);
        apiFunction.setInputParamsMerged(inputParamsMerged);
        if (inputParamsMerged) {
            ApiInputParam merged = apiParamParser.parseMergedInputParams(methodDef, objectParameters);
            apiFunction.setInputParams(new ArrayList<>(Collections.singletonList(merged)));
        } else {
            apiFunction.setInputParams(apiParamParser.parseInputParams(methodDef, objectParameters));
        }
        apiFunction.setInputParamCount(apiFunction.getInputParams().size());

        // handle extra anno
        if (ObjectUtil.isNotEmpty(config.getFunctionAnno())) {

        }
        return apiFunction;
    }

    private List<String> parseMethodPath(Method method, Class<?> serviceType) {
        List<Http> http = config.getHttp();
        Object pathAnn = retrieveAndInvokeAnnotation(method, http, Http::getPathAnno, Http::getPathGetter);
        Object basePathAnn = retrieveAndInvokeAnnotation(serviceType, http, Http::getPathAnno, Http::getPathGetter);

        List<String> path = null, basePath = null;
        if (pathAnn != null) path = annoValueToStringList(pathAnn);
        if (basePathAnn != null) basePath = annoValueToStringList(basePathAnn);

        if (basePath != null && path != null) {
            return PathUtil.crossJoin(basePath, path);
        } else if (basePath != null) {
            return PathUtil.normalize(basePath);
        } else if (path != null) {
            return PathUtil.normalize(path);
        } else {
            return null;
        }
    }

    private List<String> parseMethodAction(Method method, Class<?> serviceType) {
        List<Http> http = config.getHttp();
        Object action = retrieveAndInvokeAnnotation(method, http,
                Http::getActionAnno, Http::getActionGetter);
        if (action != null) {
            return annoValueToStringList(action);
        }

        Object baseAction = retrieveAndInvokeAnnotation(serviceType, http,
                Http::getActionAnno, Http::getActionGetter);
        if (baseAction == null) return null;
        return annoValueToStringList(baseAction);
    }
}
