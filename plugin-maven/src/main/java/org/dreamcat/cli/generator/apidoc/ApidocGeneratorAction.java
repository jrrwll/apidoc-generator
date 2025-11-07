package org.dreamcat.cli.generator.apidoc;

import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;

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
        URLClassLoader userCodeClassLoader = ApiDocGeneratorUtil.buildUserCodeClassLoader(
                project, mojo.getLocalRepository());
        logDebug("userCodeClassLoader urls: {}", Arrays.toString(userCodeClassLoader.getURLs()));

        ApiDocParseConfig config = ApiDocGeneratorUtil.buildApiDocConfig(mojo, project, log);
        logDebug("generate with config:\n{}", JsonUtil.toJsonWithPretty(config));

        ApiDocGenerator generator = new ApiDocGenerator(config, userCodeClassLoader);
        generator.setInfoLogger(this::logInfo);
        generator.setErrorLogger(this::logError);

        boolean hasOutput = false;
        File outputDir = mojo.getOutputDir() != null ? new File(mojo.getOutputDir()) : null;
        // swagger
        Swagger swagger = mojo.getSwagger();
        if (swagger != null && Objects.equals(swagger.getEnabled(), true)) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildSwaggerRenderer(swagger);
            generator.generate(renderer, outputDir);
            hasOutput = true;
        }
        // renderer plugin
        RendererPlugin rendererPlugin = mojo.getRendererPlugin();
        if (rendererPlugin != null && ObjectUtil.isNotEmpty(rendererPlugin.getPath())) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildExternalRenderer(rendererPlugin, userCodeClassLoader);
            generator.generate(renderer, outputDir);
            hasOutput = true;
        }

        // jwc, default renderer
        JsonWithComment jwc = mojo.getJsonWithComment();
        if (jwc == null) jwc = new JsonWithComment();
        Boolean jwcEnabled = jwc.getEnabled();
        if (Objects.equals(jwcEnabled, true) || (jwcEnabled == null && !hasOutput)) {
            if (jwcEnabled == null) {
                logInfo("render is unset, using jwc");
            }
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildJsonWithCommentRenderer(jwc);
            generator.generate(renderer, outputDir);
        }
    }

    private void logInfo(String msg, Object... args) {
        log.info(StringUtil.formatMessage(msg, args));
    }

    private void logError(String msg, Object... args) {
        log.error(StringUtil.formatMessage(msg, args));
    }

    private void logDebug(String msg, Object... args) {
        log.debug(StringUtil.formatMessage(msg, args));
    }
}
