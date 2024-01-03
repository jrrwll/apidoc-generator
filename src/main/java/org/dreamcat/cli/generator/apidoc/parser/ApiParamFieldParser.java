package org.dreamcat.cli.generator.apidoc.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.Validation;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentFieldDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.reflect.ObjectField;
import org.dreamcat.common.reflect.ObjectType;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@RequiredArgsConstructor
public class ApiParamFieldParser {

    final ApiDocParserConfig config;
    final CommentJavaParser commentJavaParser;

    private final Map<ObjectType, List<ApiParamField>> paramFieldCache = new ConcurrentHashMap<>();

    protected List<ApiParamField> resolveParamField(ObjectType type) {
        List<ApiParamField> fields = paramFieldCache.get(type);
        if (fields == null) fields = parseParamField(type);
        if (fields != null) paramFieldCache.putIfAbsent(type, fields);
        return fields;
    }

    private List<ApiParamField> parseParamField(ObjectType type) {
        Map<Field, ObjectField> fieldMap = type.resolveFields();
        if (fieldMap == null) return null;
        List<ApiParamField> paramFields = new ArrayList<>(fieldMap.size());
        for (ObjectField objectField : fieldMap.values()) {
            Field field = objectField.getField();
            CommentFieldDef fieldDef = commentJavaParser.resolveField(field);

            ApiParamField paramField = new ApiParamField();
            paramField.setName(fieldDef.getName());
            paramField.setComment(fieldDef.getComment());
            paramField.setTypeName(field.getType().getSimpleName());

            paramField.setRequired(getApiParamFieldRequired(field));

            List<ApiParamField> fields = resolveParamField(objectField.getType());
            paramField.setFields(fields);

            paramFields.add(paramField);
        }
        return paramFields;
    }

    private boolean getApiParamFieldRequired(Field field) {
        Validation validation = config.getValidation();
        if (validation != null) {
            if (field.getAnnotation(validation.getNotNullAnno()) != null ||
                    field.getAnnotation(validation.getNotEmptyAnno()) != null) {
                return true;
            }
            if (validation.getNotBlankAnno() != null) {
                // use validation 2.0+
                if (field.getAnnotation(validation.getNotBlankAnno()) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clear() {
        paramFieldCache.clear();
    }
}
