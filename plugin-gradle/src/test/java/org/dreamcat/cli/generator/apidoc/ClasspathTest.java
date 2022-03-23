package org.dreamcat.cli.generator.apidoc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.io.PathUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
class ClasspathTest {


    @Test
    void test() throws Exception {
        String gradleRepo = System.getenv("HOME") + "/.gradle/caches/modules-2/files-2.1";
        List<String> classpath = Collections.singletonList("../build/classes/java/test");
        classpath = PathUtil.absolute(classpath);
        List<String> jarDirs = Arrays.asList(
                gradleRepo + "/org.springframework/spring-web/5.3.17",
                gradleRepo + "/org.springframework/spring-core/5.3.17");
        if (ObjectUtil.isNotEmpty(jarDirs)) {
            jarDirs = ApiDocGeneratorUtil.treeClassPath(jarDirs);
        }
        classpath.addAll(jarDirs);
        ClassLoader userCodeClassLoader = ApiDocGeneratorUtil.buildUserCodeClassLoader(classpath);

        ApiDocConfig config = new ApiDocConfig();
        config.setSrcDirs(Collections.singletonList("../src/test/java"));
        config.setJavaFileDirs(Collections.singletonList("com/example/controller"));
        config.setUseRelativeJavaFilePath(true);
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"));
        config.setEnableSpringWeb(true);
        config.setJsonWithComment(true);

        SwaggerRenderer renderer = new SwaggerRenderer();
        ApiDocGenerator generator = new ApiDocGenerator(config, renderer, userCodeClassLoader);
        String doc = generator.generate();
        System.out.println(doc);
    }

}
