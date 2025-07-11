package org.dreamcat.cli.generator.apidoc.renderer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JsonUtil;

import java.io.Writer;
import java.util.Collections;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Getter
@RequiredArgsConstructor
@JsonInclude(Include.NON_EMPTY)
public class TextTemplateRenderer implements ApiDocRenderer {

    private final String template;
    private final Map<String, String> includeTemplates;

    public TextTemplateRenderer(String template) {
        this(template, Collections.emptyMap());
    }

    @Override
    public void render(ApiDoc apiDoc, Writer out) {
        Map<String, Object> context = JsonUtil.toMap(apiDoc);
        process(template, context, out, includeTemplates);
    }

    @SneakyThrows
    protected static void process(
            String content, Map<String, Object> context, Writer out, Map<String, String> includes) {
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        includes.forEach(templateLoader::putTemplate);
        templateLoader.putTemplate("", content);

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setTemplateLoader(templateLoader);
        cfg.setFallbackOnNullLoopVariable(false);

        Template template = cfg.getTemplate("");
        template.process(context, out);
    }
}
