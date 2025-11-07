package org.dreamcat.cli.generator.apidoc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.reflect.ObjectField;
import org.dreamcat.common.reflect.ObjectParameter;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Set<String> ignoreParamNames; // ignore input/output param names, e.g. "a.b.Box#field1,field2"
    private Set<String> ignoreFunctionNames; // ignore method names, e.g. "a.b.Box#method1,method2"

    // annotation
    private boolean autoDetect; // auto detect classpath and setup annotation config
    private List<Http> http; // http annotations
    private List<Validation> validation; // auto detect javax-validation
    // other annotations
    private List<ServiceDoc> serviceDoc; // annotation for class
    private List<FunctionDoc> functionDoc; // annotation for method
    private List<FieldDoc> fieldDoc; // annotation for param or field

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private transient List<String> existingSrcDirs;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private transient Map<String, Set<String>> ignoreParamNameMap;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonIgnore
    private transient Map<String, Set<String>> ignoreFunctionNameMap;

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public void afterPropertySet(ClassLoader classLoader) {
        AssertUtil.requireNotEmpty(basePackages, "basePackages");
        AssertUtil.requireNotEmpty(srcDirs, "srcDirs");
        AssertUtil.requireNotEmpty(javaFileDirs, "javaFileDirs");

        // auto detect
        if (!autoDetect) return;

        if(ObjectUtil.isEmpty(ignoreInputParamTypes)) {
            ignoreInputParamTypes = new HashSet<>(Arrays.asList(
                    "[B",
                    "javax.servlet.http.HttpServletRequest",
                    "javax.servlet.http.HttpServletResponse",
                    "javax.servlet.http.Cookie",
                    "org.springframework.web.multipart.MultipartFile"
            ));
        }

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

    public boolean ignoreInputParamType(ObjectParameter parameter) {
        return ignoreInputParamType(parameter.getType().getType().getName());
    }

    private boolean ignoreInputParamType(String type) {
        return ObjectUtil.isNotEmpty(ignoreInputParamTypes) &&
                ignoreInputParamTypes.contains(type);
    }

    public boolean ignoreParamName(ObjectField field) {
        return ignoreParamName(field.getField().getName(),
                field.getDeclaringType().getType().getName());
    }

    public boolean ignoreParamName(ObjectParameter parameter) {
        return ignoreParamName(parameter.getParameter().getName(),
                parameter.getDeclaringMethod().getDeclaringType().getType().getName());
    }

    private boolean ignoreParamName(String name, String type) {
        if (ignoreParamNameMap == null) {
            ignoreParamNameMap = buildIgnoreMap(ignoreParamNames, "ignoreParamNames");
        }
        return ignoreParamNameMap.getOrDefault(type, Collections.emptySet()).contains(name);
    }

    public boolean ignoreFunctionName(String name, String type) {
        if (ignoreFunctionNameMap == null) {
            ignoreFunctionNameMap = buildIgnoreMap(ignoreFunctionNames, "ignoreFunctionNames");
        }
        return ignoreFunctionNameMap.getOrDefault(type, Collections.emptySet()).contains(name);
    }

    private static Map<String, Set<String>> buildIgnoreMap(Set<String> names, String configField) {
        if (ObjectUtil.isEmpty(names)) {
            return Collections.emptyMap();
        } else {
            return names.stream()
                    .map(n -> n.split("#"))
                    .peek(split -> AssertUtil.require(split.length == 2,
                            "invalid " + configField + ": " + Arrays.toString(split)))
                    .collect(Collectors.toMap(split -> split[0],
                            split -> new HashSet<>(Arrays.asList(split[1].split(",")))));
        }
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
                allFlat = allFlat && it.flatTypeTester.test(parameterType, parameter.getParameterizedType());
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
        private BiPredicate<Class<?>, Type> flatTypeTester = ApiDocParseConfig::isFlatType;

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

    private static boolean isFlatType(Class<?> clazz, Type type) {
        if (ReflectUtil.isFlat(clazz)) return true;
        if (ReflectUtil.isAssignable(Collection.class, clazz)) {
            if (type instanceof ParameterizedType) {
                Class<?> typeArg = ReflectUtil.getTypeArgument((ParameterizedType)type);
                return ReflectUtil.isFlat(typeArg);
            }
        } else if (clazz.isArray()) {
            return ReflectUtil.isFlat(clazz.getComponentType());
        }
        return false;
    }

    @Data
    @JsonInclude(Include.NON_EMPTY)
    public static class Http {

        private List<PathHttp> paths;
        private List<ActionHttp> actions;
        private List<PathVarHttp> pathVars;
        private List<RequiredParamHttp> requiredParams;
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(Include.NON_EMPTY)
    public static class PathHttp {

        private String path;
        private List<String> pathMethod = Arrays.asList("path", "value"); // string or string[]
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(Include.NON_EMPTY)
    public static class ActionHttp implements Supplier<String> {

        private String action;
        private List<String> actionMethod = Arrays.asList("method", "action"); // string or string[] or Enum[]
        private String actionLiteral;

        @Override
        public String get() {
            return actionLiteral;
        }
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(Include.NON_EMPTY)
    public static class PathVarHttp {

        private String pathVar;
        private List<String> pathVarMethod = Arrays.asList("name", "value"); // string
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(Include.NON_EMPTY)
    public static class RequiredParamHttp {

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
    public static class ServiceDoc {

        private String name;
        private List<String> nameMethod = Arrays.asList("name", "value");
        private List<String> commentMethod = COMMENT_METHODS;
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
        // paths
        h.setPaths(Stream.of(REQUEST_MAPPING, GET_MAPPING, POST_MAPPING, PUT_MAPPING, DELETE_MAPPING)
                .map(it -> new PathHttp().setPath(it)).collect(Collectors.toList()));

        // actions
        h.setActions(new ArrayList<>());
        h.getActions().add(new ActionHttp().setAction(REQUEST_MAPPING));

        Function<String, String> actionLiteral = it -> {
            String name = it.substring(it.lastIndexOf( ".") + 1);
            return name.substring(0, name.length() - 7).toUpperCase();
        };
        h.getActions().addAll(Stream.of(GET_MAPPING, POST_MAPPING, PUT_MAPPING, DELETE_MAPPING)
                .map(it -> new ActionHttp().setAction(it)
                        .setActionLiteral(actionLiteral.apply(it))).collect(Collectors.toList()));

        // pathVars
        h.setPathVars(Collections.singletonList(new PathVarHttp()
                .setPathVar("org.springframework.web.bind.annotation.PathVariable")));

        // requiredParams
        h.setRequiredParams(Collections.singletonList(new RequiredParamHttp()
                .setRequired("org.springframework.web.bind.annotation.RequestParam")));
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
        f.setNameMethod(Collections.singletonList("value"));
        return f;
    }

    private static final String NOT_NULL = "javax.validation.constraints.NotNull";
    private static final String NOT_BLANK = "javax.validation.constraints.NotBlank";
    private static final String REQUEST_MAPPING =
            "org.springframework.web.bind.annotation.RequestMapping";
    private static final String GET_MAPPING =
            "org.springframework.web.bind.annotation.GetMapping";
    private static final String POST_MAPPING =
            "org.springframework.web.bind.annotation.PostMapping";
    private static final String PUT_MAPPING =
            "org.springframework.web.bind.annotation.PutMapping";
    private static final String DELETE_MAPPING =
            "org.springframework.web.bind.annotation.DeleteMapping";

    private static final String JACKSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty";
    private static final List<String> COMMENT_METHODS = Arrays.asList(
            "comment", "description", "desc", "displayName");
}
