package com.adva.netemu;

import java.util.Collection;
import java.util.stream.Stream;

import javax.annotation.Nonnull;


public class Owned {

    public static final class Maker {
        private Maker() {}
    }

    @Nonnull
    public static <T extends YangBinding, O extends YangBinding> T by(
            @Nonnull final O owner, @Nonnull final T objectToOwn) {

        objectToOwn.makeOwned(new Maker(), owner);
        return objectToOwn;
    }

    @Nonnull
    public static <T extends YangBinding, O extends YangBinding>
    Stream<T> by(
            @Nonnull final O owner,
            @Nonnull final Stream<T> objectsToOwn) {

        final var maker = new Maker();
        return objectsToOwn.peek((obj) -> obj.makeOwned(maker, owner));
    }

    @Nonnull
    public static <
            C extends Collection<? extends YangBinding>,
            O extends YangBinding>
    C by(
            @Nonnull final O owner, @Nonnull final C objectsToOwn) {

        final var maker = new Maker();
        objectsToOwn.forEach((obj) -> obj.makeOwned(maker, owner));
        return objectsToOwn;
    }
}
