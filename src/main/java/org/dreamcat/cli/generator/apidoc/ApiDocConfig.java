package org.dreamcat.cli.generator.apidoc;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
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
    private boolean jsonWithComment; //

    private boolean enableSpringWeb; // parse spring annotations or not
    private Http http; // http annotations

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public void afterPropertySet(ClassLoader classLoader) {
        ObjectUtil.requireNotEmpty(basePackages, "basePackages");
        ObjectUtil.requireNotEmpty(srcDirs, "srcDirs");
        ObjectUtil.requireNotEmpty(javaFileDirs, "javaFileDirs");

        if (enableSpringWeb && http == null) {
            http = springHttp(classLoader);
        }
    }

    public boolean ignoreInputParamType(String type) {
        return ObjectUtil.isNotEmpty(ignoreInputParamTypes) &&
                ignoreInputParamTypes.contains(type);
    }

    /**
     * @author Jerry Will
     * @version 2022-01-09
     */
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

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private Http springHttp(ClassLoader classLoader) {
        Http h = new Http();
        h.setPath(ReflectUtil.forName("org.springframework.web.bind.annotation.RequestMapping", true, classLoader));
        h.setPathGetter(ApiDocConfig::requestMappingPath);
        h.setAction(ReflectUtil.forName("org.springframework.web.bind.annotation.RequestMapping", true, classLoader));
        h.setActionGetter(ApiDocConfig::requestMappingMethod);
        h.setRequired(ReflectUtil.forName("org.springframework.web.bind.annotation.RequestParam", true, classLoader));
        h.setRequiredGetter(ApiDocConfig::requestParamRequired);
        h.setPathVar(ReflectUtil.forName("org.springframework.web.bind.annotation.PathVariable", true, classLoader));
        h.setPathVarGetter(ApiDocConfig::pathVariablePathVar);
        return h;
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
