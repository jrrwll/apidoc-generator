package org.dreamcat.cli.generator.apidoc.renderer;

import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
public interface ApiDocRenderer<T> {

    T render(ApiDoc apiDoc);
}
