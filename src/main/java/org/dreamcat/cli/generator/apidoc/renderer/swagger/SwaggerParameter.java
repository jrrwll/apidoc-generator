package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class SwaggerParameter {

    private In in;
    private String name;
    private String description;
    private Boolean required;
    private SwaggerSchema schema;
    private SwaggerDefinition items; // need by array

    // 2.0
    private SwaggerType type;
    private SwaggerFormat format;
    private Double maximum;
    private Double minimum;

    public enum In {
        path,
        query,
        header,
        // 3.0
        formData,
        body,
    }
}
