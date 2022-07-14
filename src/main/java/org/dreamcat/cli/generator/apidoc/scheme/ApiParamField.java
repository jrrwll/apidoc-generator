package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.List;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Data
public class ApiParamField {

    private String name;
    private String comment = "";
    private String typeName;

    private boolean required;

    private List<ApiParamField> fields;
}
