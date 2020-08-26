package com.adva.netemu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;


public abstract class YangFutures {

    private YangFutures() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static <T extends YangBindable> CompletableFuture<T> awaitOperationalDataApplyingTo(@Nonnull final T object) {
        return object.getYangBinding().map(binding -> binding.awaitOperationalDataApplying().thenApply(applied -> object))
                .orElse(CompletableFuture.completedFuture(object));
    }

    @Nonnull
    public static <T extends YangListBindable> CompletableFuture<T> awaitOperationalDataApplyingTo(@Nonnull final T object) {
        return object.getYangListBinding().map(binding -> binding.awaitOperationalDataApplying().thenApply(applied -> object))
                .orElse(CompletableFuture.completedFuture(object));
    }

    @Nonnull
    public static <T extends YangListBindable, C extends Collection<T>>
    CompletableFuture<C> awaitOperationalDataApplyingTo(@Nonnull final C objects) {
        return CompletableFuture
                .allOf(StreamEx.of(objects).map(YangListBindable::getYangListBinding).flatMap(Optional::stream)
                        .map(YangListBinding::awaitOperationalDataApplying).toArray(CompletableFuture.class))

                .thenApply($ -> objects);
    }

    @Nonnull
    public static <T extends YangListBindable>
    CompletableFuture<List<T>> awaitOperationalDataApplyingTo(@Nonnull final T[] objects) {
        return awaitOperationalDataApplyingTo(List.of(objects));
    }

    @Nonnull
    public static <T extends YangListBindable>
    CompletableFuture<List<T>> awaitOperationalDataApplyingTo(@Nonnull final Stream<T> objects) {
        return awaitOperationalDataApplyingTo(StreamEx.of(objects).toImmutableList());
    }
}
