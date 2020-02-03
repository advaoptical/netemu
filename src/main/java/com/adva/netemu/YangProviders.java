package com.adva.netemu;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;


public final class YangProviders {

    private YangProviders() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    Optional<Y> operationalDataFrom(@Nonnull final T object) {
        return Optional.ofNullable(object.provideOperationalData());
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    Stream<Y> streamOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects.stream());
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    Stream<Y> streamOperationalDataFrom(@Nonnull final Stream<T> objects) {
        return objects.map(YangModeled::provideOperationalData)
                .filter(Objects::nonNull);
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    List<Y> listOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects).collect(Collectors.toList());
    }

    @Nonnull
    public static <
            Y extends ChildOf,
            T extends YangModeled<Y, ? extends Builder<Y>>>

    List<Y> listOperationalDataFrom(@Nonnull final Stream<T> objects) {
        return streamOperationalDataFrom(objects).collect(Collectors.toList());
    }
}
