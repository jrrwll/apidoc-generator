package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApidocGeneratorAction implements Runnable {

    private final ApidocGeneratorMojo mojo;
    private final MavenProject project;
    private final Log log;

    public ApidocGeneratorAction(ApidocGeneratorMojo mojo) {
        this.mojo = mojo;
        this.project = mojo.getProject();
        this.log = mojo.getLog();
    }

    @SneakyThrows
    public void run() {
        ClassLoader userCodeClassLoader = ApiDocGeneratorUtil.buildUserCodeClassLoader(
                project, mojo.getLocalRepository(), mojo.getVerbose(), log);
        ApiDocParseConfig config = ApiDocGeneratorUtil.buildApiDocConfig(mojo, project, log);

        Swagger swagger = mojo.getSwagger();
        // swagger
        if (swagger != null && swagger.getEnabled()) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildSwaggerRenderer(swagger);
            output(config, renderer, userCodeClassLoader);
            return;
        }
        // renderer plugin
        RendererPlugin rendererPlugin = mojo.getRendererPlugin();
        if (rendererPlugin != null && ObjectUtil.isNotEmpty(rendererPlugin.getPath())) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildExternalRenderer(rendererPlugin, userCodeClassLoader);
            output(config, renderer, userCodeClassLoader);
            return;
        }

        JsonWithComment jwc = mojo.getJsonWithComment();
        if (jwc == null) jwc = new JsonWithComment();
        ApiDocRenderer renderer = ApiDocGeneratorUtil.buildJsonWithCommentRenderer(jwc);
        output(config, renderer, userCodeClassLoader);
    }

    private void output(
            ApiDocParseConfig config, ApiDocRenderer renderer,
            ClassLoader userCodeClassLoader) throws Exception {
        log.info("renderer: " + renderer.getClass().getName());
        log.info("generate with config:\n" +
                JsonUtil.toJsonWithPretty(config));

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer, userCodeClassLoader);
        String doc = generator.generate();

        String outputPath = mojo.getOutputPath();
        boolean rewrite = mojo.getRewrite();
        if (outputPath == null) {
            logWarn("only print doc since `outputPath` is unset");
            logInfo("********** Generated Doc **********");
            logInfo(doc); // print doc to console
            logInfo("***********************************");
            return;
        }
        File outputFile = new File(outputPath).getAbsoluteFile();
        if (outputFile.exists() && !rewrite) {
            logError("output file already exists(set `rewrite = true` to confirm it): {}",
                    outputFile);
            return;
        }

        try {
            logInfo("writing to {}", outputFile);
            FileUtil.writeFrom(outputFile, doc);
            logInfo("done");
        } catch (IOException e) {
            logError("error to write file {}, doc:\n {}", outputFile, doc);
        }
    }

    private void logInfo(String msg, Object... args) {
        log.info(StringUtil.format(msg, args));
    }

    private void logWarn(String msg, Object... args) {
        log.warn(StringUtil.format(msg, args));
    }

    private void logError(String msg, Object... args) {
        log.error(StringUtil.format(msg, args));
    }
}
