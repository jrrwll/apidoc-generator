package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
class ClasspathTest {

    @Test
    void test() throws Exception {
        String gradleRepo = System.getenv("HOME") + "/.gradle/caches/modules-2/files-2.1";
        String mavenRepo = System.getenv("HOME") + "/.m2/repository";
        List<String> classpath = Collections.singletonList("../build/classes/java/test");
        classpath = PathUtil.absolute(classpath);
        List<String> jarDirs = Arrays.asList(
                mavenRepo + "/org/springframework/spring-web/5.3.31",
                mavenRepo + "/org/springframework/spring-core/5.3.31",
                gradleRepo + "/org.springframework/spring-web/5.3.31",
                gradleRepo + "/org.springframework/spring-core/5.3.31");
        if (ObjectUtil.isNotEmpty(jarDirs)) {
            jarDirs = FileUtil.getAllFiles(jarDirs.stream().map(Paths::get).collect(Collectors.toList()))
                    .stream().map(Path::toFile).map(File::getAbsolutePath).collect(Collectors.toList());
        }
        classpath.addAll(jarDirs);
        ClassLoader userCodeClassLoader = ClassLoaderUtil.fromStringUrl(classpath);

        ApiDocParseConfig config = ApiDocParseConfig.fromAutoDetect();
        config.setSrcDirs(Collections.singletonList("../src/test/share"));
        config.setJavaFileDirs(Collections.singletonList("com/example/biz/controller"));

        ApiDocGenerator generator = new ApiDocGenerator(config, userCodeClassLoader);
        ApiDoc apiDoc = generator.getApiDoc();
        SwaggerRenderer renderer = new SwaggerRenderer();
        String doc = renderer.render(apiDoc);
        System.out.println(doc);
    }

}
