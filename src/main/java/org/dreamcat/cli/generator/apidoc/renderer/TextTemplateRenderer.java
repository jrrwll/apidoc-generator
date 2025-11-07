package org.dreamcat.cli.generator.apidoc.renderer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.ObjectUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_EMPTY)
public class TextTemplateRenderer implements ApiDocRenderer {

    private String template;
    private Map<String, String> includeTemplates;

    @Override
    public String getOutputFileSuffix() {
        return "md";
    }

    @Override
    public String render(ApiDoc doc) throws IOException {
        AssertUtil.requireNotNull(template, "template");

        try (StringWriter out = new StringWriter()) {
            Map<String, Object> context = JsonUtil.toMap(doc);
            process(template, context, out, includeTemplates);
            return out.toString();
        }
    }

    @SneakyThrows
    protected static void process(
            String content, Map<String, Object> context, Writer out, Map<String, String> includes) {
        StringTemplateLoader templateLoader = new StringTemplateLoader();
        if (ObjectUtil.isNotEmpty(includes)) {
            includes.forEach(templateLoader::putTemplate);
        }
        templateLoader.putTemplate("", content);

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setTemplateLoader(templateLoader);
        cfg.setFallbackOnNullLoopVariable(false);

        Template template = cfg.getTemplate("");
        template.process(context, out);
    }
}
