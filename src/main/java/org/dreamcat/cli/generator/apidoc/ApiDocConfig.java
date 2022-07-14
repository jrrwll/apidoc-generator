package org.dreamcat.cli.generator.apidoc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dreamcat.common.function.VoidConsumer;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiDocConfig {

    // parser
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

    // output
    private boolean useJsonWithComment;
    private boolean useIndentedTable;

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public void afterPropertySet(ClassLoader classLoader) {
        ObjectUtil.requireNotEmpty(basePackages, "basePackages");
        ObjectUtil.requireNotEmpty(srcDirs, "srcDirs");
        ObjectUtil.requireNotEmpty(javaFileDirs, "javaFileDirs");

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
            tryToRun(() -> this.http = springWeb(classLoader));
        }
        if (validation == null) {
            tryToRun(() -> this.validation = validation(classLoader));
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

        private Class<? extends Annotation> path;
        private Function<Annotation, List<String>> pathGetter;

        private Class<? extends Annotation> action;
        private Function<Annotation, List<String>> actionGetter;

        private Class<? extends Annotation> pathVar;
        private Function<Annotation, String> pathVarGetter;

        private Class<? extends Annotation> required;
        private Function<Annotation, Boolean> requiredGetter;
    }

    @Data
    public static class Validation {

        private Class<? extends Annotation> notNull;
        private Class<? extends Annotation> notEmpty;
        private Class<? extends Annotation> notBlank;
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

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private void tryToRun(VoidConsumer... cbs) {
        for (VoidConsumer cb : cbs) {
            try {
                cb.accept();
            } catch (Exception ignore) {
            }
        }
    }

    private Http springWeb(ClassLoader classLoader) {
        Http h = new Http();
        h.setPath(ReflectUtil.forName("org.springframework.web.bind.annotation.RequestMapping", classLoader));
        h.setPathGetter(ApiDocConfig::requestMappingPath);
        h.setAction(ReflectUtil.forName("org.springframework.web.bind.annotation.RequestMapping", classLoader));
        h.setActionGetter(ApiDocConfig::requestMappingMethod);
        h.setRequired(ReflectUtil.forName("org.springframework.web.bind.annotation.RequestParam", classLoader));
        h.setRequiredGetter(ApiDocConfig::requestParamRequired);
        h.setPathVar(ReflectUtil.forName("org.springframework.web.bind.annotation.PathVariable", classLoader));
        h.setPathVarGetter(ApiDocConfig::pathVariablePathVar);
        return h;
    }

    private Validation validation(ClassLoader classLoader) {
        Validation v = new Validation();
        v.setNotNull(ReflectUtil.forName("javax.validation.constraints.NotNull", classLoader));
        v.setNotEmpty(ReflectUtil.forName("javax.validation.constraints.NotEmpty", classLoader));
        try {
            // javax-validation 1.0 has no NotBlank
            v.setNotBlank(ReflectUtil.forName("javax.validation.constraints.NotBlank", classLoader));
        } catch (Exception ignore) {
        }
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
