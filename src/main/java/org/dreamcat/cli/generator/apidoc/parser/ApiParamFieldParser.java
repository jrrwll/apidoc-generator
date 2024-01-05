package org.dreamcat.cli.generator.apidoc.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentFieldDef;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.reflect.ObjectField;
import org.dreamcat.common.reflect.ObjectType;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
class ApiParamFieldParser extends BaseParser {

    final ApiDocParser apiDocParser;

    public ApiParamFieldParser(ApiDocParser apiDocParser) {
        super(apiDocParser.config, apiDocParser.classLoader);
        this.apiDocParser = apiDocParser;
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
            CommentFieldDef fieldDef = apiDocParser.commentJavaParser.resolveField(field);

            ApiParamField paramField = new ApiParamField();
            paramField.setName(fieldDef.getName());
            paramField.setComment(fieldDef.getComment());
            paramField.setTypeName(field.getType().getSimpleName());
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
