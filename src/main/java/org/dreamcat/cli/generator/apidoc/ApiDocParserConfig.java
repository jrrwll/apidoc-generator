package org.dreamcat.cli.generator.apidoc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dreamcat.common.function.VoidConsumer;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiDocParserConfig {

    // parser
    private boolean verbose;
    private List<String> basePackages = Collections.singletonList(""); // java files dirs
    private List<String> srcDirs; // source dir
    private List<String> javaFileDirs; // service class dir
    private boolean useRelativeJavaFilePath; // cross join srcDirs & javaFileDirs or not
    private Set<String> ignoreInputParamTypes; // ignore input params

    private boolean enableAutoDetect; // auto detect classpath
    private boolean enableSpringWeb; // parse spring annotations or not
    private Http http; // http annotations
    private Validation validation; // auto detect javax-validation
    private MergeInputParam mergeInputParam; // when to use indented table

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public void afterPropertySet(ClassLoader classLoader) {
        AssertUtil.requireNotEmpty(basePackages, "basePackages");
        AssertUtil.requireNotEmpty(srcDirs, "srcDirs");
        AssertUtil.requireNotEmpty(javaFileDirs, "javaFileDirs");

        // auto detect
        if (enableAutoDetect) {
            tryToRun(() -> {
                if (!enableSpringWeb) {
                    ReflectUtil.forName("org.springframework.web.bind.annotation.RequestMapping", classLoader);
                    this.enableSpringWeb = true;
                }
            });
        }

        // set default
        if (enableSpringWeb && http == null) {
            this.http = springWeb();
        }
        if (validation == null) {
            tryToRun(() -> this.validation = validation());
        }
    }

    public boolean ignoreInputParamType(String type) {
        return ObjectUtil.isNotEmpty(ignoreInputParamTypes) &&
                ignoreInputParamTypes.contains(type);
    }

    public boolean needMergeInputParam(
            Class<?> clazz, Method method) {
        MergeInputParam it = mergeInputParam;
        if (it == null) return false;
        if (ObjectUtil.isNotEmpty(it.namePatterns)) {
            String id = clazz.getName() + "." + method.getName();
            for (String pattern : it.namePatterns) {
                if (id.matches(pattern)) return true;
            }
        }

        Parameter[] parameters = method.getParameters();
        int count = 0;
        boolean allFlat = true;
        for (Parameter parameter : parameters) {
            Class<?> parameterType = parameter.getType();
            if (!ignoreInputParamType(parameterType.getName())) {
                count++;
                allFlat = allFlat && it.flatTypeTester.test(parameterType);
            }
        }
        if (it.byFlatType && allFlat) return true;
        return it.countThreshold > 0 && count >= it.countThreshold;
    }

    @Data
    public static class Http {

        private String pathAnno;
        @Builder.Default
        private List<String> pathGetter = Arrays.asList("path", "value"); // string or string[]
        private String actionAnno;
        private List<String> actionGetter; // string or string[] or Enum[]
        private String pathVarAnno;
        private List<String> pathVarGetter = Arrays.asList("name", "value"); // string
        private String requiredAnno;
        @Builder.Default
        private List<String> requiredGetter = Collections.singletonList("required"); // boolean
    }

    @Data
    public static class Validation {

        private String notNullAnno;
        private String notEmptyAnno;
        private String notBlankAnno;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeInputParam {

        // regexp to match ${className}.${methodName}
        private Set<String> namePatterns;
        // input params count
        private int countThreshold;
        // all input params have flat type
        private boolean byFlatType;
        @Builder.Default
        private Predicate<Class<?>> flatTypeTester = ReflectUtil::isFlat;

        public static MergeInputParam namePatterns(Set<String> namePatterns) {
            return builder().namePatterns(namePatterns).build();
        }

        public static MergeInputParam countThreshold(int countThreshold) {
            return builder().countThreshold(countThreshold).build();
        }

        public static MergeInputParam byFlatType() {
            return builder().byFlatType(true).build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionDoc {

        private String docAnno;
        @Builder.Default
        private List<String> docCommentGetter = Arrays.asList("comment", "description");
        @Builder.Default
        private List<String> docNestedParamGetter = Arrays.asList("params", "parameters");
        @Builder.Default
        private List<String> docNestedParamNameGetter = Collections.singletonList("name");
        @Builder.Default
        private List<String> docNestedParamCommentGetter = Arrays.asList("comment", "description");
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private void tryToRun(VoidConsumer... cbs) {
        for (VoidConsumer cb : cbs) {
            try {
                cb.accept();
            } catch (Exception ignore) {
            }
        }
    }

    private Http springWeb() {
        Http h = new Http();
        h.setPathAnno("org.springframework.web.bind.annotation.RequestMapping");
        h.setActionAnno("org.springframework.web.bind.annotation.RequestMapping");
        h.setActionGetter(Collections.singletonList("method"));
        h.setRequiredAnno("org.springframework.web.bind.annotation.RequestParam");
        h.setPathVarAnno("org.springframework.web.bind.annotation.PathVariable");
        return h;
    }

    private Validation validation() {
        Validation v = new Validation();
        v.setNotNullAnno("javax.validation.constraints.NotNull");
        v.setNotEmptyAnno("javax.validation.constraints.NotEmpty");
        // javax-validation 1.0 has no NotBlank
        v.setNotBlankAnno("javax.validation.constraints.NotBlank");
        return v;
    }

    private static List<String> requestMappingPath(Annotation annotation) {
        String[] paths = (String[]) ReflectUtil.invoke(annotation, "path");
        if (ObjectUtil.isEmpty(paths)) {
            paths = (String[]) ReflectUtil.invoke(annotation, "value");
        }
        return ObjectUtil.isEmpty(paths) ? null : Arrays.asList(paths);
    }

    private static List<String> requestMappingMethod(Annotation annotation) {
        // org.springframework.web.bind.annotation.RequestMethod
        Enum<?>[] methods = (Enum<?>[]) ReflectUtil.invoke(annotation, "method");
        if (ObjectUtil.isEmpty(methods)) return null;
        return Arrays.stream(methods).map(Enum::name)
                .collect(Collectors.toList());
    }

    private static Boolean requestParamRequired(Annotation annotation) {
        return (Boolean) ReflectUtil.invoke(annotation, "required");
    }

    private static String pathVariablePathVar(Annotation annotation) {
        String name = (String) ReflectUtil.invoke(annotation, "name");
        if (name.isEmpty()) {
            name = (String) ReflectUtil.invoke(annotation, "value");
        }
        if (name.isEmpty()) return null;
        return name;
    }
}
