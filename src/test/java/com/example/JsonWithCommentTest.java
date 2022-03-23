package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.renderer.jwc.JsonWithCommentRenderer;
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
        config.setJsonWithComment(true);
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setEnableSpringWeb(true);

        JsonWithCommentRenderer renderer = new JsonWithCommentRenderer();
        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        String doc = generator.generate();
        System.out.println(doc);
    }
}
