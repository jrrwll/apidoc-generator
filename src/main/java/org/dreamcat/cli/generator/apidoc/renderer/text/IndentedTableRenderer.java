package org.dreamcat.cli.generator.apidoc.renderer.text;

import java.io.IOException;
import lombok.Setter;
import org.dreamcat.common.io.ClassPathUtil;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Setter
public class IndentedTableRenderer extends TextRenderer {

    public String getTemplate() {
        return template != null ? template : TEMPLATE;
    }

    private static final String TEMPLATE;

    static {
        try {
            TEMPLATE = ClassPathUtil.getResourceAsString(
                    "org/dreamcat/cli/generator/apidoc/IndentedTable.ftl");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
