package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.renderer.JsnoWithCommentRenderer;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
public class JsonWithCommentBaseTest {

    String srcDir = new File("src/test/java").getAbsolutePath();
    List<String> basePackages = Collections.singletonList("com.example");

    JsnoWithCommentRenderer createRenderer() {
        return new JsnoWithCommentRenderer();
    }

    ApiDocParserConfig createConfig(String javaFileDir) {
        ApiDocParserConfig config = new ApiDocParserConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setAutoDetect(true);
        return config;
    }

    void generate(ApiDocParserConfig config, JsnoWithCommentRenderer renderer) throws Exception {
        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        String doc = generator.generate();
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
        System.out.println(doc);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
    }

}
