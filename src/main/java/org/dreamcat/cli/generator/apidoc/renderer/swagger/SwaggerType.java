package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dreamcat.common.util.ReflectUtil;

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
        if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return _boolean;
        } else if (ReflectUtil.isSubOrEq(clazz, Number.class)) {
            return integer;
        } else if (ReflectUtil.isSubOrEq(clazz, CharSequence.class) || clazz.isEnum()) {
            return string;
        } else if (ReflectUtil.isCollectionOrArray(clazz)) {
            return array;
        } else {
            return object;
        }
    }
}
