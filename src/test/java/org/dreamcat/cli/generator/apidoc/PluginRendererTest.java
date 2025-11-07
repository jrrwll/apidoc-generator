package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.common.util.MapUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Jerry Will
 * @version 2024-01-09
 */
class PluginRendererTest extends BaseTest {

    String path = new File("simple-renderer-plugin/build/dep").getAbsolutePath();

    @Test
    void testController() throws Exception {
        ApiDocParseConfig config = buildConfigForController();

        ApiDocRenderer renderer = ApiDocRenderer.loadFromPath(
                MapUtil.of("a", 1, "b", 3.14,
                        "c", true, "d", Arrays.asList("pi", "e"),
                        "e", MapUtil.of("x", 1, "y", Collections.emptyList(), "z", "abc")), path);
        generate(config, renderer);
    }
}
