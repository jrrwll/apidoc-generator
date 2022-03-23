package org.dreamcat.cli.generator.apidoc.renderer.jwc;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import lombok.Setter;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.io.ClassPathUtil;
import org.dreamcat.common.x.jackson.JsonUtil;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
@Setter
public class JsonWithCommentRenderer implements ApiDocRenderer {

    private String template = freemarkerTemplate;
    private String nameHeader = "###";
    private String functionHeader = "####";
    private String inputParamNameHeader = ">";
    private String inputParamTitle = "- Input Params";
    private String outputParamTitle = "- Output Param";
    private String functionSep = "\n";
    private String groupSep = "\n";

    @Override
    public void render(ApiDoc apiDoc, Writer out) {
        Map<String, Object> context = JsonUtil.toMap(apiDoc);
        context.put("nameHeader", nameHeader);
        context.put("functionHeader", functionHeader);
        context.put("inputParamNameHeader", inputParamNameHeader);
        context.put("inputParamTitle", inputParamTitle);
        context.put("outputParamTitle", outputParamTitle);
        context.put("functionSep", functionSep);
        context.put("groupSep", groupSep);

        FreemarkerUtil.process(template, context, out);
    }

    private static final String freemarkerTemplate;

    static {
        try {
            freemarkerTemplate = ClassPathUtil.getResourceAsString(
                    "org/dreamcat/cli/generator/apidoc/JsonWithComment.ftl");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
