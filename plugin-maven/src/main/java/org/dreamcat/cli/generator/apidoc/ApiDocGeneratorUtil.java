package org.dreamcat.cli.generator.apidoc;

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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.project.MavenProject;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FieldDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FunctionDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.Http;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApidocGeneratorMojo.Swagger;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.io.UrlUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorUtil {

    public static ClassLoader buildUserCodeClassLoader(MavenProject project) throws Exception{
        File classDir = MavenUtil.getClassDir(project);
        List<String> classpath = MavenUtil.getCompileClasspath(project);

        URL[] urls = Stream.concat(Stream.of(classDir), classpath.stream().map(File::new))
                .map(UrlUtil::toURL).toArray(URL[]::new);
        return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    public static ApiDocParseConfig buildApiDocConfig(ApidocGeneratorMojo mojo, MavenProject project)
            throws IOException {
        File srcDir = MavenUtil.getSrcDir(project);

        ApiDocParseConfig config = new ApiDocParseConfig();
        config.setSrcDirs(Collections.singletonList(srcDir.getAbsolutePath()));

        setIf(config::setVerbose, mojo.getVerbose());
        setIf(config::setBasePackages, mojo.getBasePackages());
        config.setJavaFileDirs(mojo.getJavaFileDirs().get());

        config.setIgnoreInputParamTypes(new HashSet<>(mojo.getIgnoreInputParamTypes()));
        if (mojo.getMergeInputParam()) {
            config.setMergeInputParam(MergeInputParam.flatType());
        }

        config.setAutoDetect(mojo.getAutoDetect());
        List<Http> httpList = mojo.getHttp().getAsMap().values().stream().map(it -> {
            Http http = new Http();
            setIf(http::setPath, it.getPath());
            setIf(http::setPathMethod, it.getPathMethod());
            setIf(http::setAction, it.getAction());
            setIf(http::setActionMethod, it.getActionMethod());
            setIf(http::setPathVar, it.getPathVar());
            setIf(http::setPathVarMethod, it.getPathVarMethod());
            setIf(http::setRequired, it.getRequired());
            setIf(http::setRequiredMethod, it.getRequiredMethod());
            if (http.getPath() == null && http.getAction() == null &&
                    http.getPathVar() == null && http.getRequired() == null) {
                return null;
            }
            return http;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!httpList.isEmpty()) config.setHttp(httpList);

        List<FunctionDoc> functionDocs = mojo.getFunctionDoc()
                .getAsMap().values().stream().map(it -> {
                    FunctionDoc doc = new FunctionDoc();
                    setIf(doc::setName, it.getAnnotationName());
                    setIf(doc::setCommentMethod, it.getCommentMethod());
                    setIf(doc::setNestedParamMethod, it.getNestedParamMethod());
                    setIf(doc::setNestedParamNameMethod, it.getNestedParamNameMethod());
                    setIf(doc::setNestedParamCommentMethod, it.getNestedParamCommentMethod());
                    setIf(doc::setNestedParamRequiredMethod, it.getNestedParamRequiredMethod());
                    if (doc.getName() == null) return null;
                    return doc;
                }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!functionDocs.isEmpty()) config.setFunctionDoc(functionDocs);

        List<FieldDoc> fieldDocs = mojo.getFieldDoc()
                .getAsMap().values().stream().map(it -> {
                    FieldDoc doc = new FieldDoc();
                    setIf(doc::setName, it.getAnnotationName());
                    setIf(doc::setNameMethod, it.getNameMethod());
                    setIf(doc::setCommentMethod, it.getCommentMethod());
                    setIf(doc::setRequiredMethod, it.getRequiredMethod());
                    if (doc.getName() == null) return null;
                    return doc;
                }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!functionDocs.isEmpty()) config.setFieldDoc(fieldDocs);
        return config;
    }

    public static ApiDocRenderer buildJsonWithCommentRenderer(JsonWithComment text) {
        String template = text.getTemplate();
        if (template != null) {
            Map<String, String> includeTemplates = text.getIncludeTemplates();
            if (includeTemplates == null) includeTemplates = Collections.emptyMap();
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

    public static ApiDocRenderer buildSwaggerRenderer(Swagger swagger) {
        SwaggerRenderer renderer = new SwaggerRenderer();
        setIf(renderer::setDefaultTitle, swagger.getDefaultTitle());
        setIf(renderer::setDefaultVersion, swagger.getDefaultVersion());
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
        return ApiDocRenderer.loadFromPath(path, injectedArgs, classLoader);
    }

    private static <T> void setIf(Consumer<T> setter, T val) {
        if (val != null && (!(val instanceof Collection) || !((Collection<?>) val).isEmpty())) {
            setter.accept(val);
        }
    }
}
