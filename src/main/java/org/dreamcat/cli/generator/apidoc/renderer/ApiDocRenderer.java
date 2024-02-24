package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import lombok.SneakyThrows;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.io.IOUtil;
import org.dreamcat.common.io.UrlUtil;
import org.dreamcat.common.javac.FileClassLoader;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.ClassPathUtil;
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

    static ApiDocRenderer loadFromPath(String path,
            Map<String, Object> injectedArgs) throws Exception {
        return loadFromPath(path, injectedArgs,
                Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings("unchecked")
    static ApiDocRenderer loadFromPath(String path,
            Map<String, Object> injectedArgs, ClassLoader classLoader) throws Exception {
        ClassLoader cl = ClassLoaderUtil.fromDir(path, classLoader);

        String className = ClassPathUtil.getServicesName(ApiDocRenderer.class.getName(), cl);
        if (className == null) {
            throw new RemoteException("SPI " + ApiDocRenderer.class.getName() +
                    " is not found in path: " + path);
        }

        Class<ApiDocRenderer> rendererClass = (Class<ApiDocRenderer>) cl.loadClass(className);
        if (ObjectUtil.isNotEmpty(injectedArgs)) {
            return JsonUtil.fromMap(injectedArgs, rendererClass);
        } else {
            return ReflectUtil.newInstance(rendererClass);
        }
    }
}
