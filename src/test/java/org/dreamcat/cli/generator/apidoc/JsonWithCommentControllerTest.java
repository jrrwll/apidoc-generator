package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
public class JsonWithCommentControllerTest extends BaseTest {

    @Test
    void test1() throws Exception {
        ApiDocParseConfig config = buildConfigForController();
        JsnoWithCommentRenderer renderer = new JsnoWithCommentRenderer();
        generate(config, renderer);
    }

    @Test
    void test2() throws Exception {
        ApiDocParseConfig config = buildConfigForController();

        JsnoWithCommentRenderer renderer = new JsnoWithCommentRenderer();
        renderer.setPinFunctionComment(true);
        renderer.setSeqPrefix("3.2.");
        renderer.setInputParamTitle(null);
        renderer.setOutputParamTitle("");
        generate(config, renderer);
    }

    @Test
    void testMergeInput() throws Exception {
        ApiDocParseConfig config = buildConfigForController();
        config.setMergeInputParam(MergeInputParam.flatType());

        JsnoWithCommentRenderer renderer = new JsnoWithCommentRenderer();
        generate(config, renderer);
    }

    @Test
    void testOutputParamAsIndentedTable() throws Exception {
        ApiDocParseConfig config = buildConfigForController();

        JsnoWithCommentRenderer renderer = new JsnoWithCommentRenderer();
        renderer.setOutputParamAsIndentedTable(true);
        renderer.setFieldsNoRequired(true);
        generate(config, renderer);
    }
}
