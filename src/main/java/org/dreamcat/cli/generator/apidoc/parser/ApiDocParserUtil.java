package org.dreamcat.cli.generator.apidoc.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2024-01-04
 */
@Slf4j
class ApiDocParserUtil {

    static Object retrieveAndInvokeAnnotation(AnnotatedElement parameter, String anno, String method) {
        return retrieveAndInvokeAnnotation(parameter, anno, Collections.singletonList(method));
    }

    static Object retrieveAndInvokeAnnotation(AnnotatedElement parameter, String anno, List<String> methods) {
        Annotation annoObj = retrieveAnnotation(parameter, anno);
        if (annoObj == null) return null;
        return invokeAnnotationMethod(annoObj, methods);
    }

    static Annotation retrieveAnnotation(AnnotatedElement element, String anno) {
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

    static Object invokeAnnotationMethod(Annotation anno, String method) {
        return invokeAnnotationMethod(anno, Collections.singletonList(method));
    }

    static Object invokeAnnotationMethod(Annotation anno, List<String> methods) {
        for (String method : methods) {
            Object value = ReflectUtil.invoke(anno, method);
            if (value == null) continue;
            if (value.getClass().isArray() && Array.getLength(value) == 0) {
                continue;
            }
            return value;
        }
        return null;
    }

    static List<String> annoValueToStringList(Object anno) {
        if (anno instanceof String) {
            return Collections.singletonList(anno.toString());
        } else {
            return Arrays.stream((Object[]) anno)
                    .map(Object::toString).collect(Collectors.toList());
        }
    }
}
