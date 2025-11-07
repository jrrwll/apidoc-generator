package org.dreamcat.cli.generator.apidoc;


import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
public class BaseTest {

    String srcDir = new File("src/test/share").getAbsolutePath();
    List<String> basePackages = Collections.singletonList("com.example.biz");

    protected void generate(ApiDocParseConfig config, ApiDocRenderer renderer) throws Exception {
        generate(config, renderer, Thread.currentThread().getContextClassLoader());
    }

    protected void generate(ApiDocParseConfig config, ApiDocRenderer renderer, ClassLoader classLoader) throws Exception {
        ApiDocGenerator generator = new ApiDocGenerator(config, classLoader);
        String doc = generator.generate(renderer);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
        System.out.println(doc);
        System.out.println("--- --- ---   --- --- ---   --- --- ---");
    }



    protected ApiDocParseConfig buildConfigForController() {
        ApiDocParseConfig config = buildConfig();
        config.setJavaFileDirs(Collections.singletonList(srcDir + "/com/example/biz/controller"));
        return config;
    }

    protected ApiDocParseConfig buildConfigForService() {
        ApiDocParseConfig config = buildConfig();
        config.setJavaFileDirs(Collections.singletonList(srcDir + "/com/example/biz/service"));
        return config;
    }

    private ApiDocParseConfig buildConfig() {
        ApiDocParseConfig config = new ApiDocParseConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setAutoDetect(true);
        return config;
    }
}
