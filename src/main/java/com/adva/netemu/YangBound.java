package com.adva.netemu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;


@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface YangBound {

    @Nonnull
    Class<?> context();

    @Nonnull
    String namespace();

    @Nonnull
    String value();
}
