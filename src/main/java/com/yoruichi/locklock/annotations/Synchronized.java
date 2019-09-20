package com.yoruichi.locklock.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Synchronized {
    String name() default "";

    String generateNameMethod() default "";

    String prefixValue() default "";

    String expiredTimeInMilliSeconds() default "-1";

    String waitTimeInMilliSeconds() default "-1";
}
