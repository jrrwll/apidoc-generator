package org.dreamcat.cli.generator.apidoc;

import lombok.RequiredArgsConstructor;
import org.dreamcat.cli.generator.apidoc.parser.ApiDocParser;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.reflect.ObjectRandomGenerator;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@RequiredArgsConstructor
public class ApiDocGenerator {

    private final ApiDocParser parser;
    private final ApiDocRenderer renderer;

    public ApiDocGenerator(ApiDocParseConfig config, ApiDocRenderer renderer) {
        this(config, renderer, Thread.currentThread().getContextClassLoader());
    }

    public ApiDocGenerator(ApiDocParseConfig config, ApiDocRenderer renderer, ClassLoader classLoader) {
        this(new ApiDocParser(config, classLoader), renderer);
    }

    public ApiDocGenerator(ApiDocParseConfig config, ApiDocRenderer renderer,
            ClassLoader classLoader, ObjectRandomGenerator randomGenerator) {
        this(new ApiDocParser(config, classLoader, randomGenerator), renderer);
    }

    public String generate() throws IOException {
        try (StringWriter out = new StringWriter()) {
            generate(out);
            return out.toString();
        }
    }

    public void generate(Writer out) throws IOException {
        renderer.render(parse(), out);
    }

    public void generate(File outputFile) throws IOException {
        renderer.render(parse(), outputFile);
    }

    public ApiDoc parse() {
        return parser.parse();
    }
}
