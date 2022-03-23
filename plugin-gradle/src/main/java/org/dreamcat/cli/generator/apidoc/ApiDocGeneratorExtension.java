package org.dreamcat.cli.generator.apidoc;

import java.util.Arrays;
import org.gradle.api.Action;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public abstract class ApiDocGeneratorExtension {

    abstract Property<String> getOutputPath();

    abstract Property<Boolean> getRewrite();

    abstract ListProperty<String> getClassDirs(); // class dir

    abstract ListProperty<String> getJarDirs(); // jar dir

    abstract ListProperty<String> getBasePackages();

    abstract ListProperty<String> getSrcDirs();

    abstract ListProperty<String> getJavaFileDirs(); // required

    abstract Property<Boolean> getUseRelativeJavaFilePath();

    abstract ListProperty<String> getIgnoreInputParamTypes();

    abstract Property<Boolean> getEnableSpringWeb();

    public ApiDocGeneratorExtension() {
        getRewrite().convention(false);
        getUseRelativeJavaFilePath().convention(true);
        getIgnoreInputParamTypes().convention(Arrays.asList(
                "org.springframework.web.multipart.MultipartFile"
        ));
        getEnableSpringWeb().convention(true);
    }

    /// nested

    @Nested
    abstract JsonWithComment getJsonWithComment();

    @Nested
    abstract Swagger getSwagger();

    public void jsonWithComment(Action<? super JsonWithComment> action) {
        action.execute(getJsonWithComment());
    }

    public void swagger(Action<? super Swagger> action) {
        action.execute(getSwagger());
    }

    /// data class

    public abstract static class JsonWithComment {

        abstract Property<Boolean> getEnabled();

        abstract Property<String> getTemplate();

        abstract Property<String> getNameHeader();

        abstract Property<String> getFunctionHeader();

        abstract Property<String> getInputParamNameHeader();

        abstract Property<String> getInputParamTitle();

        abstract Property<String> getOutputParamTitle();

        abstract Property<String> getFunctionSep();

        abstract Property<String> getGroupSep();
    }

    public abstract static class Swagger {

        abstract Property<Boolean> getEnabled();

        abstract Property<String> getDefaultTitle();

        abstract Property<String> getDefaultVersion();

        abstract Property<String> getFieldNameAnnotation();

        abstract ListProperty<String> getFieldNameAnnotationName();

        abstract Property<Boolean> getUseJacksonFieldNameGetter();
    }
}
