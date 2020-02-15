package com.adva.netemu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface YangListProvider {

    @Nonnull
    Class<?> origin();

    @Nonnull
    Class<? extends DataObject> value();

    @Nonnull
    Class<? extends Identifier<?>> key();
}
