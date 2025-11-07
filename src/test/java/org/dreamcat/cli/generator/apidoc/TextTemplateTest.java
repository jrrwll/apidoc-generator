package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jerry Will
 * @version 2022-07-12
 */
class TextTemplateTest extends BaseTest {

    static final String template = FunctionUtil.invokeOrNull(() -> ClassLoaderUtil.getResourceAsString(
            "IndentedTable.ftl"));
    static final String fields_template = FunctionUtil.invokeOrNull(() -> ClassLoaderUtil.getResourceAsString(
            "IndentedTable-fields.ftl"));
    static final String jwc_template = FunctionUtil.invokeOrNull(() -> ClassLoaderUtil.getResourceAsString(
            "JsonWithComment.ftl"));
    Map<String, String> includeTemplates = Collections.singletonMap("fields", fields_template);

    @Test
    void testController() throws Exception {
        generate(template, buildConfigForController());
    }

    @Test
    void testService() throws Exception {
        generate(template, buildConfigForService());
    }

    @Test
    void testJWCController() throws Exception {
        generate(jwc_template, buildConfigForController());
    }

    @Test
    void testJWCService() throws Exception {
        generate(jwc_template, buildConfigForService());
    }

    void generate(String template, ApiDocParseConfig config ) throws Exception {
        Objects.requireNonNull(template, "template");

        TextTemplateRenderer renderer = new TextTemplateRenderer(template, includeTemplates);
        generate(config, renderer);
    }
}
