package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dreamcat.common.util.ReflectUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
public enum SwaggerType {

    object,
    array,
    string,
    integer,
    @JsonProperty("boolean")
    _boolean;

    public static SwaggerType parse(Class<?> clazz) {
        if (clazz == null) return object;
        if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return _boolean;
        } else if (ReflectUtil.isAssignable(Number.class, clazz)) {
            return integer;
        } else if (ReflectUtil.isAssignable(CharSequence.class, clazz) || clazz.isEnum()
                || clazz.equals(LocalDate.class) || clazz.equals(LocalDateTime.class)) {
            return string;
        } else if (ReflectUtil.isCollectionOrArray(clazz)) {
            return array;
        } else {
            return object;
        }
    }
}
