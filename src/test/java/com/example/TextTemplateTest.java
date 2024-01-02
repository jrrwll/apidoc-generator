package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-12
 */
class TextTemplateTest {

    String srcDir = new File("src/test/java").getAbsolutePath();
    List<String> basePackages = Collections.singletonList("com.example");

    static final String template = FunctionUtil.invokeOrNull(() -> ClassPathUtil.getResourceAsString(
            "IndentedTable.ftl"));
    static final String fields_template = FunctionUtil.invokeOrNull(() -> ClassPathUtil.getResourceAsString(
            "IndentedTable-fields.ftl"));
    static final String jwc_template = FunctionUtil.invokeOrNull(() -> ClassPathUtil.getResourceAsString(
            "JsonWithComment.ftl"));
    Map<String, String> includeTemplates = Collections.singletonMap("fields", fields_template);

    @Test
    void testController() throws Exception {
        generate(template, srcDir + "/com/example/controller");
    }

    @Test
    void testService() throws Exception {
        generate(template, srcDir + "/com/example/service");
    }

    @Test
    void testJWCController() throws Exception {
        generate(jwc_template, srcDir + "/com/example/controller");
    }

    @Test
    void testJWCService() throws Exception {
        generate(jwc_template, srcDir + "/com/example/service");
    }

    void generate(String template, String javaFileDir) throws Exception {
        Objects.requireNonNull(template, "template");
        ApiDocParserConfig config = new ApiDocParserConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setEnableSpringWeb(true);

        TextTemplateRenderer renderer = new TextTemplateRenderer(template, includeTemplates);
        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        String doc = generator.generate();
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
        System.out.println(doc);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
    }
}
