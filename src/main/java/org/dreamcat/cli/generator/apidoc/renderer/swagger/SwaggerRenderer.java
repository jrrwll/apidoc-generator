package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.HttpPushConfig;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.Swagger.Components;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.Swagger.Info;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.Swagger.Tag;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerParameter.In;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerPath.SwaggerRequestBody;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.util.ByteUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.RandomUtil;
import org.dreamcat.common.util.StringUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(Include.NON_EMPTY)
public class SwaggerRenderer implements ApiDocRenderer {

    private boolean swagger2; // use swagger 2.0 rather than openapi 3.0
    private String defaultTitle = "Swagger Doc";
    private String defaultVersion = "0.1";
    private HttpPushConfig httpPush;
    private boolean formatAsJson; // json or yaml, default is yaml

    @JsonIgnore
    @Override
    public String getOutputFileSuffix() {
        if (formatAsJson) {
            return "json";
        } else {
            return "yaml";
        }
    }

    @Override
    public String render(ApiDoc doc) throws IOException {
        Swagger swagger = renderSwagger(doc);
        String s;
        if (formatAsJson) {
            s = JsonUtil.toJson(swagger);
        } else {
            s = YamlUtil.toJson(swagger);
        }

        if (httpPush != null) {
            Map<String, String> context = new HashMap<>();
            String swaggerJson = JsonUtil.toJson(swagger);

            context.put("swagger", s);
            context.put("swaggerJson", swaggerJson);
            context.put("swaggerJsonEscaped", StringUtil.escape(swaggerJson, '"'));
            httpPush.pushDoc(context);
        }
        return s;
    }

    private Swagger renderSwagger(ApiDoc apiDoc) {
        Swagger swagger = new Swagger();
        if (swagger2) {
            swagger.setSwagger(Swagger.SWAGGER_VERSION);
        } else {
            swagger.setOpenapi(Swagger.OPENAPI_VERSION);
        }
        renderInfo(apiDoc, swagger);

        List<Tag> tags = new ArrayList<>();
        swagger.setTags(tags);

        swagger.setPaths(new HashMap<>());
        List<ApiGroup> groups = apiDoc.getGroups();
        for (ApiGroup group : groups) {
            String tagName = group.getName();

            Tag tag = new Tag();
            tag.setName(tagName);
            tag.setDescription(group.getComment());
            tags.add(tag);

            List<ApiFunction> functions = group.getFunctions();
            for (ApiFunction function : functions) {
                renderFunction(function, tagName, swagger);
            }
        }

        return swagger;
    }

    private void renderInfo(ApiDoc apiDoc, Swagger swagger) {
        String title = FunctionUtil.firstNotNull(apiDoc.getName(), defaultTitle);
        String version = FunctionUtil.firstNotNull(apiDoc.getVersion(), defaultVersion);

        Info info = new Info();
        info.setTitle(title);
        info.setVersion(version);
        info.setDescription(apiDoc.getComment());
        swagger.setInfo(info);

        if (swagger2) {
            swagger.setSchemes(Arrays.asList("http", "https"));
        }
    }

    private void renderFunction(ApiFunction function, String tagName,
            Swagger swagger) {
        List<String> paths = function.getPath();
        if (ObjectUtil.isEmpty(paths)) {
            String path = function.getServiceName() + "#" + function.getName();
            paths = Collections.singletonList(path);
        }
        List<String> actions = function.getAction();
        if (ObjectUtil.isEmpty(actions)) {
            actions = Collections.singletonList(SwaggerMethod.post.name());
        }

        Map<String, Map<SwaggerMethod, SwaggerPath>> swaggerPaths = new HashMap<>();
        for (String path : paths) {
            // path = path.replaceFirst("\\{(.*?)}", "\\$$1");
            Map<SwaggerMethod, SwaggerPath> swaggerPathMap = swaggerPaths.computeIfAbsent(
                    path, it -> new HashMap<>());
            for (String action : actions) {
                SwaggerPath swaggerPath = formatFunction(function, tagName, action, path, swagger);
                swaggerPathMap.put(SwaggerMethod.valueOf(action.toLowerCase()), swaggerPath);
            }
        }
        swagger.getPaths().putAll(swaggerPaths);
    }

    private SwaggerPath formatFunction(ApiFunction function, String tagName,
            String action, String path, Swagger swagger) {
        SwaggerPath swaggerPath = new SwaggerPath();
        swaggerPath.setTags(Collections.singletonList(tagName));
        swaggerPath.setDescription(function.getComment());

        String operationId = formatOperationId(function, action, path);
        swaggerPath.setOperationId(operationId);

        List<ApiInputParam> inputParams = function.getInputParams();
        List<SwaggerParameter> parameters = new ArrayList<>();
        if (function.isInputParamsMerged()) {
            List<ApiParamField> fields = inputParams.get(0).getFields();
            for (ApiParamField field : fields) {
                SwaggerParameter parameter = formatParameter(field, swagger);
                // openapi 3.0 case
                if (!swagger2 && parameter.getIn() == SwaggerParameter.In.body) {
                    SwaggerRequestBody requestBody = formatRequestBody(parameter);
                    swaggerPath.setRequestBody(requestBody);
                    continue;
                }
                parameters.add(parameter);
            }
        } else {
            for (ApiInputParam inputParam : inputParams) {
                SwaggerParameter parameter = formatParameter(inputParam, swagger);
                // openapi 3.0 case
                if (!swagger2 && parameter.getIn() == SwaggerParameter.In.body) {
                    SwaggerRequestBody requestBody = formatRequestBody(parameter);
                    swaggerPath.setRequestBody(requestBody);
                    continue;
                }
                parameters.add(parameter);
            }
        }
        swaggerPath.setParameters(parameters);

        SwaggerResponse response = formatResponse(function.getOutputParam(), swagger);
        swaggerPath.setResponses(Collections.singletonMap("200", response));

        if (swagger2) {
            swaggerPath.setConsumes(function.getConsumes());
            swaggerPath.setProduces(function.getProduces());
        }
        return swaggerPath;
    }

    private String formatOperationId(ApiFunction function, String action, String path) {
        if (ObjectUtil.isBlank(path)) {
            return action + "_" + function.getName();
        }
        return action + "_" + function.getName() + "_" + ByteUtil.hex(path.getBytes());
    }

    private SwaggerParameter formatParameter(
            ApiInputParam inputParam, Swagger swagger) {
        String name = inputParam.getName();
        ObjectType type = inputParam.getType();

        SwaggerParameter parameter = new SwaggerParameter();
        parameter.setDescription(inputParam.getComment());
        parameter.setRequired(FunctionUtil.mapOrElse(inputParam.getRequired(), it -> it, true));

        SwaggerType swaggerType = SwaggerType.parse(type.getType());
        String pathVar = inputParam.getPathVar();
        if (inputParam.getRequired() != null) {
            parameter.setIn(In.query);
            parameter.setName(name);
            if (swagger2) {
                parameter.setType(swaggerType);
            } else {
                SwaggerSchema schema = new SwaggerSchema();
                schema.setType(swaggerType);
                parameter.setSchema(schema);
            }
        } else if (pathVar != null) {
            parameter.setIn(In.path);
            parameter.setName(pathVar);
            if (swagger2) {
                parameter.setType(swaggerType);
            } else {
                SwaggerSchema schema = new SwaggerSchema();
                schema.setType(swaggerType);
                parameter.setSchema(schema);
            }
        } else {
            parameter.setIn(In.body);
            parameter.setName(name);
            SwaggerSchema schema = swagger.getTypeSchemaCache().computeIfAbsent(
                    type, it -> formatSchema(it, inputParam.getComment(), inputParam.getFields(), swagger));
            parameter.setSchema(schema);
        }

        return parameter;
    }

    private SwaggerParameter formatParameter(ApiParamField field, Swagger swagger) {
        SwaggerParameter parameter = new SwaggerParameter();
        parameter.setName(field.getName());
        parameter.setDescription(field.getComment());
        parameter.setRequired(field.getRequired());

        ObjectType type = field.getType();
        if (swagger2) {
            parameter.setType(SwaggerType.parse(type.getType()));
        }

        if (ObjectUtil.isEmpty(field.getFields())) {
            parameter.setIn(In.query);
            if(SwaggerType.array.equals(parameter.getType())) {
                SwaggerType arrayType = SwaggerType.parse(type.getParameterType(0).getType());
                SwaggerDefinition items = new SwaggerDefinition();
                items.setType(arrayType);
                parameter.setItems(items);
            }
            if (!swagger2) {
                SwaggerSchema schema = new SwaggerSchema();
                schema.setType(SwaggerType.parse(type.getType()));
                parameter.setSchema(schema);
            }
        } else {
            parameter.setIn(In.body);
            if (!swagger2) {
                SwaggerSchema schema = swagger.getTypeSchemaCache().computeIfAbsent(
                        type, it -> formatSchema(it, field.getComment(), field.getFields(), swagger));
                parameter.setSchema(schema);
            }
        }
        return parameter;
    }

    private SwaggerRequestBody formatRequestBody(SwaggerParameter parameter) {
        SwaggerRequestBody requestBody = new SwaggerRequestBody();
        requestBody.setDescription(parameter.getDescription());
        SwaggerContent content = new SwaggerContent();
        content.setSchema(parameter.getSchema());
        requestBody.setContent(Collections.singletonMap("application/json", content));
        return requestBody;
    }

    private SwaggerResponse formatResponse(ApiOutputParam outputParam, Swagger swagger) {
        SwaggerResponse response = new SwaggerResponse();

        ObjectType type = outputParam.getType();
        response.setDescription(type.getSimpleName());

        SwaggerSchema schema = swagger.getTypeSchemaCache().computeIfAbsent(
                type, it -> formatSchema(it, outputParam.getComment(), outputParam.getFields(), swagger));
        if (swagger2) {
            response.setSchema(schema);
        } else {
            SwaggerContent content = new SwaggerContent();
            content.setSchema(schema);
            response.setContent(Collections.singletonMap("application/json", content));
        }
        return response;
    }

    private SwaggerSchema formatSchema(ObjectType type, String comment, List<ApiParamField> paramFields, Swagger swagger) {
        SwaggerSchema schema = new SwaggerSchema();
        String defName = formatDefinitionName(type), defName0 = defName;

        int retry = 1;
        Map<String, SwaggerSchema> defNameSchemaCache = swagger.getDefNameSchemaCache();
        while (retry < 3 && defNameSchemaCache.containsKey(defName)) {
            defName = defName0 + retry++;
        }
        if (defNameSchemaCache.containsKey(defName)) defName = RandomUtil.uuid32();
        String refPrefix = "#/components/schemas/";
        if (swagger2) {
            refPrefix = "#/definitions/";
        }
        schema.setRef(refPrefix + defName);

        Map<String, SwaggerDefinition> definitions;
        if (swagger2) {
            definitions = swagger.getDefinitions();
            if (definitions == null) {
                definitions = new HashMap<>();
                swagger.setDefinitions(definitions);
            }
        } else {
            Components components = swagger.getComponents();
            if (components == null) {
                components = new Components();
                swagger.setComponents(components);
                definitions = new HashMap<>();
                components.setSchemas(definitions);
            } else {
                definitions = components.getSchemas();
            }
        }

        SwaggerDefinition definition = SwaggerDefinition.parse(type, comment, paramFields, swagger);
        definitions.put(defName, definition);

        return schema;
    }

    private String formatDefinitionName(ObjectType type) {
        String defName = type.getSimpleName();
        return defName.replace("<", "_")
                .replace(">", "")
                .replace(", ", "");
    }
}
