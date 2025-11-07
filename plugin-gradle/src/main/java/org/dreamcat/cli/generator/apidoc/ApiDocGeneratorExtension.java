package org.dreamcat.cli.generator.apidoc;

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

    public abstract Property<Boolean> getVerbose();

    public abstract Property<String> getOutputDir();

    public abstract ListProperty<String> getBasePackages();

    public abstract ListProperty<String> getJavaFileDirs(); // required

    public abstract ListProperty<String> getIgnoreInputParamTypes();

    public abstract ListProperty<String> getIgnoreParamNames();

    public abstract Property<Boolean> getMergeInputParam();

    public abstract Property<Boolean> getAutoDetect();

    public ApiDocGeneratorExtension() {
        getAutoDetect().convention(true);
    }

    /// nested

    @Nested
    public abstract JsonWithComment getJsonWithComment();

    @Nested
    public abstract Swagger getSwagger();

    @Nested
    public abstract RendererPlugin getRendererPlugin();

    public void jsonWithComment(Action<? super JsonWithComment> action) {
        action.execute(getJsonWithComment());
    }

    public void swagger(Action<? super Swagger> action) {
        action.execute(getSwagger());
    }

    public void rendererPlugin(Action<? super RendererPlugin> action) {
        action.execute(getRendererPlugin());
    }

    public abstract NamedDomainObjectContainer<ServiceDoc> getServiceDoc();

    public abstract NamedDomainObjectContainer<FunctionDoc> getFunctionDoc();

    public abstract NamedDomainObjectContainer<FieldDoc> getFieldDoc();

    public abstract Property<String> getExtraConfigJson();

    /// data class

    public abstract static class JsonWithComment {

        public abstract Property<Boolean> getEnabled();

        public abstract Property<Boolean> getI18n(); // use $LANG

        public abstract Property<String> getLang();

        // template
        public abstract Property<String> getTemplate();

        public abstract MapProperty<String, String> getIncludeTemplates();

        // jwc
        public abstract Property<Boolean> getFieldsNoRequired();

        public abstract Property<Boolean> getOutputParamAsIndentedTable();

        public abstract Property<String> getNameHeader();

        public abstract Property<String> getFunctionHeader();

        public abstract Property<String> getInputParamTitle();

        public abstract Property<String> getOutputParamTitle();

        public abstract Property<Boolean> getPinFunctionComment();

        public abstract Property<String> getSeqPrefix();

        public abstract Property<Integer> getSeqOffset();

        public abstract Property<Integer> getMaxNestLevel();

        public abstract Property<String> getIndentSpace();

        public abstract Property<String> getIndentPrefix();

        public abstract Property<String> getIndentName();

        public abstract Property<String> getIndentType();

        public abstract Property<String> getIndentRequired();

        public abstract Property<String> getRequiredTrue();

        public abstract Property<String> getRequiredFalse();

        public abstract Property<String> getRequiredNull();
    }

    public abstract static class Swagger {

        public abstract Property<Boolean> getEnabled();

        public abstract Property<Boolean> getSwagger2();

        public abstract Property<String> getDefaultTitle();

        public abstract Property<String> getDefaultVersion();

        public abstract Property<Boolean> getFormatAsJson();

        @Nested
        public abstract HttpPush getHttpPush();

        public void httpPush(Action<? super HttpPush> action) {
            action.execute(getHttpPush());
        }
    }

    public abstract static class RendererPlugin {

        public abstract Property<String> getPath();

        //  support to inject env vars to string value
        public abstract MapProperty<String, Object> getInjectedArgs();
    }

    public abstract static class Http {

        public abstract String getName(); // NamedDomainObjectContainer need it

        public abstract Property<String> getPath();

        public abstract ListProperty<String> getPathMethod();

        public abstract Property<String> getAction();

        public abstract ListProperty<String> getActionMethod();

        public abstract Property<String> getPathVar();

        public abstract ListProperty<String> getPathVarMethod();

        public abstract Property<String> getRequired();

        public abstract ListProperty<String> getRequiredMethod();
    }

    public abstract static class ServiceDoc {

        public abstract String getName(); // NamedDomainObjectContainer need it

        public abstract Property<String> getAnnotationName();

        public abstract ListProperty<String> getNameMethod();

        public abstract ListProperty<String> getCommentMethod();
    }

    public abstract static class FunctionDoc {

        public abstract String getName(); // NamedDomainObjectContainer need it

        public abstract Property<String> getAnnotationName();

        public abstract ListProperty<String> getCommentMethod();

        public abstract ListProperty<String> getNestedParamMethod();

        public abstract ListProperty<String> getNestedParamNameMethod();

        public abstract ListProperty<String> getNestedParamCommentMethod();

        public abstract ListProperty<String> getNestedParamRequiredMethod();
    }

    public abstract static class FieldDoc {

        public abstract String getName(); // NamedDomainObjectContainer need it

        public abstract Property<String> getAnnotationName();

        public abstract ListProperty<String> getNameMethod();

        public abstract ListProperty<String> getCommentMethod();

        public abstract ListProperty<String> getRequiredMethod();
    }

    public abstract static class HttpPush {

        public abstract Property<String> getUrl();

        public abstract MapProperty<String, String> getHeaders();

        public abstract MapProperty<String, Object> getJson();

        public abstract Property<String> getText();

        public abstract MapProperty<String, String> getForm();
    }
}
