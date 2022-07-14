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
    @Parameter(defaultValue = "false")
    private Boolean enableMergeInputParam;
    @Parameter(defaultValue = "true")
    private Boolean enableSpringWeb;
    @Parameter
    private Text text;
    @Parameter
    private Swagger swagger;

    public void execute() throws MojoExecutionException {
        new ApidocGeneratorAction(this).run();
    }

    // @Parameter( defaultValue = "${project}", readonly = true )
    // private MavenProject project;

    @Data
    public static class Text {

        @Parameter(defaultValue = "true")
        private Boolean enabled;
        @Parameter
        private Boolean enableIndentedTable;
        @Parameter
        private String template;
        @Parameter
        private String nameHeader;
        @Parameter
        private String functionHeader;
        @Parameter
        private String paramHeader;
        @Parameter
        private String inputParamTitle;
        @Parameter
        private String outputParamTitle;
        @Parameter(defaultValue = "false")
        private Boolean pinFunctionComment;
        @Parameter
        private String seqPrefix;

        @Parameter
        private Integer maxNestLevel;
        @Parameter
        private String indentPrefix;
        @Parameter
        private String indentName;
        @Parameter
        private String indentType;
        @Parameter
        private String indentRequired;
        @Parameter
        private String indentComment;
        @Parameter
        private String requiredTrue;
        @Parameter
        private String requiredFalse;
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
