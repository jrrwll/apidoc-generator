package org.dreamcat.cli.generator.apidoc;

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

import java.io.File;
import java.io.IOException;
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

        boolean hasOutput = false;
        // swagger
        Swagger swagger = mojo.getSwagger();
        if (swagger != null && Objects.equals(swagger.getEnabled(), true)) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildSwaggerRenderer(swagger);
            output(config, renderer, userCodeClassLoader);
            hasOutput = true;
        }
        // renderer plugin
        RendererPlugin rendererPlugin = mojo.getRendererPlugin();
        if (rendererPlugin != null && ObjectUtil.isNotEmpty(rendererPlugin.getPath())) {
            ApiDocRenderer renderer = ApiDocGeneratorUtil.buildExternalRenderer(rendererPlugin, userCodeClassLoader);
            output(config, renderer, userCodeClassLoader);
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
            output(config, renderer, userCodeClassLoader);
        }
    }

    private void output(
            ApiDocParseConfig config, ApiDocRenderer renderer,
            ClassLoader userCodeClassLoader) throws Exception {
        logInfo("renderer: {}", renderer.getClass().getName());

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
            log.error("output file already exists(set `rewrite = true` to confirm it): " + outputFile);
            return;
        }

        try {
            logInfo("writing to {}", outputFile);
            FileUtil.write(outputFile, doc);
            logInfo("done");
        } catch (IOException e) {
            log.error(StringUtil.formatMessage("error to write file {}, doc:\n {}",
                    outputFile, doc), e);
        }
    }

    private void logInfo(String msg, Object... args) {
        log.info(StringUtil.formatMessage(msg, args));
    }

    private void logWarn(String msg, Object... args) {
        log.warn(StringUtil.formatMessage(msg, args));
    }

    private void logDebug(String msg, Object... args) {
        log.debug(StringUtil.formatMessage(msg, args));
    }
}
