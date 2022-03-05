package com.example;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocGenerator;
import org.dreamcat.cli.generator.apidoc.renderer.SwaggerRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.Swagger;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-01-07
 */
class SwaggerTest {

    String srcDir = new File("src/test/java").getAbsolutePath();

    @Test
    void testController() throws Exception {
        String javaFileDir = srcDir + "/com/example/controller";
        List<String> basePackages = Collections.singletonList("com.example");

        ApiDocConfig config = new ApiDocConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setEnableSpringWeb(true);

        SwaggerRenderer renderer = new SwaggerRenderer();

        ApiDocGenerator<Swagger> generator = new ApiDocGenerator<>(config, renderer);
        Swagger swagger = generator.generate();
        String doc = swagger.toYaml();
        System.out.println(doc);
    }

}
