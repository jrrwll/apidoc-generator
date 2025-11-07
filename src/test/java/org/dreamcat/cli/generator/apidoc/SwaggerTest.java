package org.dreamcat.cli.generator.apidoc;

import org.dreamcat.cli.generator.apidoc.renderer.HttpPushConfig;
import org.dreamcat.cli.generator.apidoc.renderer.swagger.SwaggerRenderer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.net.UrlUtil;
import org.dreamcat.common.util.ClassLoaderUtil;
import org.dreamcat.common.util.MapUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

/**
 * <a href="https://editor.swagger.io/">swagger editor</a>
 *
 * @author Jerry Will
 * @version 2022-01-07
 */
class SwaggerTest extends BaseTest {

    @Test
    void testController() throws Exception {
        ApiDocParseConfig config = buildConfigForController();

        SwaggerRenderer renderer = new SwaggerRenderer();
        generate(config, renderer);
    }

    @Test
    void testControllerSwagger2() throws Exception {
        ApiDocParseConfig config = buildConfigForController();

        SwaggerRenderer renderer = new SwaggerRenderer();
        renderer.setSwagger2(true);
        generate(config, renderer);
    }

    @Test
    void testControllerWithHttpConfig() throws Exception {
        ApiDocParseConfig config = buildConfigForController();

        SwaggerRenderer renderer = new SwaggerRenderer();
        renderer.setHttpPush(buildHttpPushConfig());
        generate(config, renderer);
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
                home_dir
                        + "/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.17"
                        + ".2/jackson-annotations-2.17.2.jar",
                home_dir + "/.m2/repository/org/springframework/spring-jcl/5.3.31/spring-jcl-5.3.31.jar"
        );
        ClassLoader classLoader = new URLClassLoader(urls.stream()
                .map(File::new).map(UrlUtil::toURL).toArray(URL[]::new));

        generate(config, renderer, classLoader);
    }

    private HttpPushConfig buildHttpPushConfig() throws IOException {
        FileUtil.loadDotEnvFile();
        String yapi_url = System.getProperty("yapi_url");
        String yapi_project_token = System.getProperty("yapi_project_token");
        if (yapi_url == null || yapi_project_token == null) return null;

        HttpPushConfig httpPush = new HttpPushConfig();
        httpPush.setUrl(yapi_url + "/api/open/import_data");
        httpPush.setJson(MapUtil.of(
                "type", "swagger",
                // normal"(普通模式) , "good"(智能合并), "merge"(完全覆盖)
                "merge", "normal",
                "token", yapi_project_token,
                "json", "$swaggerJsonEscaped"
        ));
        System.out.println("httpPush:\n" + JsonUtil.toJsonWithPretty(httpPush));
        return httpPush;
    }
}
