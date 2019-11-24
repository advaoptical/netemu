package com.adva.netemu;

import java.util.Collection;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;


public class YangData<Y extends ChildOf> {

    private @Nonnull final Y _data;

    @Nonnull
    public Y get() {
        return this._data;
    }

    private YangData(@Nonnull final Y data) {
        this._data = data;
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    YangData<Y> of(@Nonnull final T object) {
        return new YangData<>(object.toYangData());
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    Stream<Y> streamOf(@Nonnull final Collection<T> objects) {
        return objects.stream().map(YangModeled::toYangData);
    }
}
