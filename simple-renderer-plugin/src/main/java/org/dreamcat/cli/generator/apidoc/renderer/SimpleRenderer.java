package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.IOException;
import java.io.Writer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JSON;

/**
 * @author Jerry Will
 * @version 2024-01-08
 */
public class SimpleRenderer implements ApiDocRenderer {

    @Override
    public void render(ApiDoc doc, Writer out) throws IOException {
        System.out.println("*** output of simple renderer plugin *** ");
        System.out.println(JSON.stringify(doc));
        System.out.println("******             end            ******");
    }
}
