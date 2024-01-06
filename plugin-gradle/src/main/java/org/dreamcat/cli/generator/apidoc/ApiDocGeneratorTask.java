package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Swagger;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Text;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Provider;
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
        // set extra dependencies
        List<String> classpath = extension.getClassDirs().getOrElse(new ArrayList<>());
        if (ObjectUtil.isEmpty(classpath)) {
            classpath = buildDefaultClassPath();
        }
        classpath = PathUtil.absolute(classpath);
        List<String> jarDirs = extension.getJarDirs().getOrElse(Collections.emptyList());
        if (ObjectUtil.isNotEmpty(jarDirs)) {
            jarDirs = ApiDocGeneratorUtil.treeClassPath(jarDirs);
            classpath.addAll(jarDirs);
        }
        ClassLoader userCodeClassLoader = ApiDocGeneratorUtil.buildUserCodeClassLoader(classpath);

        ApiDocParserConfig config = buildApiDocConfig();

        Swagger swagger = extension.getSwagger();
        // swagger
        if (swagger.getEnabled().getOrElse(false)) {
            ApiDocRenderer renderer = swaggerRenderer(swagger);
            output(config, renderer, userCodeClassLoader);
        }
        // jwc
        Text text = extension.getText();
        if (text.getEnabled().getOrElse(true)) {
            ApiDocRenderer renderer = textRenderer(text);
            output(config, renderer, userCodeClassLoader);
        }
        // renderer plugin
        RendererPlugin rendererPlugin = extension.getRendererPlugin();
        if (rendererPlugin.getEnabled().getOrElse(true)) {
            ApiDocRenderer renderer = externalRenderer(rendererPlugin, userCodeClassLoader);
            output(config, renderer, userCodeClassLoader);
        }
    }

    private void output (
            ApiDocParserConfig config, ApiDocRenderer renderer,
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
            if (getLogger().isDebugEnabled()) {
                getLogger().warn("only print doc since `outputPath` is unset");
            }
            getLogger().info("********** Generated Doc **********");
            getLogger().info(doc);
            getLogger().info("***********************************");
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

    private ApiDocParserConfig buildApiDocConfig() {
        ApiDocParserConfig config = new ApiDocParserConfig();
        ApiDocGeneratorExtension extension = getProject().getExtensions()
                .getByType(ApiDocGeneratorExtension.class);
        setIf(config::setBasePackages, extension.getBasePackages());

        setIf(config::setSrcDirs, extension.getSrcDirs());
        if (ObjectUtil.isEmpty(config.getSrcDirs())) {
            config.setSrcDirs(buildDefaultSrcDirs());
        }

        config.setJavaFileDirs(extension.getJavaFileDirs().get());

        config.setUseRelativeJavaFilePath(extension.getUseRelativeJavaFilePath().get());
        config.setIgnoreInputParamTypes(new HashSet<>(extension.getIgnoreInputParamTypes().get()));
        if (extension.getMergeInputParam().getOrElse(false)) {
            config.setMergeInputParam(MergeInputParam.flatType());
        }
        config.setAutoDetect(extension.getAutoDetect().get());
        return config;
    }

    private ApiDocRenderer textRenderer(Text text) {
        String template = text.getTemplate().getOrNull();
        if (template != null) {
            Map<String, String> includeTemplates = text.getIncludeTemplates()
                    .getOrElse(Collections.emptyMap());
            return new TextTemplateRenderer(template, includeTemplates);
        }

        JsnoWithCommentRenderer renderer = new JsnoWithCommentRenderer();
        setIf(renderer::setFieldsNoRequired, text.getFieldsNoRequired());
        setIf(renderer::setOutputParamAsIndentedTable, text.getOutputParamAsIndentedTable());

        setIf(renderer::setNameHeader, text.getNameHeader());
        setIf(renderer::setFunctionHeader, text.getFunctionHeader());
        setIf(renderer::setInputParamTitle, text.getInputParamTitle());
        setIf(renderer::setOutputParamTitle, text.getOutputParamTitle());
        setIf(renderer::setPinFunctionComment, text.getPinFunctionComment());
        setIf(renderer::setSeqPrefix, text.getSeqPrefix());

        setIf(renderer::setMaxNestLevel, text.getMaxNestLevel());
        setIf(renderer::setIndentSpace, text.getIndentSpace());
        setIf(renderer::setIndentPrefix, text.getIndentPrefix());
        setIf(renderer::setIndentName, text.getIndentName());
        setIf(renderer::setIndentType, text.getIndentType());
        setIf(renderer::setIndentRequired, text.getIndentRequired());
        setIf(renderer::setRequiredTrue, text.getRequiredTrue());
        setIf(renderer::setRequiredFalse, text.getRequiredFalse());
        return renderer;
    }

    private ApiDocRenderer swaggerRenderer(Swagger swagger) {
        SwaggerRenderer renderer = new SwaggerRenderer();
        setIf(renderer::setDefaultTitle, swagger.getDefaultTitle());
        setIf(renderer::setDefaultVersion, swagger.getDefaultVersion());

        // if (swagger.getUseJacksonFieldNameGetter().getOrElse(false)) {
        //     renderer.useJacksonFieldNameGetter();
        // } else {
        //     renderer.setFieldNameAnnotation(swagger.getFieldNameAnnotation().getOrNull());
        //     renderer.setFieldNameAnnotationName(swagger.getFieldNameAnnotationName().getOrNull());
        // }
        return renderer;
    }

    private ApiDocRenderer externalRenderer(RendererPlugin rendererPlugin,
            ClassLoader classLoader) {
        String dir = rendererPlugin.getDir().getOrNull();
        if (dir == null) {
            getLogger().error("dir is unset in rendererPlugin");
            throw new RuntimeException("rendererPlugin.dir is unset");
        }
        Map<String, Object> constructArgs = rendererPlugin.getConstructArgs().getOrNull();
        return ApiDocRenderer.loadFromPath(dir, constructArgs, classLoader);
    }

    private List<String> buildDefaultClassPath() {
        return buildDefaultPaths("build/libs",
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
            getLogger().error("error at {}, not a directory: {}", methodName, current);
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
            getLogger().error("error at {}, cannot find {}/*/{}",
                    methodName, current, child);
        }
        return paths;
    }

    private <T> void setIf(Consumer<T> setter, Provider<T> provider) {
        T val = provider.getOrNull();
        if (val != null && (!(val instanceof Collection) || !((Collection<?>) val).isEmpty())) {
            setter.accept(val);
        }
    }
}
