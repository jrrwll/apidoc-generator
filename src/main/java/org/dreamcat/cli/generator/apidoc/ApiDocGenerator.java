package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.dreamcat.cli.generator.apidoc.parser.ApiDocParser;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.databind.instance.RandomInstance;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
public class ApiDocGenerator {

    private final ApiDocParser parser;
    private final ApiDocRenderer renderer;
    @Getter
    private final RandomInstance randomInstance = new RandomInstance();

    public ApiDocGenerator(ApiDocConfig config, ApiDocRenderer renderer) {
        this(config, renderer, ApiDocGenerator.class.getClassLoader());
    }

    public ApiDocGenerator(ApiDocConfig config, ApiDocRenderer renderer, ClassLoader classLoader) {
        this.parser = new ApiDocParser(config, classLoader, randomInstance);
        this.renderer = renderer;
        // use classLoader on render
        if (renderer instanceof SwaggerRenderer) {
            ((SwaggerRenderer) renderer).setClassLoader(classLoader);
        }
    }

    @SneakyThrows
    public String generate() {
        try (StringWriter out = new StringWriter()) {
            generate(out);
            return out.toString();
        }
    }

    public void generate(Writer out) throws IOException {
        ApiDoc apiDoc = parser.parse();
        renderer.render(apiDoc, out);
    }

    public void generate(File outputFile) throws IOException {
        ApiDoc apiDoc = parser.parse();
        renderer.render(apiDoc, outputFile);
    }
}
