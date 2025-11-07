package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import org.dreamcat.common.reflect.ObjectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class Swagger {

    public static final String OPENAPI_VERSION = "3.0.4";
    public static final String SWAGGER_VERSION = "2.0";

    private String openapi;
    private String swagger;

    private Info info;
    private List<Tag> tags;
    private Map<String, Map<SwaggerMethod, SwaggerPath>> paths; // path-method
    private ExternalDoc externalDocs;

    // openapi 3.0
    private List<Server> servers;
    private Components components;

    // swagger 2.0
    private String host;
    private String basePath;
    private List<String> schemes; // http, https
    private Map<String, SwaggerSecurityDefinition> securityDefinitions;
    private Map<String, SwaggerDefinition> definitions;

    @JsonIgnore
    Map<ObjectType, SwaggerSchema> typeSchemaCache = new HashMap<>();
    @JsonIgnore
    Map<String, SwaggerSchema> defNameSchemaCache = new HashMap<>();

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class Info {

        private String description;
        private String version;
        private String title;
        private String termsOfService;
        private Contact contact;
        private License license;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class Contact {

        private String email;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class License {

        private String name;
        private String url;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class Tag {

        private String name;
        private String description;
        private ExternalDoc externalDocs;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class ExternalDoc {

        private String description;
        private String url;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class Server {

        private String url;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class Components {

        private Map<String, SwaggerDefinition> schemas;
        private Map<String, RequestBody> requestBodies;
        // private Map<String, SecurityScheme> securitySchemes;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class RequestBody {

        private String description;
        private Map<String, SwaggerSchema> content;
    }
}
