package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.List;
import lombok.Data;
import org.dreamcat.databind.type.ObjectType;

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
    private ObjectType type;

    private Boolean required; // required by request param or validation
    private String pathVar; // variable name in url path

    // renderer type
    private String jsonWithComment;
    private List<ApiParamField> fields; // indentedTable
    private String typeName;
}
