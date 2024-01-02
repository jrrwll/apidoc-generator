package com.example;

import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.common.util.SetUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
class JsonWithCommentServiceTest extends JsonWithCommentBaseTest {

    String javaFileDir = srcDir + "/com/example/service";

    @Test
    void test1() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);
        config.setIgnoreInputParamTypes(SetUtil.of(
                "org.springframework.web.multipart.MultipartFile",
                "com.example.base.ApiContext"
        ));

        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void test2() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);
        config.setIgnoreInputParamTypes(SetUtil.of(
                "org.springframework.web.multipart.MultipartFile",
                "com.example.base.ApiContext"
        ));

        JsnoWithCommentRenderer renderer = createRenderer();
        renderer.setSeqPrefix("3.2.");
        renderer.setInputParamTitle("##### - Some Input Params");
        renderer.setOutputParamTitle(null);
        generate(config, renderer);
    }

    @Test
    void test3() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);
        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void testMergeInput() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);
        config.setMergeInputParam(MergeInputParam.builder().byFlatType(true).build());

        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }
}
