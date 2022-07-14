package org.dreamcat.cli.generator.apidoc.renderer.text;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import lombok.SneakyThrows;

/**
 * @author Jerry Will
 * @version 2022-03-20
 */
class TemplateUtil {

    private TemplateUtil() {
    }

    @SneakyThrows
    public static void process(
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

    public static String process(
            String content, Map<String, Object> context, Map<String, String> includes) {
        StringWriter stringWriter = new StringWriter();
        process(content, context, stringWriter, includes);
        return stringWriter.toString();
    }
}
