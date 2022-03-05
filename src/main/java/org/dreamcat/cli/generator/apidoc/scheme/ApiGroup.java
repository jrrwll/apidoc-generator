package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiGroup {

    private String comment;
    private String name;
    private Map<String, ApiFunction> functions = new LinkedHashMap<>();
}
