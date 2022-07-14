package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.List;
import lombok.Data;
import org.dreamcat.databind.type.ObjectType;

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
