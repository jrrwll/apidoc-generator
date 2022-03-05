package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiDoc {

    private String comment;
    private String name;
    private String version;
    private Map<String, ApiGroup> groups = new LinkedHashMap<>();
}
