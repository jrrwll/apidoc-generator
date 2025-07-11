package org.dreamcat.cli.generator.apidoc.parser;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FunctionDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.Http;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentClassDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentMethodDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.common.Pair;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.reflect.ObjectMethod;
import org.dreamcat.common.reflect.ObjectParameter;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Slf4j
public class ApiDocParser extends BaseParser {

    final CommentJavaParser commentJavaParser;
    final ApiParamParser apiParamParser;

    public ApiDocParser(ApiDocParseConfig config) {
        this(config, (ClassLoader) null);
    }

    public ApiDocParser(ApiDocParseConfig config, ClassLoader classLoader) {
        this(config, classLoader, new ObjectRandomGenerator());
    }

    public ApiDocParser(ApiDocParseConfig config, ObjectRandomGenerator randomGenerator) {
        this(config, null, randomGenerator);
    }

    public ApiDocParser(ApiDocParseConfig config, ClassLoader classLoader,
            ObjectRandomGenerator randomGenerator) {
        super(config, classLoader);
        this.config.afterPropertySet(classLoader);
        this.commentJavaParser = new CommentJavaParser(config);
        this.apiParamParser = new ApiParamParser(this, randomGenerator);
    }

    @SneakyThrows
    public ApiDoc parse() {
        List<String> javaFileDirs = config.getJavaFileDirs();
        List<String> srcDirs = config.getExistingSrcDirs();

        List<String> fileDirs = new ArrayList<>();
        for (String javaFileDir : javaFileDirs) {
            if (javaFileDir.startsWith("/")) {
                fileDirs.add(javaFileDir);
            } else {
                fileDirs.addAll(PathUtil.crossJoin(srcDirs, Collections.singletonList(javaFileDir)));
            }
        }

        fileDirs = fileDirs.stream().filter(javaFileDir -> {
            File dir = new File(javaFileDir);
            if (!dir.exists()) {
                log.warn("java dir not found: {}", dir.getAbsolutePath());
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        ApiDoc apiDoc = new ApiDoc();
        if (fileDirs.isEmpty()) {
            log.warn("no any existing java dir found in your `javaFileDirs`");
            return apiDoc;
        } else if (config.isVerbose()){
            log.info("real javaFileDirs: " + fileDirs);
        }

        Map<String, ApiGroup> groupMap = new LinkedHashMap<>();
        for (String javaFileDir : fileDirs) {
            File dir = new File(javaFileDir);
            File[] files;
            if (dir.isFile()) {
                files = new File[]{dir};
            } else if (ObjectUtil.isEmpty(files = dir.listFiles())) {
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
        if (ObjectUtil.isNotEmpty(config.getFunctionDoc())) {
            parseFunctionDoc(method, apiFunction);
        }
        return apiFunction;
    }

    private List<String> parseMethodPath(Method method, Class<?> serviceType) {
        List<Http> http = config.getHttp();
        Object pathAnn = findAndInvokeAnno(method, http, Http::getPath, Http::getPathMethod);
        Object basePathAnn = findAndInvokeAnno(serviceType, http, Http::getPath, Http::getPathMethod);

        List<String> path = null, basePath = null;
        if (pathAnn != null) path = annoValueToStrs(pathAnn);
        if (basePathAnn != null) basePath = annoValueToStrs(basePathAnn);

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
        Object action = findAndInvokeAnno(method, http,
                Http::getAction, Http::getActionMethod);
        if (action != null) {
            return annoValueToStrs(action);
        }

        Object baseAction = findAndInvokeAnno(serviceType, http,
                Http::getAction, Http::getActionMethod);
        if (baseAction == null) return null;
        return annoValueToStrs(baseAction);
    }

    private void parseFunctionDoc(Method method, ApiFunction apiFunction) {
        for (FunctionDoc funDoc : config.getFunctionDoc()) {
            Object annoObj = findAnno(method, funDoc.getName());
            if (annoObj == null) continue;
            // function comment
            Object comment = invokeAnno(annoObj, funDoc.getCommentMethod());
            if (comment != null) {
                apiFunction.setComment(comment.toString());
            }

            Object paramsObj = invokeAnno(annoObj, funDoc.getNestedParamMethod());
            if (paramsObj == null) continue;
            Object[] params = paramsObj instanceof Object[] ?
                    (Object[]) paramsObj : new Object[]{paramsObj};
            Map<String, Pair<Object, Object>> paramMap = new HashMap<>();
            for (Object param : params) {
                Object paramName = invokeAnno(param, funDoc.getNestedParamNameMethod());
                if (paramName == null) continue;
                Object paramComment = invokeAnno(param, funDoc.getNestedParamCommentMethod());
                Object paramRequired = invokeAnno(param, funDoc.getNestedParamRequiredMethod());
                if (paramComment != null || paramRequired != null) {
                    paramMap.put(paramName.toString(), Pair.of(paramComment, paramRequired));
                }
            }
            if (paramMap.isEmpty()) continue;
            for (ApiInputParam inputParam : apiFunction.getInputParams()) {
                Pair<Object, Object> pair = paramMap.get(inputParam.getName());
                if (pair == null) continue;
                if (pair.hasFirst()) {
                    inputParam.setComment(pair.first().toString());
                }
                if (pair.hasSecond()) {
                    inputParam.setRequired(Objects.equals(pair.second(), true));
                }
            }
        }
    }
}
