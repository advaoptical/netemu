package com.adva.netemu;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
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
        return objects.stream().map(YangModeled::provideOperationalData)
                .filter(Objects::nonNull);
    }
}
