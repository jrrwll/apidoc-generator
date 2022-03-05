package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.parser.ApiDocParser;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
public class ApiDocGenerator<T> {

    private final ApiDocParser parser;
    private final ApiDocRenderer<T> renderer;

    public ApiDocGenerator(ApiDocConfig config, ApiDocRenderer<T> renderer) {
        this.parser = new ApiDocParser(config);
        this.renderer = renderer;
    }

    public T generate() throws Exception {
        ApiDoc apiDoc = parser.parse();
        return renderer.render(apiDoc);
    }
}
