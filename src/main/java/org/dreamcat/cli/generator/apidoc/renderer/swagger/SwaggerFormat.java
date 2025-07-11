package org.dreamcat.cli.generator.apidoc.renderer.swagger;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author Jerry Will
 * @version 2022-01-04
 */
public enum SwaggerFormat {
    int32,
    int64,
    @JsonProperty("date-time")
    date_time;

    public static SwaggerFormat parse(Class<?> clazz) {
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return int32;
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return int64;
        } else if (clazz.equals(Date.class) || clazz.equals(LocalDateTime.class)) {
            return date_time;
        } else {
            return null;
        }
    }
}
