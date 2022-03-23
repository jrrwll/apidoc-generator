package org.dreamcat.cli.generator.apidoc.renderer.jwc;

import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-03-21
 */
class FreemarkerUtilTest {

    @Test
    void test() {
        String s = FreemarkerUtil.process("${name}",
                Collections.singletonMap("name", "Jerry"));
        System.out.println(s);
    }
}
