package com.adva.netemu;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;


public final class Yang {

    private Yang() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    Optional<Y> operationalDataFrom(@Nonnull final T object) {
        return Optional.ofNullable(object.provideOperationalData());
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    StreamEx<Y> streamOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects.stream());
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    StreamEx<Y> streamOperationalDataFrom(@Nonnull final Stream<T> objects) {
        return StreamEx.of(objects).map(YangBinding::provideOperationalData).nonNull();
    }

    /*
    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf>
    List<Y> listOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects).collect(Collectors.toList());
    }
    */

    /*
    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf>
    List<Y> listOperationalDataFrom(@Nonnull final Stream<T> objects) {
        return streamOperationalDataFrom(objects).collect(Collectors.toList());
    }
    */
}