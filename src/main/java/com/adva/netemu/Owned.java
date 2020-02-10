package com.adva.netemu;

import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;

import java.util.Collection;
import java.util.stream.Stream;

import javax.annotation.Nonnull;


public class Owned {

    public static final class Maker {
        private Maker() {}
    }

    @Nonnull
    public static <T extends YangBindable, O extends YangBindable> T by(
            @Nonnull final O owner, @Nonnull final T objectToOwn) {

        objectToOwn.getYangBinding().ifPresent(binding -> binding
                .makeOwned(new Maker(), owner.getYangBinding().orElse(null)));

        return objectToOwn;
    }

    @Nonnull
    public static <T extends YangListBindable, O extends YangBindable> T by(
            @Nonnull final O owner, @Nonnull final T objectToOwn) {

        objectToOwn.getYangListBinding().ifPresent(binding -> binding
                .makeOwned(new Maker(), owner.getYangBinding().orElse(null)));

        return objectToOwn;
    }

    @Nonnull
    public static <T extends YangListBindable, O extends YangBindable>
    Stream<T> by(
            @Nonnull final O owner, @Nonnull final Stream<T> objectsToOwn) {

        final var maker = new Maker();
        return objectsToOwn.peek(object -> object.getYangListBinding()
                .ifPresent(binding -> binding.makeOwned(maker, owner
                        .getYangBinding().orElse(null))));
    }

    @Nonnull
    public static <
            C extends Collection<? extends YangListBindable>,
            O extends YangBindable>

    C by(@Nonnull final O owner, @Nonnull final C objectsToOwn) {
        final var maker = new Maker();
        objectsToOwn.forEach(object -> object.getYangListBinding()
                .ifPresent(binding -> binding.makeOwned(maker, owner
                        .getYangBinding().orElse(null))));

        return objectsToOwn;
    }
}
