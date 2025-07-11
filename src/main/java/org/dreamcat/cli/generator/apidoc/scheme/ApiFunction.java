package org.dreamcat.cli.generator.apidoc.scheme;

import lombok.Data;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiFunction {

    private String name; // method name
    private String serviceName; // class name
    private String comment; // method comment
    private List<ApiInputParam> inputParams;
    private int inputParamCount; // inputParams.size()
    private boolean inputParamsMerged;
    private ApiOutputParam outputParam;

    private List<String> path; // url path
    private List<String> action; // http method
    private List<String> consumes; // consume type
    private List<String> produces; // produce type
}
