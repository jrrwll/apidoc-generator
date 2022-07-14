package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Setter;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.Swagger.Info;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.Swagger.Tag;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerParameter.In;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.common.util.ByteUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.RandomUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.databind.type.ObjectType;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Setter
public class SwaggerRenderer implements ApiDocRenderer {

    private String defaultTitle = "";
    private String defaultVersion = "";
    private String fieldNameAnnotation;
    private List<String> fieldNameAnnotationName;
    private Function<Field, String> fieldNameGetter;
    private ClassLoader classLoader;

    @Override
    public void render(ApiDoc apiDoc, Writer out) throws IOException {
        Swagger swagger = renderSwagger(apiDoc);
        String s = swagger.toYaml();
        out.write(s);
    }

    private Swagger renderSwagger(ApiDoc apiDoc) {
        afterPropertySet();
        Swagger swagger = new Swagger();

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
        String title = apiDoc.getName();
        if (ObjectUtil.isBlank(title)) title = defaultTitle;
        String version = apiDoc.getVersion();
        if (ObjectUtil.isBlank(version)) version = defaultVersion;

        Info info = new Info();
        info.setTitle(title);
        info.setVersion(version);
        info.setDescription(apiDoc.getComment());
        swagger.setInfo(info);

        swagger.setSchemes(Arrays.asList("https", "http"));
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
        swaggerPath.setConsumes(function.getConsumes());
        swaggerPath.setProduces(function.getProduces());

        List<ApiInputParam> inputParams = function.getInputParams();
        List<SwaggerParameter> parameters = new ArrayList<>(inputParams.size());
        for (ApiInputParam inputParam : inputParams) {
            SwaggerParameter parameter = formatParameter(inputParam, swagger);
            parameters.add(parameter);
        }
        swaggerPath.setParameters(parameters);

        SwaggerResponse response = formatResponse(function.getOutputParam(), swagger);
        swaggerPath.setResponses(Collections.singletonMap("200", response));

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
        parameter.setRequired(ObjectUtil.mapOrElse(inputParam.getRequired(), it -> it, true));

        SwaggerType swaggerType = SwaggerType.parse(type.getType());
        String pathVar = inputParam.getPathVar();
        if (inputParam.getRequired() != null) {
            parameter.setIn(In.query);
            parameter.setName(name);
            parameter.setType(swaggerType);
        } else if (pathVar != null) {
            parameter.setIn(In.path);
            parameter.setName(pathVar);
            parameter.setType(swaggerType);
        } else {
            parameter.setIn(In.body);
            parameter.setName(name);
            SwaggerSchema schema = swagger.getTypeSchemaCache().computeIfAbsent(
                    type, it -> formatSchema(it, swagger));
            parameter.setSchema(schema);
        }

        return parameter;
    }

    private SwaggerResponse formatResponse(ApiOutputParam outputParam, Swagger swagger) {
        SwaggerResponse response = new SwaggerResponse();

        ObjectType type = outputParam.getType();
        response.setDescription(type.getSimpleName());

        SwaggerSchema schema = swagger.getTypeSchemaCache().computeIfAbsent(
                type, it -> formatSchema(it, swagger));
        response.setSchema(schema);
        return response;
    }

    private SwaggerSchema formatSchema(ObjectType type, Swagger swagger) {
        SwaggerSchema schema = new SwaggerSchema();
        String defName = formatDefinitionName(type), defName0 = defName;

        int retry = 1;
        Map<String, SwaggerSchema> defNameSchemaCache = swagger.getDefNameSchemaCache();
        while (retry < 3 && defNameSchemaCache.containsKey(defName)) {
            defName = defName0 + retry++;
        }
        if (defNameSchemaCache.containsKey(defName)) defName = RandomUtil.uuid32();
        schema.setRef("#/definitions/" + defName);

        Map<String, SwaggerDefinition> definitions = swagger.getDefinitions();
        if (definitions == null) {
            definitions = new HashMap<>();
            swagger.setDefinitions(definitions);
        }

        SwaggerDefinition definition = SwaggerDefinition.parse(type, swagger, fieldNameGetter);
        definitions.put(defName, definition);

        return schema;
    }

    private String formatDefinitionName(ObjectType type) {
        String defName = type.getSimpleName();
        return defName.replace("<", "_")
                .replace(">", "")
                .replace(", ", "");
    }

    private void afterPropertySet() {
        if (fieldNameGetter == null &&
                ObjectUtil.isNotBlank(fieldNameAnnotation) &&
                ObjectUtil.isNotEmpty(fieldNameAnnotationName)) {
            fieldNameGetter = this::fieldNameAnnotationName;
        }
    }

    private String fieldNameAnnotationName(Field field) {
        Class<? extends Annotation> annType;
        if (classLoader == null) {
            annType = ReflectUtil.forName(
                    fieldNameAnnotation);
        } else {
            annType = ReflectUtil.forName(
                    fieldNameAnnotation, true, classLoader);
        }
        Object ann = field.getDeclaredAnnotation(annType);
        if (ann != null) {
            for (String methodName : fieldNameAnnotationName) {
                String name = (String) ReflectUtil.invoke(ann, methodName);
                if (ObjectUtil.isNotEmpty(name)) {
                    return name;
                }
            }
        }
        return field.getName(); // default use field name
    }

    public void useJacksonFieldNameGetter() {
        this.fieldNameAnnotation = "com.fasterxml.jackson.annotation.JsonProperty";
        this.fieldNameAnnotationName = Collections.singletonList("value");
    }
}
