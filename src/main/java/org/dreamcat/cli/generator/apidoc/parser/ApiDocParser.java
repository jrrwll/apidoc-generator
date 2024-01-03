package org.dreamcat.cli.generator.apidoc.parser;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.Http;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.Validation;
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
import org.dreamcat.common.json.JSONWithComment;
import org.dreamcat.common.reflect.ObjectMethod;
import org.dreamcat.common.reflect.ObjectParameter;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Slf4j
public class ApiDocParser {

    final ApiDocParserConfig config;
    final ClassLoader classLoader;
    final CommentJavaParser commentJavaParser;
    final ObjectRandomGenerator randomGenerator;
    @Setter
    ApiParamFieldParser apiParamFieldParser;

    public ApiDocParser(ApiDocParserConfig config) {
        this(config, null, new ObjectRandomGenerator());
    }

    public ApiDocParser(ApiDocParserConfig config, ClassLoader classLoader, ObjectRandomGenerator randomGenerator) {
        Objects.requireNonNull(config);
        config.afterPropertySet(classLoader);
        AssertUtil.requireNonNull(randomGenerator, "randomGenerator");

        this.config = config;
        this.classLoader = classLoader;
        this.commentJavaParser = new CommentJavaParser(config);
        this.randomGenerator = randomGenerator;

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
        // extra info
        outputParam.setJsonWithComment(toJSONWithComment(returnType));
        outputParam.setFields(apiParamFieldParser.resolveParamField(returnType));
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
        if (http == null || http.getPathAnno() == null) return null;

        Annotation pathAnn = ReflectUtil.retrieveAnnotation(method, http.getPathAnno());
        Annotation basePathAnn = ReflectUtil.retrieveAnnotation(serviceType, http.getPathAnno());
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
        if (http == null || ObjectUtil.isEmpty(http.getActionAnno())) return null;

        Object action = retrieveAndInvokeAnnotation(method, http.getActionAnno(), http.getActionGetter());
        if (action != null) {
            if (action instanceof String) {
                return Collections.singletonList(action.toString());
            } else {
                return Arrays.asList((String[])action);
            }
        }

        Annotation baseActionAnn = ReflectUtil.retrieveAnnotation(serviceType, http.getActionAnno());
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
        apiParam.setJsonWithComment(toJSONWithComment(type));
        apiParam.setFields(apiParamFieldParser.resolveParamField(type));

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
        if (http != null && ObjectUtil.isNotEmpty(http.getRequiredAnno())) {
            Object required = retrieveAndInvokeAnnotation(parameter,
                    http.getRequiredAnno(), http.getRequiredGetter());
            if (required != null) {
                return Objects.equals(required, true);
            }
        }
        Validation validation = config.getValidation();
        if (config.getValidation() != null) {
            if (retrieveAnnotation(parameter, validation.getNotNullAnno()) != null ||
                    retrieveAnnotation(parameter, validation.getNotEmptyAnno()) != null ||
                    retrieveAnnotation(parameter, validation.getNotBlankAnno()) != null) {
                return true;
            }
        }
        return null;
    }

    private String parseParameterPathVar(Parameter parameter) {
        Http http = config.getHttp();
        if (http == null || ObjectUtil.isEmpty(http.getPathVarAnno())) return null;
        Object pathVar = retrieveAndInvokeAnnotation(parameter, http.getPathVarAnno(), http.getPathVarGetter());
        if (pathVar == null) return null;
        return pathVar.toString();
    }

    private String toJSONWithComment(ObjectType type) {
        ObjectRandomGenerator randomGenerator = new ObjectRandomGenerator();
        Object bean = randomGenerator.generate(type);
        return JSONWithComment.stringify(bean, commentJavaParser::provideFieldComment);
    }

    private Object retrieveAndInvokeAnnotation(AnnotatedElement parameter, String anno, String method) {
        return retrieveAndInvokeAnnotation(parameter, anno, Collections.singletonList(method));
    }

    private Object retrieveAndInvokeAnnotation(AnnotatedElement parameter, String anno, List<String> methods) {
        Annotation annoObj = retrieveAnnotation(parameter, anno);
        if (annoObj == null) return null;
        return invokeAnnotationMethod(annoObj, methods);
    }

    private Annotation retrieveAnnotation(AnnotatedElement element, String anno) {
        try {
            Class<? extends Annotation> annoClass = ReflectUtil.forNameOrThrow(anno, classLoader);
            if (element instanceof Parameter) {
                return ReflectUtil.retrieveAnnotation((Parameter) element, annoClass);
            } else if (element instanceof Method) {
                return ReflectUtil.retrieveAnnotation((Method) element, annoClass);
            } else {
                return ReflectUtil.retrieveAnnotation((Class<?>) element, annoClass);
            }
        } catch (ClassNotFoundException e) {
            if (config.isVerbose()) {
                log.error("failed to find annotation on {}: {}", element, e.getMessage());
            }
            return null;
        }
    }

    private Object invokeAnnotationMethod(Annotation anno, String method) {
        return invokeAnnotationMethod(anno, Collections.singletonList(method));
    }

    private Object invokeAnnotationMethod(Annotation anno, List<String> methods) {
        for (String method : methods) {
            Object value = ReflectUtil.invoke(anno, method);
            if (value == null) continue;
            if (value.getClass().isArray() && Array.getLength(value) == 0) {
                continue;
            }
            return value;
        }
        return null;
    }
}
