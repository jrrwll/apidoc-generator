package org.dreamcat.cli.generator.apidoc.scheme;

import lombok.Data;
import org.dreamcat.common.reflect.ObjectType;

import java.util.List;

/**
 * complex type: array, object
 * value type: boolean, date, string, number
 *
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiInputParam {

    private String name;
    private String comment;
    private String fieldName; // java param/field name
    private String fieldType; // java class name
    private ObjectType type;

    private Boolean required; // required by request param or validation
    private String pathVar; // variable name in url path

    // renderer type
    private String jsonWithComment;
    private List<ApiParamField> fields; // indentedTable
    private String typeName;
}
