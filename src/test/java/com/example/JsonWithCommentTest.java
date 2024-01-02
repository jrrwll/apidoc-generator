package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.TextTemplateRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.SetUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
class JsonWithCommentTest {

    String srcDir = new File("src/test/java").getAbsolutePath();
    TextTemplateRenderer renderer = TextTemplateRenderer.builder()
            .jsnoWithComment().build();

    @Test
    void testController() throws Exception {
        renderer.setPinFunctionComment(true);
        renderer.setSeqPrefix("3.2.");
        renderer.setInputParamTitle(null);
        renderer.setOutputParamTitle("");

        String javaFileDir = srcDir + "/com/example/controller";
        List<String> basePackages = Collections.singletonList("com.example");
        generate(srcDir, javaFileDir, basePackages);
    }

    @Test
    void testService() throws Exception {
        String javaFileDir = srcDir + "/com/example/service";
        List<String> basePackages = Collections.singletonList("com.example");
        generate(srcDir, javaFileDir, basePackages);
    }


    void generate(String srcDir, String javaFileDir, List<String> basePackages) throws Exception {
        ApiDocParserConfig config = new ApiDocParserConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(SetUtil.of(
                "org.springframework.web.multipart.MultipartFile",
                "com.example.base.ApiContext"
        ));
        config.setEnableSpringWeb(true);
        config.setMergeInputParam(MergeInputParam.builder().byFlatType(true).build());

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        ApiDoc apiDoc = generator.parseOnly();
        System.out.println(JsonUtil.toJson(apiDoc));
        String doc = renderer.render(apiDoc);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
        System.out.println(doc);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
    }
}
