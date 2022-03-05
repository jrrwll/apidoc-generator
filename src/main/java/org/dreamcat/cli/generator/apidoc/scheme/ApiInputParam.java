package org.dreamcat.cli.generator.apidoc.scheme;

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
    // http-like part
    private Boolean required; // required request param or not
    private String pathVar; // variable name in url path
}
