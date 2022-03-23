package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.jwc.JsonWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.RandomUtil;
import org.dreamcat.common.x.jackson.JsonUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorTask extends DefaultTask {

    private final Project project;
    private final Logger logger;
    private final ApiDocGeneratorExtension extension;

    @javax.inject.Inject
    public ApiDocGeneratorTask(Project project, ApiDocGeneratorExtension extension) {
        this.project = project;
        this.logger = project.getLogger();
        this.extension = extension;
    }

    @TaskAction
    public void run() {
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

        ApiDocConfig config = buildApiDocConfig();
        ApiDocRenderer renderer;

        Swagger swagger = extension.getSwagger();
        JsonWithComment jwc = extension.getJsonWithComment();
        // swagger
        if (swagger.getEnabled().getOrElse(false)) {
            renderer = swaggerRenderer(swagger);

            output(config, renderer, userCodeClassLoader, ".yaml");
        }
        // jwc
        if (jwc.getEnabled().getOrElse(true)) {
            renderer = jsonWithCommentRenderer(jwc);
            config.setJsonWithComment(true);

            output(config, renderer, userCodeClassLoader, ".md");
        }
    }

    private void output(
            ApiDocConfig config, ApiDocRenderer renderer,
            ClassLoader userCodeClassLoader, String suffix) {
        logger.info("generate with config: {}, renderer: {}",
                JsonUtil.toJson(config), renderer.getClass().getSimpleName());

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer, userCodeClassLoader);

        String outputPath = extension.getOutputPath().getOrNull();
        boolean rewrite = extension.getRewrite().get();
        if (outputPath == null) {
            outputPath = "apidoc-" + RandomUtil.timeBaseRadix36W16() + suffix;
        }
        File outputFile = new File(outputPath).getAbsoluteFile();
        if (outputFile.exists() && !rewrite) {
            logger.error(
                    "output file already exists(set `rewrite = true` to confirm it): {}", outputFile);
        }

        String doc = generator.generate();
        try {
            FileUtil.writeFrom(outputFile, doc);
        } catch (IOException e) {
            logger.error("error to write file {}, doc:\n {}", outputFile, doc);
        }
    }

    private ApiDocConfig buildApiDocConfig() {
        ApiDocConfig config = new ApiDocConfig();
        setIf(config::setBasePackages, extension.getBasePackages());

        setIf(config::setSrcDirs, extension.getSrcDirs());
        if (ObjectUtil.isEmpty(config.getSrcDirs())) {
            config.setSrcDirs(buildDefaultSrcDirs());
        }

        config.setJavaFileDirs(extension.getJavaFileDirs().get());

        config.setUseRelativeJavaFilePath(extension.getUseRelativeJavaFilePath().get());
        config.setIgnoreInputParamTypes(new HashSet<>(extension.getIgnoreInputParamTypes().get()));
        config.setEnableSpringWeb(extension.getEnableSpringWeb().get());
        return config;
    }

    private ApiDocRenderer jsonWithCommentRenderer(JsonWithComment jwc) {
        JsonWithCommentRenderer renderer = new JsonWithCommentRenderer();
        setIf(renderer::setTemplate, jwc.getTemplate());
        setIf(renderer::setNameHeader, jwc.getNameHeader());
        setIf(renderer::setFunctionHeader, jwc.getFunctionHeader());
        setIf(renderer::setInputParamNameHeader, jwc.getInputParamNameHeader());
        setIf(renderer::setInputParamTitle, jwc.getInputParamTitle());
        setIf(renderer::setOutputParamTitle, jwc.getOutputParamTitle());
        setIf(renderer::setFunctionSep, jwc.getFunctionSep());
        setIf(renderer::setGroupSep, jwc.getGroupSep());
        return renderer;
    }

    private ApiDocRenderer swaggerRenderer(Swagger swagger) {
        SwaggerRenderer renderer = new SwaggerRenderer();
        setIf(renderer::setDefaultTitle, swagger.getDefaultTitle());
        setIf(renderer::setDefaultVersion, swagger.getDefaultVersion());

        if (swagger.getUseJacksonFieldNameGetter().getOrElse(false)) {
            renderer.useJacksonFieldNameGetter();
        } else {
            renderer.setFieldNameAnnotation(swagger.getFieldNameAnnotation().getOrNull());
            renderer.setFieldNameAnnotationName(swagger.getFieldNameAnnotationName().getOrNull());
        }
        return renderer;
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
            project.getLogger().error("error at {}, not a directory: {}", methodName, current);
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
            project.getLogger().error("error at {}, cannot find {}/*/{}",
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
