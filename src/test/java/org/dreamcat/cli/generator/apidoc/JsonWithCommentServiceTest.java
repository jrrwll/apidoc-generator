package org.dreamcat.cli.generator.apidoc;

import java.util.Arrays;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.FieldDoc;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.dreamcat.common.util.SetUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
class JsonWithCommentServiceTest extends JsonWithCommentBaseTest {

    String javaFileDir = srcDir + "/com/example/biz/service";

    @Test
    void test1() throws Exception {
        ApiDocParseConfig config = createConfig(javaFileDir);
        config.setIgnoreInputParamTypes(SetUtil.of(
                "org.springframework.web.multipart.MultipartFile",
                "com.example.base.ApiContext"
        ));

        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void test2() throws Exception {
        ApiDocParseConfig config = createConfig(javaFileDir);
        config.setIgnoreInputParamTypes(SetUtil.of(
                "org.springframework.web.multipart.MultipartFile",
                "com.example.base.ApiContext"
        ));

        JsnoWithCommentRenderer renderer = createRenderer();
        renderer.setSeqPrefix("5.1.");
        renderer.setInputParamTitle("- ##### Some Input Params");
        renderer.setOutputParamTitle(null);
        generate(config, renderer);
    }

    @Test
    void test3() throws Exception {
        ApiDocParseConfig config = createConfig(javaFileDir);
        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void testMergeInput() throws Exception {
        ApiDocParseConfig config = createConfig(javaFileDir);
        config.setMergeInputParam(MergeInputParam.flatType());

        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void testFieldDoc() throws Exception {
        ApiDocParseConfig config = createConfig(javaFileDir);
        config.setFieldDoc(Arrays.asList(
                new FieldDoc().setName("com.fasterxml.jackson.annotation.JsonProperty"),
                new FieldDoc().setName("com.example.annotation.FieldDoc")));

        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }
}
