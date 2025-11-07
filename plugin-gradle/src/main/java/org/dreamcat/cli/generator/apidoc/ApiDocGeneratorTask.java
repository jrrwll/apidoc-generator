package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.StringUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorTask extends DefaultTask {

    private final ApiDocGeneratorExtension extension;
    private final JavaPluginExtension javaPluginExtension;
    private final Configuration compileConfiguration;

    @Inject
    public ApiDocGeneratorTask(Project project) {
        this.extension = project.getExtensions()
                .getByType(ApiDocGeneratorExtension.class);
        this.javaPluginExtension = project.getExtensions()
                .getByType(JavaPluginExtension.class);

        this.compileConfiguration = GradleUtil.getCompileClasspath(project);
    }

    @TaskAction
    public void run() throws Exception {
        URLClassLoader userCodeClassLoader = GradleUtil.buildUserCodeClassLoader(javaPluginExtension, compileConfiguration);
        getLogger().info("userCodeClassPaths: " + Arrays.toString(userCodeClassLoader.getURLs()));

        List<String> srcDirs = GradleUtil.getSrcDirs(javaPluginExtension).stream()
                .map(File::getPath).collect(Collectors.toList());
        ApiDocParseConfig config = ApiDocGeneratorUtil.buildApiDocConfig(extension, srcDirs);
        getLogger().info("generate with config:\n{}", JsonUtil.toJsonWithPretty(config));

        ApiDocGenerator generator = new ApiDocGenerator(config, userCodeClassLoader);
        generator.setInfoLogger(this::logInfo);
        generator.setErrorLogger(this::logError);

        boolean hasOutput = false;
        File outputDir = extension.getOutputDir().map(File::new).getOrNull();
        // swagger
        Swagger swagger = extension.getSwagger();
        if (swagger.getEnabled().getOrElse(false)) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildSwaggerRenderer(swagger);
            generator.generate(renderer, outputDir);
            hasOutput = true;
        }
        // renderer plugin, not support
        RendererPlugin rendererPlugin = extension.getRendererPlugin();
        if (rendererPlugin.getPath().getOrNull() != null) {
            getLogger().quiet("path: " + new File(rendererPlugin.getPath().get()).getCanonicalPath());
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildExternalRenderer(rendererPlugin, userCodeClassLoader);
            generator.generate(renderer, outputDir);
            hasOutput = true;
        }
        // jwc, default renderer
        JsonWithComment jwc = extension.getJsonWithComment();
        Boolean jwcEnabled = jwc.getEnabled().getOrNull();
        if (Objects.equals(jwcEnabled, true) || (jwcEnabled == null && !hasOutput)) {
            if (jwcEnabled == null) {
                getLogger().quiet("render is unset, using jwc");
            }
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildJsonWithCommentRenderer(jwc);
            generator.generate(renderer, outputDir);
        }
    }

    private void logInfo(String msg, Object... args) {
        getLogger().info(StringUtil.formatMessage(msg, args));
    }

    private void logError(String msg, Object... args) {
        getLogger().error(StringUtil.formatMessage(msg, args));
    }
}
