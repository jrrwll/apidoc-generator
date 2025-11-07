package org.dreamcat.cli.generator.apidoc;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
@Getter
@Setter
// @Execute()
@Mojo(name = "apidocGenerate", requiresDependencyResolution = ResolutionScope.COMPILE)
public class ApidocGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;
    @Component
    protected RepositorySystem repositorySystem;

    @Parameter(defaultValue = "false")
    private Boolean verbose;
    @Parameter
    private String outputDir;
    @Parameter
    private List<String> basePackages;
    @Parameter(required = true)
    private List<String> javaFileDirs;
    @Parameter
    private List<String> ignoreInputParamTypes;
    @Parameter
    private List<String> ignoreParamNames;
    @Parameter(defaultValue = "false")
    private Boolean mergeInputParam;
    @Parameter(defaultValue = "true")
    private Boolean autoDetect;

    @Parameter
    private JsonWithComment jsonWithComment;
    @Parameter
    private Swagger swagger;
    @Parameter
    private RendererPlugin rendererPlugin;

    @Parameter
    private List<ServiceDoc> serviceDocList;
    @Parameter
    private List<FunctionDoc> functionDocList;
    @Parameter
    private List<FieldDoc> fieldDocList;

    @Parameter
    private String extraConfigJson;

    public void execute() throws MojoExecutionException {
        new ApidocGeneratorAction(this).run();
    }

    @Data
    public static class JsonWithComment {

        @Parameter(defaultValue = "false")
        private Boolean enabled;
        @Parameter(defaultValue = "false")
        private Boolean i18n;
        @Parameter
        private String lang;
        @Parameter
        private String template;
        @Parameter
        private Map<String, String> includeTemplates;
        @Parameter(defaultValue = "false")
        private Boolean fieldsNoRequired;
        @Parameter(defaultValue = "false")
        private Boolean outputParamAsIndentedTable;
        @Parameter
        private String nameHeader;
        @Parameter
        private String functionHeader;
        @Parameter
        private String inputParamTitle;
        @Parameter
        private String outputParamTitle;
        @Parameter(defaultValue = "false")
        private Boolean pinFunctionComment;
        @Parameter
        private String seqPrefix;
        @Parameter
        private Integer seqOffset;

        @Parameter
        private Integer maxNestLevel;
        @Parameter
        private String indentSpace;
        @Parameter
        private String indentPrefix;
        @Parameter
        private String indentName;
        @Parameter
        private String indentType;
        @Parameter
        private String indentRequired;
        @Parameter
        private String requiredTrue;
        @Parameter
        private String requiredFalse;
        @Parameter
        private String requiredNull;
    }

    @Data
    public static class Swagger {

        @Parameter(defaultValue = "false")
        private Boolean enabled;
        @Parameter(defaultValue = "false")
        private Boolean swagger2;
        @Parameter
        private String defaultTitle;
        @Parameter
        private String defaultVersion;

        @Parameter
        private HttpPush httpPush;
    }

    @Data
    public static class RendererPlugin {

        @Parameter
        private String path;
        // json object
        @Parameter
        private String injectedArgs;
    }

    @Data
    public static class ServiceDoc {

        @Parameter
        private String annotationName;
        @Parameter
        private List<String> nameMethod;
        @Parameter
        private List<String> commentMethod;
    }

    @Data
    public static class FunctionDoc {

        @Parameter
        private String annotationName;
        @Parameter
        private List<String> commentMethod;
        @Parameter
        private List<String> nestedParamMethod;
        @Parameter
        private List<String> nestedParamNameMethod;
        @Parameter
        private List<String> nestedParamCommentMethod;
        @Parameter
        private List<String> nestedParamRequiredMethod;
    }

    @Data
    public static class FieldDoc {

        @Parameter
        private String annotationName;
        @Parameter
        private List<String> nameMethod;
        @Parameter
        private List<String> commentMethod;
        @Parameter
        private List<String> requiredMethod;
    }

    @Data
    public static class HttpPush {
        @Parameter
        private String url;

        @Parameter
        private Map<String, String> headers;

        @Parameter
        private Map<String, Object> json;

        @Parameter
        private String text;

        @Parameter
        private Map<String, String> form;
    }
}
