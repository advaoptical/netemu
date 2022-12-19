package com.adva.netemu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;

// import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;


@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface YangProvider {

    @Nonnull
    Class<?> origin();

    @Nonnull
    Class<? extends DataObject> value();

    @Nonnull
    Class<?/* extends YangBuilder<?>*/> builder();
}
