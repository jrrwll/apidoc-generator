package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Swagger;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FieldDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FunctionDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.Http;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorUtil {

    private ApiDocGeneratorUtil() {
    }

    public static ApiDocParseConfig buildApiDocConfig(ApiDocGeneratorExtension extension, Project project) {
        List<String> srcDirs = GradleUtil.getSrcDirs(project).stream()
                .map(File::getPath).collect(Collectors.toList());

        ApiDocParseConfig config = new ApiDocParseConfig();
        config.setSrcDirs(srcDirs);

        setIf(config::setVerbose, extension.getVerbose());
        setIf(config::setBasePackages, extension.getBasePackages());
        config.setJavaFileDirs(extension.getJavaFileDirs().get());

        config.setIgnoreInputParamTypes(new HashSet<>(extension.getIgnoreInputParamTypes().get()));
        if (extension.getMergeInputParam().getOrElse(false)) {
            config.setMergeInputParam(MergeInputParam.flatType());
        }

        config.setAutoDetect(extension.getAutoDetect().get());
        List<Http> httpList = extension.getHttp().getAsMap().values().stream().map(it -> {
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

        List<FunctionDoc> functionDocs = extension.getFunctionDoc()
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

        List<FieldDoc> fieldDocs = extension.getFieldDoc()
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
        setIf(renderer::setRequiredNull, text.getRequiredNull());
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
        String path = rendererPlugin.getPath().get();
        Map<String, Object> injectedArgs = rendererPlugin.getInjectedArgs().getOrNull();
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
        return ApiDocRenderer.loadFromPath(path, injectedArgs, classLoader);
    }

    private static <T> void setIf(Consumer<T> setter, Provider<T> provider) {
        T val = provider.getOrNull();
        if (val != null && (!(val instanceof Collection) || !((Collection<?>) val).isEmpty())) {
            setter.accept(val);
        }
    }
}
