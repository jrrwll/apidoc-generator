package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.dreamcat.common.x.jackson.YamlUtil;
import org.dreamcat.databind.type.ObjectType;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
@Data
@JsonInclude(Include.NON_NULL)
public class Swagger {

    private String swagger = "2.0";
    private Info info;
    private String host;
    private String basePath;
    private List<Tag> tags;
    private List<String> schemes; // https
    private Map<String, Map<SwaggerMethod, SwaggerPath>> paths; // path-method
    private Map<String, SwaggerSecurityDefinition> securityDefinitions;
    private Map<String, SwaggerDefinition> definitions;
    private List<ExternalDoc> externalDocs;

    @JsonIgnore
    Map<ObjectType, SwaggerSchema> typeSchemaCache = new HashMap<>();
    @JsonIgnore
    Map<String, SwaggerSchema> defNameSchemaCache = new HashMap<>();

    public String toYaml() {
        return YamlUtil.toYaml(this);
    }

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
        private List<ExternalDoc> externalDocs;
    }

    @Data
    @JsonInclude(Include.NON_NULL)
    public static class ExternalDoc {

        private String description;
        private String url;
    }

}
