package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.dreamcat.common.reflect.ObjectField;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    @JsonProperty("$ref")
    private String ref; // #/definitions/some_def

    private Boolean wrapped;
    @JsonProperty("default")
    private Boolean _default;
    private String example;
    @JsonProperty("enum")
    private List<String> _enum;

    public static SwaggerDefinition parse(
            ObjectType type, Swagger swagger, Function<Field, String> fieldNameGetter) {
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

        if (clazz.isEnum()) {
            List<String> consts = Arrays.stream(clazz.getEnumConstants())
                    .map(Object::toString).collect(Collectors.toList());
            definition.set_enum(consts);
            return definition;
        }

        if (swaggerType.equals(SwaggerType.array)) {
            SwaggerDefinition items;
            if (type.isArray()) {
                items = parse(type.getComponentType(), swagger, fieldNameGetter); // T[]
            } else {
                items = parse(type.getParameterType(0), swagger, fieldNameGetter); // Collection<T>
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
                    if (fieldNameGetter != null) {
                        fieldName = fieldNameGetter.apply(field);
                    }

                    SwaggerDefinition fieldDefinition = parse(objectField.getType(), swagger, fieldNameGetter);
                    properties.put(fieldName, fieldDefinition);
                }
                definition.setProperties(properties);
            }
        }

        return definition;
    }

}
