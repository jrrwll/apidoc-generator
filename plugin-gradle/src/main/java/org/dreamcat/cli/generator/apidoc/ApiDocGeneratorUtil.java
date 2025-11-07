package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.HttpPush;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.JsonWithComment;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.RendererPlugin;
import org.dreamcat.cli.generator.apidoc.ApiDocGeneratorExtension.Swagger;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FieldDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FunctionDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.ServiceDoc;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.HttpPushConfig;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.Pair;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorUtil {

    private ApiDocGeneratorUtil() {
    }

    public static ApiDocParseConfig buildApiDocConfig(ApiDocGeneratorExtension extension, List<String> srcDirs) {
        ApiDocParseConfig config;
        String extraConfigJson = extension.getExtraConfigJson().getOrNull();
        if (extraConfigJson != null) {
            config = JsonUtil.fromJson(extraConfigJson, ApiDocParseConfig.class);
        } else {
            config = new ApiDocParseConfig();
        }
        config.setSrcDirs(srcDirs);

        setIf(config::setVerbose, extension.getVerbose());
        setIf(config::setBasePackages, extension.getBasePackages());
        config.setJavaFileDirs(extension.getJavaFileDirs().get());

        config.setIgnoreInputParamTypes(new HashSet<>(extension.getIgnoreInputParamTypes()
                .getOrElse(Collections.emptyList())));
        config.setIgnoreParamNames(new HashSet<>(extension.getIgnoreParamNames()
                .getOrElse(Collections.emptyList())));

        if (extension.getMergeInputParam().getOrElse(false)) {
            config.setMergeInputParam(MergeInputParam.flatType());
        }

        config.setAutoDetect(extension.getAutoDetect().get());

        List<ServiceDoc> serviceDocs = extension.getServiceDoc().getAsMap().values().stream().map(it -> {
            ServiceDoc doc = new ServiceDoc();
            setIf(doc::setName, it.getAnnotationName());
            setIf(doc::setNameMethod, it.getNameMethod());
            setIf(doc::setCommentMethod, it.getCommentMethod());
            return doc.getName() != null ? doc : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (!serviceDocs.isEmpty()) config.setServiceDoc(serviceDocs);

        List<FunctionDoc> functionDocs = extension.getFunctionDoc().getAsMap().values().stream().map(it -> {
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

        List<FieldDoc> fieldDocs = extension.getFieldDoc().getAsMap().values().stream().map(it -> {
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

    public static ApiDocRenderer buildJsonWithCommentRenderer(JsonWithComment text) {
        String template = text.getTemplate().getOrNull();
        if (template != null) {
            Map<String, String> includeTemplates = text.getIncludeTemplates()
                    .getOrElse(Collections.emptyMap());
            return new TextTemplateRenderer(template, includeTemplates);
        }

        String lang = text.getLang().getOrNull();
        if (lang == null && text.getI18n().getOrElse(false)) {
            lang = System.getenv("LANG");
        }
        JsnoWithCommentRenderer renderer;
        if (lang != null) {
            renderer = JsnoWithCommentRenderer.fromI18n(lang);
        } else {
            renderer = new JsnoWithCommentRenderer();
        }
        setIf(renderer::setFieldsNoRequired, text.getFieldsNoRequired());
        setIf(renderer::setOutputParamAsIndentedTable, text.getOutputParamAsIndentedTable());

        setIf(renderer::setNameHeader, text.getNameHeader());
        setIf(renderer::setFunctionHeader, text.getFunctionHeader());
        setIf(renderer::setInputParamTitle, text.getInputParamTitle());
        setIf(renderer::setOutputParamTitle, text.getOutputParamTitle());
        setIf(renderer::setPinFunctionComment, text.getPinFunctionComment());
        setIf(renderer::setSeqPrefix, text.getSeqPrefix());
        setIf(renderer::setSeqOffset, text.getSeqOffset());

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
        setIf(renderer::setSwagger2, swagger.getSwagger2());
        setIf(renderer::setDefaultVersion, swagger.getDefaultVersion());
        setIf(renderer::setFormatAsJson, swagger.getFormatAsJson());

        HttpPush httpPush = swagger.getHttpPush();
        if (httpPush != null && httpPush.getUrl().isPresent()) {
            HttpPushConfig httpPushConfig = new HttpPushConfig();
            httpPushConfig.setUrl(httpPush.getUrl().get());
            httpPushConfig.setHeaders(httpPush.getHeaders().getOrNull());

            Map<String, Object> json = httpPush.getJson().getOrNull();
            // Object in MapProperty<String, Object> could be a Property, so revolse it
            if (ObjectUtil.isNotEmpty(json)) {
                json = json.entrySet().stream().map(it -> {
                    Object value = it.getValue();
                    if (value instanceof Property) {
                        value = ((Property<?>) value).getOrNull();
                    }
                    return Pair.of(it.getKey(), value);
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                httpPushConfig.setJson(json);
            }

            httpPushConfig.setText(httpPush.getText().getOrNull());
            httpPushConfig.setForm(httpPush.getForm().getOrNull());
            renderer.setHttpPush(httpPushConfig);
        }
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
        return ApiDocRenderer.loadFromPath(injectedArgs, path, classLoader);
    }

    private static <T> void setIf(Consumer<T> setter, Provider<T> provider) {
        T val = provider.getOrNull();
        if (val != null && (!(val instanceof Collection) || !((Collection<?>) val).isEmpty())) {
            setter.accept(val);
        }
    }
}
