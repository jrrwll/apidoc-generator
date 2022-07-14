package org.dreamcat.cli.generator.apidoc.parser;

import java.io.File;
import java.lang.annotation.Annotation;
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
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig.Http;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig.Validation;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentClassDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentMethodDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentParameterDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.databind.instance.RandomInstance;
import org.dreamcat.databind.json.legacy.JSONWithComment;
import org.dreamcat.databind.type.ObjectMethod;
import org.dreamcat.databind.type.ObjectParameter;
import org.dreamcat.databind.type.ObjectType;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Slf4j
public class ApiDocParser {

    final ApiDocConfig config;
    final ClassLoader classLoader;
    final CommentJavaParser commentJavaParser;
    final RandomInstance randomInstance;
    @Setter
    ApiParamFieldParser apiParamFieldParser;

    public ApiDocParser(ApiDocConfig config) {
        this(config, null, new RandomInstance());
    }

    public ApiDocParser(ApiDocConfig config, ClassLoader classLoader, RandomInstance randomInstance) {
        Objects.requireNonNull(config);
        config.afterPropertySet(classLoader);
        ObjectUtil.requireNotNull(randomInstance, "randomInstance");

        this.config = config;
        this.classLoader = classLoader;
        this.commentJavaParser = new CommentJavaParser(config);
        this.randomInstance = randomInstance;

        this.apiParamFieldParser = new ApiParamFieldParser(config, commentJavaParser);
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
        Class<?> serviceType = ReflectUtil.forName(type, true, classLoader);

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

        if (config.getHttp() != null) {
            apiFunction.setPath(parseMethodPath(methodDef, method, serviceType));
            apiFunction.setAction(parseMethodAction(methodDef, method, serviceType));
        }

        // output
        ObjectMethod objectMethod = ObjectMethod.fromMethod(method);
        List<ObjectParameter> objectParameters = objectMethod.getParameters();
        ObjectType returnType = objectMethod.getReturnType();
        ApiOutputParam outputParam = new ApiOutputParam();
        outputParam.setType(returnType);
        outputParam.setComment(methodDef.getReturnComment());
        if (config.isUseJsonWithComment()) {
            ObjectType type = outputParam.getType();
            outputParam.setJsonWithComment(toJSONWithComment(type));
        }
        if (config.isUseIndentedTable()) {
            ObjectType type = outputParam.getType();
            outputParam.setFields(apiParamFieldParser.resolveParamField(type));
        }
        apiFunction.setOutputParam(outputParam);

        // input
        boolean inputParamsMerged = config.needMergeInputParam(serviceType, method);
        apiFunction.setInputParamsMerged(inputParamsMerged);
        if (inputParamsMerged) {
            ApiInputParam merged = parseMergedInputParams(methodDef, objectParameters);
            apiFunction.setInputParams(new ArrayList<>(Collections.singletonList(merged)));
        } else {
            apiFunction.setInputParams(parseInputParams(methodDef, objectParameters));
        }
        apiFunction.setInputParamCount(apiFunction.getInputParams().size());
        return apiFunction;
    }

    private List<String> parseMethodPath(CommentMethodDef methodDef, Method method, Class<?> serviceType) {
        Http http = config.getHttp();
        if (http == null || http.getPath() == null) return null;

        Annotation pathAnn = ReflectUtil.retrieveAnnotation(method, http.getPath());
        Annotation basePathAnn = ReflectUtil.retrieveAnnotation(serviceType, http.getPath());
        List<String> path = null, basePath = null;
        if (pathAnn != null) path = http.getPathGetter().apply(pathAnn);
        if (basePathAnn != null) basePath = http.getPathGetter().apply(basePathAnn);

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

    private List<String> parseMethodAction(CommentMethodDef methodDef, Method method, Class<?> serviceType) {
        Http http = config.getHttp();
        if (http == null || http.getAction() == null) return null;

        Annotation actionAnn = ReflectUtil.retrieveAnnotation(method, http.getAction());
        if (actionAnn != null) {
            List<String> action = http.getActionGetter().apply(actionAnn);
            if (action != null) return action;
        }

        Annotation baseActionAnn = ReflectUtil.retrieveAnnotation(serviceType, http.getAction());
        if (baseActionAnn == null) return null;
        return http.getActionGetter().apply(baseActionAnn);
    }

    private List<ApiInputParam> parseInputParams(CommentMethodDef methodDef, List<ObjectParameter> objectParameters) {
        List<CommentParameterDef> parameters = methodDef.getParameters();
        int n = parameters.size();
        List<ApiInputParam> inputParams = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            CommentParameterDef parameter = parameters.get(i);
            ObjectParameter objectParameter = objectParameters.get(i);

            if (config.ignoreInputParamType(objectParameter.getType().getType().getName())) continue;
            ApiInputParam apiParam = parseInputParam(parameter, objectParameter);
            inputParams.add(apiParam);
        }
        return inputParams;
    }

    private ApiInputParam parseInputParam(CommentParameterDef parameter, ObjectParameter objectParameter) {
        ApiInputParam apiParam = new ApiInputParam();
        ObjectType type = objectParameter.getType();
        apiParam.setType(type);
        apiParam.setTypeName(type.getType().getSimpleName());
        apiParam.setName(parameter.getName());
        apiParam.setComment(parameter.getComment());
        if (config.isUseJsonWithComment()) {
            apiParam.setJsonWithComment(toJSONWithComment(type));
        }
        if (config.isUseIndentedTable()) {
            apiParam.setFields(apiParamFieldParser.resolveParamField(type));
        }

        apiParam.setRequired(parseParameterRequired(objectParameter.getParameter()));

        if (config.getHttp() != null) {
            apiParam.setPathVar(parseParameterPathVar(objectParameter.getParameter()));
        }
        return apiParam;
    }

    private ApiInputParam parseMergedInputParams(CommentMethodDef methodDef, List<ObjectParameter> objectParameters) {
        ApiInputParam apiParam = new ApiInputParam();
        apiParam.setName("<param>");
        apiParam.setTypeName("<anonymous>"); // merged type

        List<ApiParamField> paramFields = new ArrayList<>();
        Map<String, CommentParameterDef> parameterDefMap = methodDef.getParameters()
                .stream().collect(Collectors.toMap(CommentParameterDef::getName, a -> a));
        for (ObjectParameter objectParameter : objectParameters) {
            Parameter parameter = objectParameter.getParameter();
            if (config.ignoreInputParamType(parameter.getType().getName())) continue;
            String parameterName = parameter.getName();
            ApiParamField paramField = new ApiParamField();
            paramField.setName(parameterName);
            paramField.setTypeName(parameter.getType().getSimpleName());

            CommentParameterDef parameterDef = parameterDefMap.get(parameterName);
            if (parameterDef != null) paramField.setComment(parameterDef.getComment());

            paramField.setFields(apiParamFieldParser.resolveParamField(
                    objectParameter.getType()));

            paramField.setRequired(Objects.equals(true, parseParameterRequired(parameter)));

            paramFields.add(paramField);
        }
        apiParam.setFields(paramFields);
        return apiParam;
    }

    private Boolean parseParameterRequired(Parameter parameter) {
        Http http = config.getHttp();
        if (http != null && http.getRequired() != null) {
            Annotation ann = ReflectUtil.retrieveAnnotation(parameter, http.getRequired());
            if (ann != null) {
                return http.getRequiredGetter().apply(ann);
            }
        }
        Validation validation = config.getValidation();
        if (config.getValidation() != null) {
            if (ReflectUtil.retrieveAnnotation(parameter, validation.getNotNull()) != null ||
                    ReflectUtil.retrieveAnnotation(parameter, validation.getNotEmpty()) != null ||
                    ReflectUtil.retrieveAnnotation(parameter, validation.getNotBlank()) != null) {
                return true;
            }
        }
        return null;
    }

    private String parseParameterPathVar(Parameter parameter) {
        Http http = config.getHttp();
        if (http == null || http.getPathVar() == null) return null;
        Annotation ann = ReflectUtil.retrieveAnnotation(parameter, http.getPathVar());
        if (ann != null) {
            return http.getPathVarGetter().apply(ann);
        }
        return null;
    }

    private String toJSONWithComment(ObjectType type) {
        Object bean = randomInstance.randomValue(type);
        return JSONWithComment.stringify(bean, commentJavaParser::provideFieldComment);
    }
}
