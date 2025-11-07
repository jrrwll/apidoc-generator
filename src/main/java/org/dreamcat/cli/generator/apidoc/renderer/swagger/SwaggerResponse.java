package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class SwaggerResponse {

    private String description;
    private Map<String, SwaggerHeader> headers;

    // 3.0
    private Map<String, SwaggerContent> content; // such as: application/json

    // 2.0
    private SwaggerSchema schema;

}
