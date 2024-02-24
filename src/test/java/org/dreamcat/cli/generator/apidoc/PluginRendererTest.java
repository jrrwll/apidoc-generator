package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.common.util.MapUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2024-01-09
 */
class PluginRendererTest {

    String srcDir = new File("src/test/java").getAbsolutePath();
    List<String> basePackages = Collections.singletonList("com.example.biz");
    String path = new File("simple-renderer-plugin/build/dep").getAbsolutePath();

    @Test
    void testController() throws Exception {
        ApiDocRenderer renderer = ApiDocRenderer.loadFromPath(
                path, MapUtil.of("a", 1, "b", 3.14,
                        "c", true, "d", Arrays.asList("pi", "e"),
                        "e", MapUtil.of("x", 1, "y", Collections.emptyList(), "z", "abc")));
        generate(renderer, srcDir + "/com/example/biz/controller");
    }

    void generate(ApiDocRenderer renderer, String javaFileDir) throws Exception {
        ApiDocParseConfig config = new ApiDocParseConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setAutoDetect(true);

        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        generator.generate();
    }
}
