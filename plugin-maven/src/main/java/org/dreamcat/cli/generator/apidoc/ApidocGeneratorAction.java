package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.maven.plugin.logging.Log;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.Swagger;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.Text;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.text.IndentedTableRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.text.JsonWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.text.TextRenderer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.RandomUtil;
import org.dreamcat.common.x.jackson.JsonUtil;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApidocGeneratorAction implements Runnable {

    private final ApidocGeneratorMojo mojo;
    private final Log log;

    public ApidocGeneratorAction(ApidocGeneratorMojo mojo) {
        this.mojo = mojo;
        this.log = mojo.getLog();
    }

    public void run() {
        // set extra dependencies
        List<String> classpath = mojo.getClassDirs();
        if (ObjectUtil.isEmpty(classpath)) {
            classpath = buildDefaultClassPath();
        }
        classpath = PathUtil.absolute(classpath);
        List<String> jarDirs = mojo.getJarDirs();
        if (ObjectUtil.isNotEmpty(jarDirs)) {
            jarDirs = ApiDocGeneratorUtil.treeClassPath(jarDirs);
            classpath.addAll(jarDirs);
        }
        ClassLoader userCodeClassLoader = ApiDocGeneratorUtil.buildUserCodeClassLoader(classpath);

        ApiDocConfig config = buildApiDocConfig();

        Swagger swagger = mojo.getSwagger();
        Text text = mojo.getText();
        // swagger
        if (swagger != null && swagger.getEnabled()) {
            ApiDocRenderer renderer = swaggerRenderer(swagger);

            output(config, renderer, userCodeClassLoader, ".yaml");
        }
        // jwc
        if (text == null || text.getEnabled()) {
            TextRenderer renderer;
            if (text != null && Objects.equals(text.getEnableIndentedTable(), true)) {
                renderer = new IndentedTableRenderer();
                config.setUseIndentedTable(true);
            } else {
                renderer = new JsonWithCommentRenderer();
                config.setUseJsonWithComment(true);
            }
            fillTextRenderer(renderer, text);

            output(config, renderer, userCodeClassLoader, ".md");
        }
    }

    private void output(
            ApiDocConfig config, ApiDocRenderer renderer,
            ClassLoader userCodeClassLoader, String suffix) {
        logInfo("generate with config: {}, renderer: {}",
                JsonUtil.toJson(config), renderer.getClass().getSimpleName());

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer, userCodeClassLoader);

        String outputPath = mojo.getOutputPath();
        boolean rewrite = mojo.getRewrite();
        if (outputPath == null) {
            String userDir = System.getProperty("user.dir");
            outputPath = userDir + "/apidoc-" + RandomUtil.timeBaseRadix36W16().substring(4, 12) + suffix;
        }
        File outputFile = new File(outputPath).getAbsoluteFile();
        if (outputFile.exists() && !rewrite) {
            logError("output file already exists(set `rewrite = true` to confirm it): {}", outputFile);
        }

        String doc = generator.generate();
        try {
            FileUtil.writeFrom(outputFile, doc);
        } catch (IOException e) {
            logError("error to write file {}, doc:\n {}", outputFile, doc);
        }
    }

    private ApiDocConfig buildApiDocConfig() {
        ApiDocConfig config = new ApiDocConfig();
        setIf(config::setBasePackages, mojo.getBasePackages());

        setIf(config::setSrcDirs, mojo.getSrcDirs());
        if (ObjectUtil.isEmpty(config.getSrcDirs())) {
            config.setSrcDirs(buildDefaultSrcDirs());
        }

        config.setJavaFileDirs(mojo.getJavaFileDirs());
        config.setUseRelativeJavaFilePath(mojo.getUseRelativeJavaFilePath());

        config.setEnableSpringWeb(mojo.getEnableSpringWeb());
        if (ObjectUtil.isNotEmpty(mojo.getIgnoreInputParamTypes())) {
            config.setIgnoreInputParamTypes(new HashSet<>(mojo.getIgnoreInputParamTypes()));
        } else if (config.isEnableSpringWeb()) {
            config.setIgnoreInputParamTypes(new HashSet<>(Arrays.asList(
                    "org.springframework.web.multipart.MultipartFile")));
        }
        if (Objects.equals(mojo.getEnableMergeInputParam(), true)) {
            config.setMergeInputParam(MergeInputParam.byFlatType());
        }
        return config;
    }

    private void fillTextRenderer(TextRenderer renderer, Text text) {
        if (text == null) return;
        setIf(renderer::setTemplate, text.getTemplate());
        setIf(renderer::setNameHeader, text.getNameHeader());
        setIf(renderer::setFunctionHeader, text.getFunctionHeader());
        setIf(renderer::setParamHeader, text.getParamHeader());
        setIf(renderer::setInputParamTitle, text.getInputParamTitle());
        setIf(renderer::setOutputParamTitle, text.getOutputParamTitle());
        setIf(renderer::setPinFunctionComment, text.getPinFunctionComment());
        setIf(renderer::setSeqPrefix, text.getSeqPrefix());

        setIf(renderer::setMaxNestLevel, text.getMaxNestLevel());
        setIf(renderer::setIndentPrefix, text.getIndentPrefix());
        setIf(renderer::setIndentName, text.getIndentName());
        setIf(renderer::setIndentType, text.getIndentType());
        setIf(renderer::setIndentRequired, text.getIndentRequired());
        setIf(renderer::setRequiredTrue, text.getRequiredTrue());
        setIf(renderer::setRequiredFalse, text.getRequiredFalse());
    }

    private ApiDocRenderer swaggerRenderer(Swagger swagger) {
        SwaggerRenderer renderer = new SwaggerRenderer();
        setIf(renderer::setDefaultTitle, swagger.getDefaultTitle());
        setIf(renderer::setDefaultVersion, swagger.getDefaultVersion());

        if (swagger.getUseJacksonFieldNameGetter()) {
            renderer.useJacksonFieldNameGetter();
        } else {
            renderer.setFieldNameAnnotation(swagger.getFieldNameAnnotation());
            renderer.setFieldNameAnnotationName(swagger.getFieldNameAnnotationName());
        }
        return renderer;
    }

    private List<String> buildDefaultClassPath() {
        return buildDefaultPaths("target",
                "buildDefaultClassPath");
    }

    private List<String> buildDefaultSrcDirs() {
        return buildDefaultPaths("src/main/java",
                "buildDefaultSrcDirs");
    }

    private List<String> buildDefaultPaths(String child, String methodName) {
        File current = new File("").getAbsoluteFile();
        File dir = new File(current, child);
        if (dir.exists()) {
            return Collections.singletonList(dir.getAbsolutePath());
        }
        File[] files = current.listFiles();
        if (files == null) {
            logError("error at {}, not a directory: {}", methodName, current);
            return Collections.emptyList();
        }

        List<String> paths = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            if (!file.isDirectory() || name.startsWith(".") || name.startsWith("$")) continue;
            File sub = new File(file, child);
            if (sub.exists()) {
                paths.add(sub.getAbsolutePath());
            }
        }
        if (paths.isEmpty()) {
            logError("error at {}, cannot find {}/*/{}",
                    methodName, current, child);
        }
        return paths;
    }

    private void logInfo(String msg, Object... args) {
        log(log::info, msg, args);
    }

    private void logError(String msg, Object... args) {
        log(log::error, msg, args);
    }

    private void log(Consumer<String> logger, String msg, Object... args) {
        int n = msg.length();
        StringBuilder s = new StringBuilder(n << 1);
        int k = 0;
        for (int i = 0; i < n; i++) {
            char c = msg.charAt(i);
            if (c == '{' && i < n - 1) {
                char nc = msg.charAt(++i);
                if (nc == '}') {
                    s.append(args[k++]);
                } else {
                    s.append(c).append(nc);
                }
            } else {
                s.append(c);
            }
        }
        logger.accept(s.toString());
    }

    private <T> void setIf(Consumer<T> setter, T val) {
        if (val != null && (!(val instanceof Collection) || !((Collection<?>) val).isEmpty())) {
            setter.accept(val);
        }
    }
}
