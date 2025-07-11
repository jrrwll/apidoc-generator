package org.dreamcat.cli.generator.apidoc.scheme;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiDoc {

    private String comment;
    private String name;
    private String version;
    private List<ApiGroup> groups = new ArrayList<>();
}
