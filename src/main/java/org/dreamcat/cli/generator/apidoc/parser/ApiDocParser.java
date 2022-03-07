package org.dreamcat.cli.generator.apidoc.parser;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig.Http;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentClassDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentMethodDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentParameterDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.databind.type.ObjectMethod;
import org.dreamcat.databind.type.ObjectParameter;
import org.dreamcat.databind.type.ObjectType;
import org.dreamcat.databind.type.TypeUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Slf4j
public class ApiDocParser {

    private final ApiDocConfig config;

    public ApiDocParser(ApiDocConfig config) {
        Objects.requireNonNull(config);
        config.check();
        this.config = config;
    }

    public ApiDoc parse() throws Exception {
        List<String> javaFileDirs = config.getJavaFileDirs();
        List<String> srcDirs = config.getSrcDirs();

        if (config.isUseRelativeJavaFilePath()) {
            javaFileDirs = PathUtil.crossJoin(srcDirs, javaFileDirs);
        }

        ApiDoc apiDoc = new ApiDoc();
        for (String javaFileDir : javaFileDirs) {
            File dir = new File(javaFileDir);
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
                List<CommentClassDef> classDefs = CommentJavaParser.parse(file.getAbsolutePath(), srcDirs);
                if (classDefs.size() != 1) {
                    throw new IllegalArgumentException(
                            "support one class defined in one java file, but got " +
                                    classDefs.size() + " class in " + file.getAbsolutePath());
                }
                CommentClassDef classDef = classDefs.get(0);
                parseClass(classDef, apiDoc);
            }
        }
        return apiDoc;
    }

    protected void parseClass(CommentClassDef classDef, ApiDoc apiDoc) throws Exception {
        String type = classDef.getType();
        Class<?> serviceType = Class.forName(type);

        ApiGroup apiGroup = apiDoc.getGroups().computeIfAbsent(type, it -> new ApiGroup());
        apiGroup.setName(type);
        apiGroup.setComment(classDef.getComment());

        for (CommentMethodDef methodDef : classDef.getMethods()) {
            ApiFunction apiFunction = parseMethod(methodDef, serviceType);
            if (apiFunction == null) continue;

            apiGroup.getFunctions().put(methodDef.getName(), apiFunction);
        }
    }

    protected ApiFunction parseMethod(CommentMethodDef methodDef, Class<?> serviceType) {
        String methodName = methodDef.getName();
        Method method = ReflectUtil.retrieveNoStaticMethods(serviceType).stream()
                .filter(it -> it.getName().equals(methodName)).findAny().orElse(null);
        if (method == null) {
            throw new RuntimeException("method " + methodName + " is not found in " + serviceType);
        }
        if (!Modifier.isPublic(method.getModifiers())) return null; // public only

        ApiFunction apiFunction = new ApiFunction();

        ObjectMethod objectMethod = TypeUtil.fromMethod(method);
        List<ObjectParameter> objectParameters = objectMethod.getParameters();
        ObjectType returnType = objectMethod.getReturnType();
        ApiOutputParam outputParam = new ApiOutputParam();
        outputParam.setType(returnType);
        apiFunction.setOutputParam(outputParam);

        List<CommentParameterDef> parameters = methodDef.getParameters();
        int n = parameters.size();
        for (int i = 0; i < n; i++) {
            CommentParameterDef parameter = parameters.get(i);
            String parameterName = parameter.getName();
            ObjectParameter objectParameter = objectParameters.get(i);

            if (config.ignoreInputParamType(objectParameter.getType().getType().getName())) continue;
            ApiInputParam apiParam = parseInputParam(parameter, objectParameter);
            if (apiParam == null) continue;
            apiFunction.getInputParams().put(parameterName, apiParam);
        }

        apiFunction.setName(methodName);
        apiFunction.setServiceName(serviceType.getName());
        apiFunction.setComment(methodDef.getComment());

        // http-like part
        if (config.getHttp() != null) {
            apiFunction.setPath(parseMethodPath(methodDef, method, serviceType));
            apiFunction.setAction(parseMethodAction(methodDef, method, serviceType));
        }
        return apiFunction;
    }

    protected List<String> parseMethodPath(CommentMethodDef methodDef, Method method, Class<?> serviceType) {
        Http http = config.getHttp();
        if (http == null) return null;

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

    protected List<String> parseMethodAction(CommentMethodDef methodDef, Method method, Class<?> serviceType) {
        Http http = config.getHttp();
        if (http == null) return null;

        Annotation actionAnn = ReflectUtil.retrieveAnnotation(method, http.getAction());
        if (actionAnn != null) {
            List<String> action = http.getActionGetter().apply(actionAnn);
            if (action != null) return action;
        }

        Annotation baseActionAnn = ReflectUtil.retrieveAnnotation(serviceType, http.getAction());
        if (baseActionAnn == null) return null;
        return http.getActionGetter().apply(baseActionAnn);
    }

    protected ApiInputParam parseInputParam(CommentParameterDef parameter, ObjectParameter objectParameter) {
        ApiInputParam apiParam = new ApiInputParam();
        apiParam.setType(objectParameter.getType());
        apiParam.setName(parameter.getName());
        apiParam.setComment(parameter.getComment());

        // http-like part
        if (config.getHttp() != null) {
            apiParam.setRequired(parseParameterRequired(parameter, objectParameter));
            apiParam.setPathVar(parseParameterPathVar(parameter, objectParameter));
        }
        return apiParam;
    }

    protected Boolean parseParameterRequired(CommentParameterDef parameterDef, ObjectParameter objectParameter) {
        Http http = config.getHttp();
        if (http == null) return null;
        Annotation ann = ReflectUtil.retrieveAnnotation(objectParameter.getParameter(), http.getRequired());
        if (ann != null) {
            return http.getRequiredGetter().apply(ann);
        }
        return null;
    }

    protected String parseParameterPathVar(CommentParameterDef parameterDef, ObjectParameter objectParameter) {
        Http http = config.getHttp();
        if (http == null) return null;
        Annotation ann = ReflectUtil.retrieveAnnotation(objectParameter.getParameter(), http.getPathVar());
        if (ann != null) {
            return http.getPathVarGetter().apply(ann);
        }
        return null;
    }

}
