package org.dreamcat.cli.generator.apidoc.renderer;

import static org.dreamcat.cli.generator.apidoc.renderer.Companion.getOutputPath;

import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-01-14
 */
class CompanionTest {

    @Test
    void getOutputPathTest() {
        System.out.println(getOutputPath("", "md"));
        System.out.println(getOutputPath(".", "md"));
        System.out.println(getOutputPath("..", "md"));
        System.out.println(getOutputPath("/", "md"));
        System.out.println(getOutputPath("/../", "md"));
        System.out.println(getOutputPath("/.././..", "md"));
        System.out.println(getOutputPath("/a../.b/..c../../etc", "md"));
    }
}
