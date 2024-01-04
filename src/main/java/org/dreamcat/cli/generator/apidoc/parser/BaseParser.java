package org.dreamcat.cli.generator.apidoc.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParserConfig.Validation;
import org.dreamcat.cli.generator.apidoc.javadoc.CommentJavaParser;
import org.dreamcat.common.json.JSONWithComment;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2024-01-04
 */
@Slf4j
@RequiredArgsConstructor
class BaseParser {

    final ApiDocParserConfig config;
    final ClassLoader classLoader;
    final ObjectRandomGenerator randomGenerator;
    final CommentJavaParser commentJavaParser;

    BaseParser(ApiDocParserConfig config, ClassLoader classLoader, ObjectRandomGenerator randomGenerator) {
        AssertUtil.requireNonNull(config, "config");
        AssertUtil.requireNonNull(randomGenerator, "randomGenerator");
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();

        this.config = config;
        this.classLoader = classLoader;
        this.randomGenerator = randomGenerator;
        this.commentJavaParser = new CommentJavaParser(config);
    }

    String toJSONWithComment(ObjectType type) {
        ObjectRandomGenerator randomGenerator = new ObjectRandomGenerator();
        Object bean = randomGenerator.generate(type);
        return JSONWithComment.stringify(bean, commentJavaParser::provideFieldComment);
    }

    Boolean isValidationRequired(AnnotatedElement element) {
        List<Validation> validation = config.getValidation();
        if (ObjectUtil.isEmpty(validation)) return null;
        for (Validation v : validation) {
            if (retrieveAnnotation(element, v.getNotNullAnno()) != null ||
                    retrieveAnnotation(element, v.getNotEmptyAnno()) != null ||
                    retrieveAnnotation(element, v.getNotBlankAnno()) != null) {
                return true;
            }
        }
        return null;
    }

    <T> Object retrieveAndInvokeAnnotation(AnnotatedElement element, List<T> configObjs,
            Function<T, String> anno, Function<T, List<String>> methods) {
        if (ObjectUtil.isEmpty(configObjs)) return null;
        for (T configObj : configObjs) {
            String annoName = anno.apply(configObj);
            if (ObjectUtil.isNotEmpty(annoName)) continue;
            List<String> methodNames = methods.apply(configObj);
            if (ObjectUtil.isNotEmpty(methodNames)) continue;

            Object value = retrieveAndInvokeAnnotation(element, annoName, methodNames);
            if (value != null) return value;
        }
        return null;
    }

    Object retrieveAndInvokeAnnotation(AnnotatedElement element, String anno, List<String> methods) {
        if (ObjectUtil.isEmpty(methods)) return null;
        Annotation annoObj = retrieveAnnotation(element, anno);
        if (annoObj == null) return null;

        for (String method : methods) {
            Object value = ReflectUtil.invoke(annoObj, method);
            if (value == null) continue;
            if (value.getClass().isArray() && Array.getLength(value) == 0) {
                continue;
            }
            return value;
        }
        return null;
    }

    Annotation retrieveAnnotation(AnnotatedElement element, String anno) {
        if (ObjectUtil.isEmpty(anno)) return null;
        try {
            Class<? extends Annotation> annoClass = ReflectUtil.forNameOrThrow(anno, classLoader);
            if (element instanceof Parameter) {
                return ReflectUtil.retrieveAnnotation((Parameter) element, annoClass);
            } else if (element instanceof Method) {
                return ReflectUtil.retrieveAnnotation((Method) element, annoClass);
            } else {
                return ReflectUtil.retrieveAnnotation((Class<?>) element, annoClass);
            }
        } catch (ClassNotFoundException e) {
            if (config.isVerbose()) {
                log.error("failed to find annotation on {}: {}", element, e.getMessage());
            }
            return null;
        }
    }

    List<String> annoValueToStringList(Object anno) {
        if (anno instanceof String) {
            return Collections.singletonList(anno.toString());
        } else {
            return Arrays.stream((Object[]) anno)
                    .map(Object::toString).collect(Collectors.toList());
        }
    }
}
