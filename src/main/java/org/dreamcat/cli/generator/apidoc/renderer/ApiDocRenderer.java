package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
public interface ApiDocRenderer {

    void render(ApiDoc doc, Writer out) throws IOException;

    default void render(ApiDoc doc, File outputFile) throws IOException {
        try (FileWriter out = new FileWriter(outputFile)) {
            render(doc, out);
        }
    }

    default String render(ApiDoc doc) {
        try (StringWriter out = new StringWriter()) {
            render(doc, out);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
