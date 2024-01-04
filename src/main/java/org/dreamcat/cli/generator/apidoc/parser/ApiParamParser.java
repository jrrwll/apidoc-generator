package org.dreamcat.cli.generator.apidoc.parser;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.Http;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentMethodDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentParameterDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.reflect.ObjectMethod;
import org.dreamcat.common.reflect.ObjectParameter;
import org.dreamcat.common.reflect.ObjectType;

/**
 * @author Jerry Will
 * @version 2024-01-04
 */
public class ApiParamParser extends BaseParser {

    final ApiDocParser apiDocParser;

    public ApiParamParser(ApiDocParser apiDocParser) {
        super(apiDocParser.config, apiDocParser.classLoader,
                apiDocParser.randomGenerator, apiDocParser.commentJavaParser);
        this.apiDocParser = apiDocParser;
    }

    ApiOutputParam parseOutputParam() {
        ObjectMethod objectMethod = ObjectMethod.fromMethod(method);
        List<ObjectParameter> objectParameters = objectMethod.getParameters();
        ObjectType returnType = objectMethod.getReturnType();
        ApiOutputParam outputParam = new ApiOutputParam();
        outputParam.setType(returnType);
        outputParam.setComment(methodDef.getReturnComment());
        // extra info
        outputParam.setJsonWithComment(toJSONWithComment(returnType));
        outputParam.setFields(apiParamFieldParser.resolveParamField(returnType));
    }

    List<ApiInputParam> parseInputParams(CommentMethodDef methodDef, List<ObjectParameter> objectParameters) {
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

            paramField.setRequired(parseParameterRequired(parameter));

            paramFields.add(paramField);
        }
        apiParam.setFields(paramFields);
        return apiParam;
    }

    private Boolean parseParameterRequired(Parameter parameter) {
        Object required = retrieveAndInvokeAnnotation(parameter, config.getHttp(),
                Http::getRequiredAnno, Http::getRequiredGetter);
        if (required != null) {
            return Objects.equals(required, true);
        }
        return isValidationRequired(parameter);
    }

    private String parseParameterPathVar(Parameter parameter) {
        Object pathVar = retrieveAndInvokeAnnotation(parameter, config.getHttp(),
                Http::getPathVarAnno, Http::getPathVarGetter);
        if (pathVar == null) return null;
        return pathVar.toString();
    }
}
