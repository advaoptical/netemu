package com.adva.netemu;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.Futures;
import one.util.streamex.StreamEx;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;


public final class Yang {

    private Yang() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    YangData<Y> operationalDataFrom(@Nonnull final T object) {
        try {
            return object.provideOperationalData().get();

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    StreamEx<Y> streamOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects.stream());
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    StreamEx<Y> streamOperationalDataFrom(@Nonnull final Stream<T> objects) {
        try {
            return StreamEx.of(Futures.allAsList(StreamEx.of(objects).map(YangBinding::provideOperationalData)).get())
                    .filter(YangData::isPresent).map(YangData::get);

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
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
