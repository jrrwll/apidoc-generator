package org.dreamcat.cli.generator.apidoc.parser;

import org.dreamcat.cli.generator.apidoc.javadoc.CommentFieldDef;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.reflect.ObjectField;
import org.dreamcat.common.reflect.ObjectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
class ApiParamFieldParser extends BaseParser {

    final CommentJavaParser commentJavaParser;

    public ApiParamFieldParser(ApiDocParser apiDocParser) {
        super(apiDocParser.config, apiDocParser.classLoader);
        this.commentJavaParser = apiDocParser.commentJavaParser;
    }

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
            if (fieldDef != null) {
                paramField.setName(fieldDef.getName());
                paramField.setComment(fieldDef.getComment());
            } else {
                paramField.setName(field.getName());
            }
            paramField.setType(field.getType());
            paramField.setRequired(isValidationRequired(field));

            List<ApiParamField> fields = resolveParamField(objectField.getType());
            paramField.setFields(fields);

            paramFields.add(paramField);
        }
        return paramFields;
    }

    public void clear() {
        paramFieldCache.clear();
    }
}
