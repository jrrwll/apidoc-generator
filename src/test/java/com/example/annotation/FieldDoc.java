package com.example.annotation;

/**
 * @author Jerry Will
 * @version 2024-02-20
 */
public @interface FieldDoc {

    String name() default "";

    String description() default "";
}
