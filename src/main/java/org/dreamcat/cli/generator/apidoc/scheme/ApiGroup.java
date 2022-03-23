package org.dreamcat.cli.generator.apidoc.scheme;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiGroup {

    private String comment;
    private String name;
    private List<ApiFunction> functions = new ArrayList<>();
}
