package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-12
 */
class TextTemplateTest {

    String srcDir = new File("src/test/share").getAbsolutePath();
    List<String> basePackages = Collections.singletonList("com.example.biz");

    static final String template = FunctionUtil.invokeOrNull(() -> ClassLoaderUtil.getResourceAsString(
            "IndentedTable.ftl"));
    static final String fields_template = FunctionUtil.invokeOrNull(() -> ClassLoaderUtil.getResourceAsString(
            "IndentedTable-fields.ftl"));
    static final String jwc_template = FunctionUtil.invokeOrNull(() -> ClassLoaderUtil.getResourceAsString(
            "JsonWithComment.ftl"));
    Map<String, String> includeTemplates = Collections.singletonMap("fields", fields_template);

    @Test
    void testController() throws Exception {
        generate(template, srcDir + "/com/example/biz/controller");
    }

    @Test
    void testService() throws Exception {
        generate(template, srcDir + "/com/example/biz/service");
    }

    @Test
    void testJWCController() throws Exception {
        generate(jwc_template, srcDir + "/com/example/biz/controller");
    }

    @Test
    void testJWCService() throws Exception {
        generate(jwc_template, srcDir + "/com/example/biz/service");
    }

    void generate(String template, String javaFileDir) throws Exception {
        Objects.requireNonNull(template, "template");
        ApiDocParseConfig config = new ApiDocParseConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setAutoDetect(true);

        TextTemplateRenderer renderer = new TextTemplateRenderer(template, includeTemplates);
        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        String doc = generator.generate();
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
        System.out.println(doc);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
    }
}
