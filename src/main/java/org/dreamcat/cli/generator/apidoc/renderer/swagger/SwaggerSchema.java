package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
public class SwaggerSchema {

    @JsonProperty("$ref")
    private String ref; // #/definitions/some_def
}
