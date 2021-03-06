package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig.MergeInputParam;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.renderer.text.JsonWithCommentRenderer;
import org.dreamcat.common.util.CollectionUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
class JsonWithCommentTest {

    String srcDir = new File("src/test/java").getAbsolutePath();
    JsonWithCommentRenderer renderer = new JsonWithCommentRenderer();

    @Test
    void testController() throws Exception {
        String javaFileDir = srcDir + "/com/example/controller";
        List<String> basePackages = Collections.singletonList("com.example");

        renderer.setPinFunctionComment(true);
        renderer.setSeqPrefix("3.2.");
        renderer.setInputParamTitle(null);
        renderer.setOutputParamTitle("");
        generate(srcDir, javaFileDir, basePackages);
    }

    @Test
    void testService() throws Exception {
        String javaFileDir = srcDir + "/com/example/service";
        List<String> basePackages = Collections.singletonList("com.example");
        generate(srcDir, javaFileDir, basePackages);
    }

    void generate(String srcDir, String javaFileDir, List<String> basePackages) throws Exception {
        ApiDocConfig config = new ApiDocConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setUseJsonWithComment(true);
        config.setIgnoreInputParamTypes(CollectionUtil.setOf(
                "org.springframework.web.multipart.MultipartFile",
                "com.example.base.ApiContext"
        ));
        config.setEnableSpringWeb(true);
        config.setMergeInputParam(MergeInputParam.builder().byFlatType(true).build());

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        String doc = generator.generate();
        System.out.println(doc);
    }
}
