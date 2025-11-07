package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class SwaggerSchema {

    // 3.0 -> #/components/schemas/some_def
    @JsonProperty("$ref")
    private String ref; // 2.0 -> #/definitions/some_def

    // 3.0
    private SwaggerType type;
    private SwaggerFormat format;
    private Double maximum;
    private Double minimum;
}
