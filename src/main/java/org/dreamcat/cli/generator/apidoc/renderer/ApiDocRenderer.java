package org.dreamcat.cli.generator.apidoc.renderer;

import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
public interface ApiDocRenderer {

    String render(ApiDoc doc) throws IOException;

    default String getOutputFileSuffix() {
        return null;
    }

    static ApiDocRenderer loadFromPath(Map<String, Object> injectedArgs, String path) throws Exception {
        return loadFromPath(injectedArgs, path, Thread.currentThread().getContextClassLoader());
    }

    static ApiDocRenderer loadFromPath(Map<String, Object> injectedArgs, String path, ClassLoader parent) throws Exception {
        return loadFromPath(null, injectedArgs, path, parent);
    }

    @SuppressWarnings("unchecked")
    static ApiDocRenderer loadFromPath(String className, Map<String, Object> injectedArgs, String path, ClassLoader parent) throws Exception {
        ClassLoader cl = ClassLoaderUtil.fromDir(path, parent);

        List<String> classNames = ClassLoaderUtil.getServicesNames(ApiDocRenderer.class.getName(), cl);
        if (ObjectUtil.isEmpty(classNames)) {
            throw new IllegalArgumentException("SPI " + ApiDocRenderer.class.getName() +
                    " is not found in path: " + path);
        }
        if (className == null) {
            className = classNames.get(0);
        } else {
            if (!classNames.contains(className)) {
                throw new IllegalArgumentException("SPI " + className + " is not found in path: " + path);
            }
        }
        Class<ApiDocRenderer> rendererClass = (Class<ApiDocRenderer>) cl.loadClass(className);
        if (ObjectUtil.isNotEmpty(injectedArgs)) {
            return JsonUtil.fromMap(injectedArgs, rendererClass);
        } else {
            return ReflectUtil.newInstance(rendererClass);
        }
    }
}
