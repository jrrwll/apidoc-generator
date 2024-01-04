package com.example;

import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
public class JsonWithCommentControllerTest extends JsonWithCommentBaseTest {

    String javaFileDir = srcDir + "/com/example/controller";

    @Test
    void test1() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);
        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void test2() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);

        JsnoWithCommentRenderer renderer = createRenderer();
        renderer.setPinFunctionComment(true);
        renderer.setSeqPrefix("3.2.");
        renderer.setInputParamTitle(null);
        renderer.setOutputParamTitle("");
        generate(config, renderer);
    }

    @Test
    void testMergeInput() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);
        config.setMergeInputParam(MergeInputParam.builder().byFlatType(true).build());

        JsnoWithCommentRenderer renderer = createRenderer();
        generate(config, renderer);
    }

    @Test
    void testOutputParamAsIndentedTable() throws Exception {
        ApiDocParserConfig config = createConfig(javaFileDir);

        JsnoWithCommentRenderer renderer = createRenderer();
        renderer.setOutputParamAsIndentedTable(true);
        renderer.setFieldsNoRequired(true);
        generate(config, renderer);
    }
}