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
import lombok.SneakyThrows;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.asm.BeanMapUtil;
import org.dreamcat.common.io.UrlUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

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

    @SneakyThrows
    @SuppressWarnings("unchecked")
    static ApiDocRenderer loadFromPath(String path, String className,
            Map<String, Object> injectedArgs, ClassLoader classLoader) {
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
        URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]), classLoader);
        Class<ApiDocRenderer> rendererClass = (Class<ApiDocRenderer>) cl.loadClass(className);
        // todo optimize
        if (ObjectUtil.isNotEmpty(injectedArgs)) {
            return JsonUtil.fromMap(injectedArgs, rendererClass);
        } else {
            return ReflectUtil.newInstance(rendererClass);
        }
    }
}
