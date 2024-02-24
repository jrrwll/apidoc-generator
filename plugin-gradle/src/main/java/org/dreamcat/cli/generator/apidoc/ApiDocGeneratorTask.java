package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorTask extends DefaultTask {

    // private final ApiDocGeneratorExtension extension;

    // @javax.inject.Inject
    // public ApiDocGeneratorTask(Project project, ApiDocGeneratorExtension extension) {
    //     this.extension = extension;
    // }

    @TaskAction
    public void run() throws Exception {
        ApiDocGeneratorExtension extension = getProject().getExtensions()
                .getByType(ApiDocGeneratorExtension.class);

        ClassLoader userCodeClassLoader = ApiDocGeneratorUtil.buildUserCodeClassLoader(getProject());
        ApiDocParseConfig config = ApiDocGeneratorUtil.buildApiDocConfig(extension, getProject());

        // swagger
        Swagger swagger = extension.getSwagger();
        if (swagger.getEnabled().getOrElse(false)) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildSwaggerRenderer(swagger);
            output(config, renderer, userCodeClassLoader);
            return;
        }
        // renderer plugin
        RendererPlugin rendererPlugin = extension.getRendererPlugin();
        if (rendererPlugin.getPath().getOrNull() != null) {
            getLogger().quiet("path: " + new File(rendererPlugin.getPath().get()).getCanonicalPath());
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildExternalRenderer(rendererPlugin, userCodeClassLoader);
            output(config, renderer, userCodeClassLoader);
            return;
        }
        // jwc
        JsonWithComment jwc = extension.getJsonWithComment();
        ApiDocRenderer renderer = ApiDocGeneratorUtil.buildJsonWithCommentRenderer(jwc);
        output(config, renderer, userCodeClassLoader);
    }

    private void output(
            ApiDocParseConfig config, ApiDocRenderer renderer,
            ClassLoader userCodeClassLoader) throws IOException {
        getLogger().info("generate with config: {}, renderer: {}",
                JsonUtil.toJson(config), renderer.getClass().getSimpleName());

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer, userCodeClassLoader);
        String doc = generator.generate();

        ApiDocGeneratorExtension extension = getProject().getExtensions()
                .getByType(ApiDocGeneratorExtension.class);
        String outputPath = extension.getOutputPath().getOrNull();
        boolean rewrite = extension.getRewrite().get();
        if (outputPath == null) {
            if (getLogger().isInfoEnabled()) {
                getLogger().warn("only print doc since `outputPath` is unset");
            }
            getLogger().quiet("********** Generated Doc **********");
            getLogger().quiet(doc); // print doc to console
            getLogger().quiet("***********************************");
            return;
        }
        File outputFile = new File(outputPath).getAbsoluteFile();
        if (outputFile.exists() && !rewrite) {
            getLogger().error(
                    "output file already exists(set `rewrite = true` to confirm it): {}", outputFile);
            return;
        }

        try {
            getLogger().info("writing to {}", outputFile);
            FileUtil.writeFrom(outputFile, doc);
            getLogger().info("done");
        } catch (IOException e) {
            getLogger().error("error to write file {}, doc:\n {}", outputFile, doc);
        }
    }
}
