package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiFunction {

    private String name; // method name
    private String serviceName; // class name
    private String comment; // method comment
    private Map<String, ApiInputParam> inputParams = new LinkedHashMap<>();
    private ApiOutputParam outputParam;
    // http-like part
    private List<String> path; // url path
    private List<String> action; // http method
    private List<String> consumes; // consume type
    private List<String> produces; // produce type
}
