package org.dreamcat.cli.generator.apidoc.parser;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig;
import org.dreamcat.cli.generator.apidoc.ApiDocParseConfig.Validation;
import org.dreamcat.common.util.AssertUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.ReflectUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2024-01-04
 */
@Slf4j
class BaseParser {

    final ApiDocParseConfig config;
    final ClassLoader classLoader;

    BaseParser(ApiDocParseConfig config, ClassLoader classLoader) {
        AssertUtil.requireNotNull(config, "config");
        if (classLoader == null) classLoader = Thread.currentThread().getContextClassLoader();

        this.config = config;
        this.classLoader = classLoader;
    }

    Boolean isValidationRequired(AnnotatedElement element) {
        List<Validation> validation = config.getValidation();
        if (ObjectUtil.isEmpty(validation)) return null;
        for (Validation v : validation) {
            if (findAnno(element, v.getNotNull()) != null ||
                    findAnno(element, v.getNotEmpty()) != null ||
                    findAnno(element, v.getNotBlank()) != null) {
                return true;
            }
        }
        return null;
    }

    <T> Object findAndInvokeAnno(AnnotatedElement element, List<T> configObjs,
            Function<T, String> anno, Function<T, List<String>> methods) {
        if (ObjectUtil.isEmpty(configObjs)) return null;
        for (T configObj : configObjs) {
            String annoName = anno.apply(configObj);
            if (ObjectUtil.isEmpty(annoName)) continue;
            List<String> methodNames = methods.apply(configObj);
            if (ObjectUtil.isEmpty(methodNames)) continue;

            Object value = findAndInvokeAnno(element, annoName, methodNames);
            if (value != null) return value;
        }
        return null;
    }

    Object findAndInvokeAnno(AnnotatedElement element, String anno, List<String> methods) {
        if (ObjectUtil.isEmpty(methods)) return null;
        Annotation annoObj = findAnno(element, anno);
        if (annoObj == null) return null;
        return invokeAnno(annoObj, methods);
    }

    Annotation findAnno(AnnotatedElement element, String anno) {
        if (ObjectUtil.isEmpty(anno)) return null;
        try {
            Class<? extends Annotation> annoClass = ReflectUtil.forNameOrThrow(anno, classLoader);
            if (element instanceof Parameter) {
                return ReflectUtil.retrieveAnnotation((Parameter) element, annoClass);
            } else if (element instanceof Method) {
                return ReflectUtil.retrieveAnnotation((Method) element, annoClass);
            } else if (element instanceof Field) {
                return element.getDeclaredAnnotation(annoClass);
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

    Object invokeAnno(Object annoObj, List<String> methods) {
        if (ObjectUtil.isEmpty(methods)) return null;

        for (String method : methods) {
            Object value = FunctionUtil.invokeOrNull(() -> ReflectUtil.invoke(annoObj, method));
            if (value == null) continue;
            if (value.getClass().isArray() && Array.getLength(value) == 0) {
                continue;
            }
            if (value instanceof String && ((String) value).isEmpty()) continue;
            return value;
        }
        return null;
    }

    List<String> annoValueToStrs(Object anno) {
        if (anno instanceof Object[]) {
            return Arrays.stream((Object[]) anno)
                    .map(Object::toString).collect(Collectors.toList());
        } else {
            return Collections.singletonList(anno.toString());
        }
    }
}
