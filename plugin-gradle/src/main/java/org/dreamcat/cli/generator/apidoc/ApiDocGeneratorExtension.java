package org.dreamcat.cli.generator.apidoc;

import java.util.Arrays;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public abstract class ApiDocGeneratorExtension {

    abstract Property<Boolean> getVerbose();

    abstract Property<String> getOutputPath();

    abstract Property<Boolean> getRewrite();

    abstract ListProperty<String> getClassDirs(); // class dir

    abstract ListProperty<String> getJarDirs(); // jar dir

    abstract ListProperty<String> getBasePackages();

    abstract ListProperty<String> getSrcDirs();

    abstract ListProperty<String> getJavaFileDirs(); // required

    abstract Property<Boolean> getUseRelativeJavaFilePath();

    abstract ListProperty<String> getIgnoreInputParamTypes();

    abstract Property<Boolean> getMergeInputParam();

    abstract Property<Boolean> getAutoDetect();

    abstract ListProperty<String> getValidation();

    public ApiDocGeneratorExtension() {
        getRewrite().convention(false);
        getUseRelativeJavaFilePath().convention(true);
        getIgnoreInputParamTypes().convention(Arrays.asList(
                "org.springframework.web.multipart.MultipartFile",
                "[B"
        ));
        getAutoDetect().convention(true);
    }

    /// nested

    @Nested
    abstract JsonWithComment getJsonWithComment();

    @Nested
    abstract Swagger getSwagger();

    @Nested
    abstract RendererPlugin getRendererPlugin();

    public void jsonWithComment(Action<? super JsonWithComment> action) {
        action.execute(getJsonWithComment());
    }

    public void swagger(Action<? super Swagger> action) {
        action.execute(getSwagger());
    }

    public void rendererPlugin(Action<? super RendererPlugin> action) {
        action.execute(getRendererPlugin());
    }

    abstract NamedDomainObjectContainer<Http> getHttp();

    abstract NamedDomainObjectContainer<FunctionDoc> getFunctionDoc();

    abstract NamedDomainObjectContainer<FieldDoc> getFieldDoc();

    /// data class

    public abstract static class JsonWithComment {

        // template
        abstract Property<String> getTemplate();

        abstract MapProperty<String, String> getIncludeTemplates();

        // jwc
        abstract Property<Boolean> getFieldsNoRequired();

        abstract Property<Boolean> getOutputParamAsIndentedTable();

        abstract Property<String> getNameHeader();

        abstract Property<String> getFunctionHeader();

        abstract Property<String> getInputParamTitle();

        abstract Property<String> getOutputParamTitle();

        abstract Property<Boolean> getPinFunctionComment();

        abstract Property<String> getSeqPrefix();

        abstract Property<Integer> getMaxNestLevel();

        abstract Property<String>  getIndentSpace();

        abstract Property<String> getIndentPrefix();

        abstract Property<String> getIndentName();

        abstract Property<String> getIndentType();

        abstract Property<String> getIndentRequired();

        abstract Property<String> getRequiredTrue();

        abstract Property<String> getRequiredFalse();
    }

    public abstract static class Swagger {

        abstract Property<Boolean> getEnabled();

        abstract Property<String> getDefaultTitle();

        abstract Property<String> getDefaultVersion();

        abstract Property<String> getFieldNameAnnotation();

        abstract ListProperty<String> getFieldNameAnnotationName();

        abstract Property<Boolean> getUseJacksonFieldNameGetter();
    }

    public abstract static class RendererPlugin {

        abstract Property<String> getPath();

        //  support to inject env vars to string value
        abstract MapProperty<String, Object> getConstructArgs();
    }

    public interface Http {

        String getName(); // for NamedDomainObjectContainer

        Property<String> getPath();

        ListProperty<String> getPathMethod();

        Property<String> getAction();

        ListProperty<String> getActionMethod();

        Property<String> getPathVar();

        ListProperty<String> getPathVarMethod();

        Property<String> getRequired();

        ListProperty<String> getRequiredMethod();
    }

    public interface FunctionDoc {

        String getName();

        Property<String> getAnnotationName();

        ListProperty<String> getCommentMethod();

        ListProperty<String> getNestedParamMethod();

        ListProperty<String> getNestedParamNameMethod();

        ListProperty<String> getNestedParamCommentMethod();

        ListProperty<String> getNestedParamRequiredMethod();
    }

    public interface FieldDoc {

        String getName();

        Property<String> getAnnotationName();

        ListProperty<String> getNameMethod();

        ListProperty<String> getCommentMethod();

        ListProperty<String> getRequiredMethod();
    }
}
