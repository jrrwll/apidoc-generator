package org.dreamcat.cli.generator.apidoc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Data;
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
    private MergeInputParam mergeInputParam; // when to use indented table

    // annotation
    private boolean autoDetect; // auto detect classpath and setup annotation config
    private List<Http> http; // http annotations
    private List<Validation> validation; // auto detect javax-validation
    // other annotations
    private List<FunctionAnno> functionAnno; // annotation for method
    private List<ParamAnno> paramAnno; // annotation for param or field

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public void afterPropertySet(ClassLoader classLoader) {
        AssertUtil.requireNotEmpty(basePackages, "basePackages");
        AssertUtil.requireNotEmpty(srcDirs, "srcDirs");
        AssertUtil.requireNotEmpty(javaFileDirs, "javaFileDirs");

        // auto detect
        if (autoDetect) {
            // springWeb
            if (ObjectUtil.isEmpty(http) && ReflectUtil.forNameOrNull(REQUEST_MAPPING, classLoader) != null) {
                this.http = new ArrayList<>(Collections.singleton(springWeb()));
            }
            if (ObjectUtil.isEmpty(validation) && ReflectUtil.forNameOrNull(NOT_NULL) != null) {
                this.validation = new ArrayList<>(Collections.singleton(javaxValidation()));
            }
            if (ReflectUtil.forNameOrNull(JACKSON_PROPERTY) != null) {

            }
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
        if (it.flatType && allFlat) return true;
        return it.countThreshold > 0 && count >= it.countThreshold;
    }


    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    @Data
    public static class Http {

        private String pathAnno;
        private List<String> pathGetter = Arrays.asList("path", "value"); // string or string[]
        private String actionAnno;
        private List<String> actionGetter; // string or string[] or Enum[]
        private String pathVarAnno;
        private List<String> pathVarGetter = Arrays.asList("name", "value"); // string
        private String requiredAnno;
        private List<String> requiredGetter = Collections.singletonList("required"); // boolean
    }

    @Data
    public static class Validation {

        private String notNullAnno;
        private String notEmptyAnno;
        private String notBlankAnno;
    }

    @Data
    public static class MergeInputParam {

        // regexp to match ${className}.${methodName}
        private Set<String> namePatterns;
        // input params count
        private int countThreshold;
        // all input params have flat type
        private boolean flatType;
        private Predicate<Class<?>> flatTypeTester = ReflectUtil::isFlat;

        public static MergeInputParam namePatterns(Set<String> namePatterns) {
            MergeInputParam mergeInputParam = new MergeInputParam();
            mergeInputParam.setNamePatterns(namePatterns);
            return mergeInputParam;
        }

        public static MergeInputParam countThreshold(int countThreshold) {
            MergeInputParam mergeInputParam = new MergeInputParam();
            mergeInputParam.setCountThreshold(countThreshold);
            return mergeInputParam;
        }

        public static MergeInputParam flatType() {
            MergeInputParam mergeInputParam = new MergeInputParam();
            mergeInputParam.setFlatType(true);
            return mergeInputParam;
        }
    }

    @Data
    public static class FunctionAnno {

        private String anno;
        private List<String> commentGetter = Arrays.asList("comment", "description");
        private List<String> nestedParamGetter = Arrays.asList("params", "parameters");
        private List<String> nestedParamNameGetter = Collections.singletonList("name");
        private List<String> nestedParamCommentGetter = Arrays.asList("comment", "description");
        private List<String> nestedParamRequiredGetter = Collections.singletonList("required");
    }

    @Data
    public static class ParamAnno {

        private String anno;
        private List<String> nameGetter = Arrays.asList("name", "value");
        private List<String> commentGetter = Arrays.asList("comment", "description");
        private List<String> requiredGetter = Collections.singletonList("required"); // boolean
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private static Http springWeb() {
        Http h = new Http();
        h.setPathAnno(REQUEST_MAPPING);
        h.setActionAnno(REQUEST_MAPPING);
        h.setActionGetter(Arrays.asList("method", "action"));
        h.setRequiredAnno("org.springframework.web.bind.annotation.RequestParam");
        h.setPathVarAnno("org.springframework.web.bind.annotation.PathVariable");
        return h;
    }

    private static Validation javaxValidation() {
        Validation v = new Validation();
        v.setNotNullAnno(NOT_NULL);
        v.setNotEmptyAnno("javax.validation.constraints.NotEmpty");
        // javax-validation 1.0 has no NotBlank
        if (ReflectUtil.forNameOrNull(NOT_BLANK) != null) {
            v.setNotBlankAnno(NOT_BLANK);
        }
        return v;
    }

    private static final String NOT_NULL = "javax.validation.constraints.NotNull";
    private static final String NOT_BLANK = "javax.validation.constraints.NotBlank";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final String JACKSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
}
