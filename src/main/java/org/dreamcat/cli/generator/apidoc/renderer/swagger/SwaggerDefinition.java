package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.reflect.ObjectField;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class SwaggerDefinition {

    private SwaggerType type;
    private SwaggerFormat format;
    private String description;
    private Map<String, SwaggerDefinition> properties;
    private SwaggerDefinition items;
    // 3.0 -> #/components/schemas/some_def
    @JsonProperty("$ref")
    private String ref; // 2.0 -> #/definitions/some_def

    private Boolean wrapped;
    @JsonProperty("default")
    private Boolean _default;
    private String example;
    @JsonProperty("enum")
    private List<String> _enum;

    public static SwaggerDefinition parse(
            ObjectType type, String comment, List<ApiParamField> paramFields, Swagger swagger) {
        if (type == null) return null;

        SwaggerDefinition definition = new SwaggerDefinition();
        SwaggerSchema schema = swagger.getTypeSchemaCache().get(type);
        if (schema != null) {
            definition.setRef(schema.getRef());
            return definition;
        }

        Class<?> clazz = type.getType();
        SwaggerType swaggerType = SwaggerType.parse(clazz);

        definition.setType(swaggerType);
        definition.setFormat(SwaggerFormat.parse(clazz));
        definition.setDescription(comment);

        if (clazz.isEnum()) {
            List<String> consts = Arrays.stream(clazz.getEnumConstants())
                    .map(Object::toString).collect(Collectors.toList());
            definition.set_enum(consts);
            return definition;
        }

        if (swaggerType.equals(SwaggerType.array)) {
            SwaggerDefinition items;
            if (type.isArray()) {
                items = parse(type.getComponentType(), null, paramFields, swagger); // T[]
            } else {
                items = parse(type.getParameterType(0), null, paramFields, swagger); // Collection<T>
            }
            definition.setItems(items);
        } else if (swaggerType.equals(SwaggerType.object) &&
                !ReflectUtil.isAssignable(Map.class, clazz)) {
            Map<Field, ObjectField> fields = type.resolveFields();
            if (fields != null) {
                Map<String, SwaggerDefinition> properties = new HashMap<>(fields.size());
                for (ObjectField objectField : fields.values()) {
                    Field field = objectField.getField();
                    String fieldName = field.getName();

                    ApiParamField paramField = null;
                    if (ObjectUtil.isNotEmpty(paramFields)) {
                        paramField = paramFields.stream()
                                .filter(it -> it.getName().equals(field.getName()))
                                .findAny().orElse(null);
                    }
                    List<ApiParamField> subParamFields = null;
                    String subComment = null;
                    if (paramField != null) {
                        fieldName = paramField.getName();
                        subParamFields = paramField.getFields();
                        subComment = paramField.getComment();
                    }

                    SwaggerDefinition fieldDefinition = parse(objectField.getType(),
                            subComment, subParamFields, swagger);
                    properties.put(fieldName, fieldDefinition);
                }
                definition.setProperties(properties);
            }
        }

        return definition;
    }

}
