package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.net.UrlUtil;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.junit.jupiter.api.Test;

/**
 * <a href="https://editor.swagger.io/">swagger editor</a>
 *
 * @author Jerry Will
 * @version 2022-01-07
 */
class SwaggerTest {

    String srcDir = new File("src/test/share").getAbsolutePath();

    @Test
    void testController() throws Exception {
        String javaFileDir = srcDir + "/com/example/biz/controller";
        List<String> basePackages = Collections.singletonList("com.example.biz");

        ApiDocParseConfig config = new ApiDocParseConfig();
        config.setBasePackages(basePackages);
        config.setSrcDirs(Collections.singletonList(srcDir));
        config.setJavaFileDirs(Collections.singletonList(javaFileDir));
        config.setIgnoreInputParamTypes(Collections.singleton(
                "org.springframework.web.multipart.MultipartFile"
        ));
        config.setAutoDetect(true);

        SwaggerRenderer renderer = new SwaggerRenderer();
        ApiDocGenerator generator = new ApiDocGenerator(config, renderer);
        String doc = generator.generate();
        System.out.println(doc);
    }

    @Test
    void testApiGenerate() throws Exception {
        String configJson = ClassLoaderUtil.getResourceAsString("config.json");
        ApiDocParseConfig config = JsonUtil.fromJson(configJson, ApiDocParseConfig.class);

        SwaggerRenderer renderer = new SwaggerRenderer();

        String home_dir = System.getProperty("user.home");
        List<String> urls = Arrays.asList(
                "plugin-gradle/example/build/classes/java/main/",
                home_dir + "/.m2/repository/org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar",
                home_dir + "/.m2/repository/org/springframework/spring-web/5.3.31/spring-web-5.3.31.jar",
                home_dir + "/.m2/repository/org/springframework/spring-beans/5.3.31/spring-beans-5.3.31.jar",
                home_dir + "/.m2/repository/org/springframework/spring-core/5.3.31/spring-core-5.3.31.jar",
                home_dir + "/.m2/repository/javax/validation/validation-api/2.0.1.Final/validation-api-2.0.1.Final.jar",
                home_dir + "/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar",
                home_dir + "/.m2/repository/org/springframework/spring-jcl/5.3.31/spring-jcl-5.3.31.jar"
        );
        ClassLoader classLoader = new URLClassLoader(urls.stream()
                .map(File::new).map(UrlUtil::toURL).toArray(URL[]::new));

        ApiDocGenerator generator = new ApiDocGenerator(
                config, renderer, classLoader);
        String doc = generator.generate();
        System.out.println(doc);
    }

}
