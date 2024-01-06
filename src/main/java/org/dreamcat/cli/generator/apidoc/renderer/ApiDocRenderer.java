package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.asm.BeanMapUtil;
import org.dreamcat.common.io.UrlUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;

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

    static ApiDocRenderer loadFromPath(String path, Map<String, Object> injectedArgs,
            ClassLoader classLoader) {
        File file = new File(path);
        List<URL> urls = new ArrayList<>();
        if (file.isDirectory()) {
            File[] subFiles = Objects.requireNonNull(file.listFiles(),
                    "listFiles is null: " + path);
            for (File subFile : subFiles) {
                urls.add(UrlUtil.toURL(subFile.toURI()));
            }
        } else {
            urls.add(UrlUtil.toURL(file.toURI()));
        }
        URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]), classLoader);
        // load from SPI
        Iterator<ApiDocRenderer> loader = ServiceLoader.load(
                ApiDocRenderer.class, urlClassLoader).iterator();
        if (!loader.hasNext()) {
            throw new RuntimeException("ApiDocRenderer SPI is not found in " + path);
        }
        ApiDocRenderer renderer = loader.next();
        // todo optimize
        if (ObjectUtil.isNotEmpty(injectedArgs)) {
            ApiDocRenderer configuredRenderer = JsonUtil.fromMap(injectedArgs, renderer.getClass());
            BeanMapUtil.copy(configuredRenderer, renderer);
        }
        return renderer;
    }
}
