package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.renderer.JsonWithCommentRenderer;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
class JsonWithCommentTest {


    @Test
    void testController() throws Exception {
        String srcDir = new File("src/test/java").getAbsolutePath();
        String javaFileDir = srcDir + "/com/example/controller";
        List<String> basePackages = Collections.singletonList("com.example");
        generate(srcDir, javaFileDir, basePackages);
    }

    @Test
    void testService() throws Exception {
        String srcDir = new File("src/test/java").getAbsolutePath();
        String javaFileDir = srcDir + "/com/example/service";
        List<String> basePackages = Collections.singletonList("com.example");
        generate(srcDir, javaFileDir, basePackages);
    }

    void generate(String srcDir, String javaFileDir, List<String> basePackages) throws Exception {
        ApiDocConfig config = new ApiDocConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setEnableSpringWeb(true);

        JsonWithCommentRenderer renderer = new JsonWithCommentRenderer(config);
        renderer.setSeqFormat("#### 4.1.%d");
        renderer.setInputParamTitle("input param");
        renderer.setOutputParamTitle("output param");

        ApiDocGenerator<String> generator = new ApiDocGenerator<>(config, renderer);
        String doc = generator.generate();
        System.out.println(doc);
    }
}
