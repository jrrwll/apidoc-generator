package org.dreamcat.cli.generator.apidoc.renderer.text;

import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-03-21
 */
class TemplateUtilTest {

    @Test
    void test() {
        String s = TemplateUtil.process("${name}",
                Collections.singletonMap("name", "Jerry"), Collections.emptyMap());
        System.out.println(s);
    }
}
