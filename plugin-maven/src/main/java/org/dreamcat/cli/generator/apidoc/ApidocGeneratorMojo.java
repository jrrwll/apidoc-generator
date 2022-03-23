package org.dreamcat.cli.generator.apidoc;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
@Getter
// @Execute()
@Mojo(name = "apidocGenerate")
public class ApidocGeneratorMojo extends AbstractMojo {

    @Parameter
    private String outputPath;
    @Parameter(defaultValue = "false")
    private Boolean rewrite;
    @Parameter
    private List<String> classDirs;
    @Parameter
    private List<String> jarDirs;
    @Parameter
    private List<String> basePackages;
    @Parameter
    private List<String> srcDirs;
    @Parameter(required = true)
    private List<String> javaFileDirs;
    @Parameter(defaultValue = "true")
    private Boolean useRelativeJavaFilePath;
    @Parameter
    private List<String> ignoreInputParamTypes;
    @Parameter(defaultValue = "true")
    private Boolean enableSpringWeb;
    @Parameter
    private JsonWithComment JsonWithComment;
    @Parameter
    private Swagger swagger;

    public void execute() throws MojoExecutionException {
        new ApidocGeneratorAction(this).run();
    }

    // @Parameter( defaultValue = "${project}", readonly = true )
    // private MavenProject project;

    @Data
    public static class JsonWithComment {

        @Parameter(defaultValue = "true")
        private Boolean enabled;
        @Parameter
        private String template;
        @Parameter
        private String nameHeader;
        @Parameter
        private String functionHeader;
        @Parameter
        private String inputParamNameHeader;
        @Parameter
        private String inputParamTitle;
        @Parameter
        private String outputParamTitle;
        @Parameter
        private String functionSep;
        @Parameter
        private String groupSep;
    }

    @Data
    public static class Swagger {

        @Parameter(defaultValue = "true")
        private Boolean enabled;
        @Parameter
        private String defaultTitle;
        @Parameter
        private String defaultVersion;
        @Parameter
        private String fieldNameAnnotation;
        @Parameter
        private List<String> fieldNameAnnotationName;
        @Parameter(defaultValue = "false")
        private Boolean useJacksonFieldNameGetter;
    }
}
