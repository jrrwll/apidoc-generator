package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import lombok.SneakyThrows;
import org.dreamcat.cli.generator.apidoc.parser.ApiDocParser;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.reflect.ObjectRandomGenerator;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
public class ApiDocGenerator {

    private final ApiDocParser parser;
    private final ApiDocRenderer renderer;
    private final ObjectRandomGenerator randomGenerator = new ObjectRandomGenerator();

    public ObjectRandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public ApiDocGenerator(ApiDocParserConfig config, ApiDocRenderer renderer) {
        this(config, renderer, ApiDocGenerator.class.getClassLoader());
    }

    public ApiDocGenerator(ApiDocParserConfig config, ApiDocRenderer renderer, ClassLoader classLoader) {
        this.parser = new ApiDocParser(config, classLoader, randomGenerator);
        this.renderer = renderer;
        this.renderer.setClassLoader(classLoader);
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
