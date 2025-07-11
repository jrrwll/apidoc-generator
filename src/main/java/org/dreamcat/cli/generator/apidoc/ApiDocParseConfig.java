package org.dreamcat.cli.generator.apidoc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
@JsonInclude(Include.NON_EMPTY)
public class ApiDocParseConfig {

    // parser
    private boolean verbose;
    private List<String> basePackages = Collections.singletonList(""); // java files dirs
    private List<String> srcDirs; // source dir
    // service class dir/file, /a/b/c for absolute path and a/b/c for relative path
    private List<String> javaFileDirs;
    private Set<String> ignoreInputParamTypes; // ignore input params
    private MergeInputParam mergeInputParam; // when to use indented table

    // annotation
    private boolean autoDetect; // auto detect classpath and setup annotation config
    private List<Http> http; // http annotations
    private List<Validation> validation; // auto detect javax-validation
    // other annotations
    private List<FunctionDoc> functionDoc; // annotation for method
    private List<FieldDoc> fieldDoc; // annotation for param or field

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private transient List<String> existingSrcDirs;

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
            if (ObjectUtil.isEmpty(validation) && ReflectUtil.forNameOrNull(NOT_NULL, classLoader) != null) {
                this.validation = new ArrayList<>(Collections.singleton(javaxValidation()));
            }
            if (ObjectUtil.isEmpty(fieldDoc) && ReflectUtil.forNameOrNull(JACKSON_PROPERTY, classLoader) != null) {
                this.fieldDoc = new ArrayList<>(Collections.singleton(jacksonFieldDoc()));
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

    @JsonIgnore
    public List<String> getExistingSrcDirs() {
        if (existingSrcDirs != null) return existingSrcDirs;
        existingSrcDirs = srcDirs.stream()
                .filter(srcDir -> new File(srcDir).exists())
                .collect(Collectors.toList());
        if (existingSrcDirs.isEmpty()) {
            String msg = "no any existing source dirs";
            if (verbose) msg += ": " + srcDirs;
            throw new IllegalArgumentException(msg);
        }
        return existingSrcDirs;
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    @Data
    @JsonInclude(Include.NON_EMPTY)
    public static class MergeInputParam {

        // regexp to match ${className}.${methodName}
        private Set<String> namePatterns;
        // input params count
        private int countThreshold;
        // all input params have flat type
        private boolean flatType;

        @JsonIgnore
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
    @JsonInclude(Include.NON_EMPTY)
    public static class Http {

        private String path;
        private List<String> pathMethod = Arrays.asList("path", "value"); // string or string[]
        private String action;
        private List<String> actionMethod; // string or string[] or Enum[]
        private String pathVar;
        private List<String> pathVarMethod = Arrays.asList("name", "value"); // string
        private String required;
        private List<String> requiredMethod = Collections.singletonList("required"); // boolean
    }

    @Data
    @JsonInclude(Include.NON_EMPTY)
    public static class Validation {

        private String notNull;
        private String notEmpty;
        private String notBlank;
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(Include.NON_EMPTY)
    public static class FunctionDoc {

        private String name;
        private List<String> commentMethod = COMMENT_METHODS;
        private List<String> nestedParamMethod = Arrays.asList("params", "parameters");
        private List<String> nestedParamNameMethod = Collections.singletonList("name");
        private List<String> nestedParamCommentMethod = COMMENT_METHODS;
        private List<String> nestedParamRequiredMethod = Collections.singletonList("required");
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(Include.NON_EMPTY)
    public static class FieldDoc {

        private String name;
        private List<String> nameMethod = Arrays.asList("name", "value");
        private List<String> commentMethod = COMMENT_METHODS;
        private List<String> requiredMethod = Collections.singletonList("required"); // boolean
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private static Http springWeb() {
        Http h = new Http();
        h.setPath(REQUEST_MAPPING);
        h.setAction(REQUEST_MAPPING);
        h.setActionMethod(Arrays.asList("method", "action"));
        h.setRequired("org.springframework.web.bind.annotation.RequestParam");
        h.setPathVar("org.springframework.web.bind.annotation.PathVariable");
        return h;
    }

    private static Validation javaxValidation() {
        Validation v = new Validation();
        v.setNotNull(NOT_NULL);
        v.setNotEmpty("javax.validation.constraints.NotEmpty");
        // javax-validation 1.0 has no NotBlank
        if (ReflectUtil.forNameOrNull(NOT_BLANK) != null) {
            v.setNotBlank(NOT_BLANK);
        }
        return v;
    }

    private static FieldDoc jacksonFieldDoc() {
        FieldDoc f = new FieldDoc();
        f.setName(JACKSON_PROPERTY);
        return f;
    }

    private static final String NOT_NULL = "javax.validation.constraints.NotNull";
    private static final String NOT_BLANK = "javax.validation.constraints.NotBlank";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final String JACKSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
    private static final List<String> COMMENT_METHODS = Arrays.asList(
            "comment", "description", "desc", "displayName");
}
