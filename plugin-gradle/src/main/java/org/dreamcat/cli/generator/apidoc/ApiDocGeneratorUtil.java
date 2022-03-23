package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.dreamcat.common.javac.FileClassLoader;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorUtil {

    private ApiDocGeneratorUtil() {
    }

    public static ClassLoader buildUserCodeClassLoader(List<String> classpath) {
        List<URL> urls = classpath.stream()
                .map(ApiDocGeneratorUtil::buildURL).collect(Collectors.toList());

        ClassLoader parent = ApiDocGeneratorUtil.class.getClassLoader();
        return new FileClassLoader(urls.toArray(new URL[0]), parent);
        // return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    @SneakyThrows
    private static URL buildURL(String path) {
        return new URL("file", null, -1, path);
    }

    public static List<String> treeClassPath(List<String> cp) {
        List<String> paths = new ArrayList<>();
        treeClassPath(cp, paths);
        return paths;
    }

    public static void treeClassPath(List<String> cp, List<String> paths) {
        for (String path : cp) {
            File file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                if (file.getName().endsWith(".jar") || file.getName().endsWith(".class") ||
                        file.getName().endsWith(".java")) {
                    paths.add(path);
                }
                continue;
            }
            File[] files = file.listFiles();
            if (files == null) continue;
            for (File f : files) {
                treeClassPath(Collections.singletonList(f.getAbsolutePath()), paths);
            }
        }
    }
}
