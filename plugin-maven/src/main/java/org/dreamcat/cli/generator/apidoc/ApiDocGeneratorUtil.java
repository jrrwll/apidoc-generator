package org.dreamcat.cli.generator.apidoc;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FieldDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FunctionDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.ServiceDoc;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.HttpPush;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.HttpPushConfig;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.net.UrlUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorUtil {

    public static URLClassLoader buildUserCodeClassLoader(
            MavenProject project, ArtifactRepository localRepository) throws Exception {
        Set<String> classDirs = new HashSet<>();
        classDirs.add(MavenUtil.getClassDir(project));
        classDirs.addAll(orEmpty(MavenUtil.getCompileClasspath(project)));
        classDirs.addAll(orEmpty(MavenUtil.getRuntimeClasspath(project)));

        List<File> dependencies = MavenUtil.getAllDependencies(project, localRepository);
        URL[] urls = Stream.concat(dependencies.stream(), classDirs.stream().map(File::new))
                .map(UrlUtil::toURL).toArray(URL[]::new);
        return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    public static ApiDocParseConfig buildApiDocConfig(
            ApidocGeneratorMojo mojo, MavenProject project, Log log)
            throws IOException {
        String srcDir = MavenUtil.getSrcDir(project);
        if (mojo.getVerbose()) {
            log.info("srcDir: " + srcDir);
        }

        String extraConfigJson = mojo.getExtraConfigJson();
        ApiDocParseConfig config;
        if (extraConfigJson != null) {
            config = JsonUtil.fromJson(extraConfigJson, ApiDocParseConfig.class);
        } else {
            config = new ApiDocParseConfig();
        }

        config.setSrcDirs(Collections.singletonList(srcDir));

        setIf(config::setVerbose, mojo.getVerbose());
        setIf(config::setBasePackages, mojo.getBasePackages());
        config.setJavaFileDirs(mojo.getJavaFileDirs());

        if (ObjectUtil.isNotEmpty(mojo.getIgnoreInputParamTypes())) {
            config.setIgnoreInputParamTypes(new HashSet<>(mojo.getIgnoreInputParamTypes()));
        }
        if (ObjectUtil.isNotEmpty(mojo.getIgnoreParamNames())) {
            config.setIgnoreParamNames(new HashSet<>(mojo.getIgnoreParamNames()));
        }
        if (mojo.getMergeInputParam()) {
            config.setMergeInputParam(MergeInputParam.flatType());
        }

        config.setAutoDetect(mojo.getAutoDetect());

        List<ServiceDoc> serviceDocs = orEmpty(mojo.getServiceDocList()).stream().map(it -> {
            ServiceDoc doc = new ServiceDoc();
            setIf(doc::setName, it.getAnnotationName());
            setIf(doc::setNameMethod, it.getNameMethod());
            setIf(doc::setCommentMethod, it.getCommentMethod());
            return doc.getName() != null ? doc : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!serviceDocs.isEmpty()) config.setServiceDoc(serviceDocs);

        List<FunctionDoc> functionDocs = orEmpty(mojo.getFunctionDocList()).stream().map(it -> {
            FunctionDoc doc = new FunctionDoc();
            setIf(doc::setName, it.getAnnotationName());
            setIf(doc::setCommentMethod, it.getCommentMethod());
            setIf(doc::setNestedParamMethod, it.getNestedParamMethod());
            setIf(doc::setNestedParamNameMethod, it.getNestedParamNameMethod());
            setIf(doc::setNestedParamCommentMethod, it.getNestedParamCommentMethod());
            setIf(doc::setNestedParamRequiredMethod, it.getNestedParamRequiredMethod());
            return doc.getName() != null ? doc : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!functionDocs.isEmpty()) config.setFunctionDoc(functionDocs);

        List<FieldDoc> fieldDocs = orEmpty(mojo.getFieldDocList()).stream().map(it -> {
            FieldDoc doc = new FieldDoc();
            setIf(doc::setName, it.getAnnotationName());
            setIf(doc::setNameMethod, it.getNameMethod());
            setIf(doc::setCommentMethod, it.getCommentMethod());
            setIf(doc::setRequiredMethod, it.getRequiredMethod());
            return doc.getName() != null ? doc : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!functionDocs.isEmpty()) config.setFieldDoc(fieldDocs);
        return config;
    }

    public static ApiDocRenderer buildJsonWithCommentRenderer(JsonWithComment jwc) {
        String template = jwc.getTemplate();
        if (template != null) {
            Map<String, String> includeTemplates = jwc.getIncludeTemplates();
            if (includeTemplates == null) includeTemplates = Collections.emptyMap();
            return new TextTemplateRenderer(template, includeTemplates);
        }
        String lang = jwc.getLang();
        if (lang == null && jwc.getI18n()) {
            lang = System.getenv("LANG");
        }
        JsnoWithCommentRenderer renderer;
        if (lang != null) {
            renderer = JsnoWithCommentRenderer.fromI18n(lang);
        } else {
            renderer = new JsnoWithCommentRenderer();
        }

        setIf(renderer::setFieldsNoRequired, jwc.getFieldsNoRequired());
        setIf(renderer::setOutputParamAsIndentedTable, jwc.getOutputParamAsIndentedTable());

        setIf(renderer::setNameHeader, jwc.getNameHeader());
        setIf(renderer::setFunctionHeader, jwc.getFunctionHeader());
        setIf(renderer::setInputParamTitle, jwc.getInputParamTitle());
        setIf(renderer::setOutputParamTitle, jwc.getOutputParamTitle());
        setIf(renderer::setPinFunctionComment, jwc.getPinFunctionComment());
        setIf(renderer::setSeqPrefix, jwc.getSeqPrefix());
        setIf(renderer::setSeqOffset, jwc.getSeqOffset());

        setIf(renderer::setMaxNestLevel, jwc.getMaxNestLevel());
        setIf(renderer::setIndentSpace, jwc.getIndentSpace());
        setIf(renderer::setIndentPrefix, jwc.getIndentPrefix());
        setIf(renderer::setIndentName, jwc.getIndentName());
        setIf(renderer::setIndentType, jwc.getIndentType());
        setIf(renderer::setIndentRequired, jwc.getIndentRequired());
        setIf(renderer::setRequiredTrue, jwc.getRequiredTrue());
        setIf(renderer::setRequiredFalse, jwc.getRequiredFalse());
        setIf(renderer::setRequiredNull, jwc.getRequiredNull());
        return renderer;
    }

    public static ApiDocRenderer buildSwaggerRenderer(Swagger swagger) {
        SwaggerRenderer renderer = new SwaggerRenderer();
        setIf(renderer::setDefaultTitle, swagger.getDefaultTitle());
        setIf(renderer::setSwagger2, swagger.getSwagger2());
        setIf(renderer::setDefaultVersion, swagger.getDefaultVersion());

        HttpPush httpPush = swagger.getHttpPush();
        if (httpPush != null) {
            HttpPushConfig httpPushConfig = new HttpPushConfig();
            setIf(httpPushConfig::setUrl, httpPush.getUrl());
            setIf(httpPushConfig::setHeaders, httpPush.getHeaders());
            setIf(httpPushConfig::setJson, httpPush.getJson());
            setIf(httpPushConfig::setText, httpPush.getText());
            setIf(httpPushConfig::setForm, httpPush.getForm());
            renderer.setHttpPush(httpPushConfig);
        }
        return renderer;
    }

    public static ApiDocRenderer buildExternalRenderer(RendererPlugin rendererPlugin,
            ClassLoader classLoader) throws Exception {
        String path = rendererPlugin.getPath();
        String injectedArgsJson = rendererPlugin.getInjectedArgs();
        Map<String, Object> injectedArgs = null;
        if (injectedArgsJson != null) {
            injectedArgs = JsonUtil.fromJsonObject(injectedArgsJson);
            if (ObjectUtil.isNotEmpty(injectedArgs)) {
                Map<String, Object> args = new HashMap<>(injectedArgs.size());
                injectedArgs.forEach((k, v) -> {
                    if (v instanceof String) {
                        v = InterpolationUtil.format((String) v, System.getenv());
                    }
                    args.put(k, v);
                });
                injectedArgs = args;
            }
        }
        return ApiDocRenderer.loadFromPath(injectedArgs, path, classLoader);
    }

    private static <T> void setIf(Consumer<T> setter, T val) {
        if (val != null && (!(val instanceof Collection) || !((Collection<?>) val).isEmpty())) {
            setter.accept(val);
        }
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
