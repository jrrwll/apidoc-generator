package org.dreamcat.cli.generator.apidoc.parser;

import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FieldDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.Http;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentFieldDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentMethodDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentParameterDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.json.JSON;
import org.dreamcat.common.reflect.ObjectMethod;
import org.dreamcat.common.reflect.ObjectParameter;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.reflect.ObjectType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2024-01-04
 */
class ApiParamParser extends BaseParser {

    final ObjectRandomGenerator randomGenerator;
    final CommentJavaParser commentJavaParser;
    final ApiParamFieldParser apiParamFieldParser;

    public ApiParamParser(ApiDocParser apiDocParser, ObjectRandomGenerator randomGenerator) {
        super(apiDocParser.config, apiDocParser.classLoader);
        this.randomGenerator = randomGenerator;
        this.commentJavaParser = apiDocParser.commentJavaParser;
        this.apiParamFieldParser = new ApiParamFieldParser(apiDocParser);
    }

    public ApiOutputParam parseOutputParam(CommentMethodDef methodDef, Method method) {
        ObjectMethod objectMethod = ObjectMethod.fromMethod(method);
        ObjectType returnType = objectMethod.getReturnType();
        ApiOutputParam outputParam = new ApiOutputParam();
        outputParam.setType(returnType);
        outputParam.setComment(methodDef.getReturnComment());
        // extra info
        outputParam.setFields(apiParamFieldParser.resolveParamField(returnType));
        outputParam.setJsonWithComment(toJSONWithComment(returnType));
        return outputParam;
    }

    public List<ApiInputParam> parseInputParams(CommentMethodDef methodDef, List<ObjectParameter> objectParameters) {
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
        apiParam.setFieldName(parameter.getName());
        apiParam.setFieldType(type.getType().getName());
        apiParam.setJsonWithComment(toJSONWithComment(type));
        apiParam.setFields(apiParamFieldParser.resolveParamField(type));

        parseFieldDoc(objectParameter.getParameter(), apiParam);
        // http/validation has high priority
        apiParam.setRequired(parseParameterRequired(objectParameter.getParameter()));
        if (config.getHttp() != null) {
            apiParam.setPathVar(parseParameterPathVar(objectParameter.getParameter()));
        }
        return apiParam;
    }

    public ApiInputParam parseMergedInputParams(CommentMethodDef methodDef, List<ObjectParameter> objectParameters) {
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
            paramField.setType(parameter.getType());

            CommentParameterDef parameterDef = parameterDefMap.get(parameterName);
            if (parameterDef != null) paramField.setComment(parameterDef.getComment());

            paramField.setFields(apiParamFieldParser.resolveParamField(
                    objectParameter.getType()));

            paramField.setRequired(parseParameterRequired(parameter));

            paramFields.add(paramField);
        }
        apiParam.setFields(paramFields);
        return apiParam;
    }

    private Boolean parseParameterRequired(Parameter parameter) {
        Object required = findAndInvokeAnno(parameter, config.getHttp(),
                Http::getRequired, Http::getRequiredMethod);
        if (required != null) {
            return Objects.equals(required, true);
        }
        return isValidationRequired(parameter);
    }

    private String parseParameterPathVar(Parameter parameter) {
        Object pathVar = findAndInvokeAnno(parameter, config.getHttp(),
                Http::getPathVar, Http::getPathVarMethod);
        if (pathVar == null) return null;
        return pathVar.toString();
    }

    private void parseFieldDoc(Parameter parameter, ApiInputParam apiParam) {
        for (FieldDoc fieldDoc : config.getFieldDoc()) {
            Object annoObj = findAnno(parameter, fieldDoc.getName());
            if (annoObj == null) continue;

            Object name = invokeAnno(annoObj, fieldDoc.getNameMethod());
            if (name != null) apiParam.setName(name.toString());
            Object comment = invokeAnno(annoObj, fieldDoc.getCommentMethod());
            if (comment != null) apiParam.setComment(comment.toString());
            Object required = invokeAnno(annoObj, fieldDoc.getRequiredMethod());
            if (required != null) apiParam.setRequired(Objects.equals(required, true));
        }
    }

    private String toJSONWithComment(ObjectType type) {
        try {
            Object bean = randomGenerator.generate(type);
            return JSON.stringifyWithComment(bean, this::provideFieldComment);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String provideFieldComment(Field field) {
        CommentFieldDef fieldDef = commentJavaParser.resolveField(field);
        return fieldDef != null ? fieldDef.getComment() : null;
    }
}
