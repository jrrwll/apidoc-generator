package org.dreamcat.cli.generator.apidoc.renderer.text;

import java.io.IOException;
import lombok.Setter;
import org.dreamcat.common.io.ClassPathUtil;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
@Setter
public class JsonWithCommentRenderer extends TextRenderer {

    public String getTemplate() {
        return template != null ? template : TEMPLATE;
    }

    private static final String TEMPLATE;

    static {
        try {
            TEMPLATE = ClassPathUtil.getResourceAsString(
                    "org/dreamcat/cli/generator/apidoc/JsonWithComment.ftl");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
