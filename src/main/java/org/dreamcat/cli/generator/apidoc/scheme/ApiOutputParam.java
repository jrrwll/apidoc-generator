package org.dreamcat.cli.generator.apidoc.scheme;

import lombok.Data;
import org.dreamcat.common.reflect.ObjectType;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiOutputParam {

    private ObjectType type;
    private String comment;

    // renderer type
    private String jsonWithComment;
    private List<ApiParamField> fields; // indentedTable
}
