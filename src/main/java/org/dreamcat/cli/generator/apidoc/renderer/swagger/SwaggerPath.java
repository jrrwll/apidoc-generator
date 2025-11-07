package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class SwaggerPath {

    private List<String> tags;
    private String summary;
    private String description;
    private String operationId;

    private List<SwaggerParameter> parameters;
    private Map<String, SwaggerResponse> responses; // 401, 402, default
    private Map<String, List<String>> security; // security def code

    // 3.0
    private SwaggerRequestBody requestBody;

    // 2.0
    private List<String> consumes;
    private List<String> produces;

    @Data
    public static class SwaggerRequestBody {

        private String description;
        private Map<String, SwaggerContent> content; // application/json -> SwaggerContent
    }
}
