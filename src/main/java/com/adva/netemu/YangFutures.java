package com.adva.netemu;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import one.util.streamex.StreamEx;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public abstract class YangFutures {

    private YangFutures() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static <T extends YangBindable> CompletableFuture<T> resolveOperationalDataApplyingTo(@Nonnull final T object) {
        return object.getYangBinding().map(binding -> binding.operationalDataApplying().thenApply(applied -> object))
                .orElse(CompletableFuture.completedFuture(object));
    }

    @Nonnull
    public static <T extends YangListBindable> CompletableFuture<T> resolveOperationalDataApplyingTo(@Nonnull final T object) {
        return object.getYangListBinding().map(binding -> binding.operationalDataApplying().thenApply(applied -> object))
                .orElse(CompletableFuture.completedFuture(object));
    }

    @Nonnull
    public static <T extends YangListBindable, C extends Collection<T>>
    CompletableFuture<C> resolveOperationalDataApplyingTo(@Nonnull final C objects) {
        return CompletableFuture
                .allOf(StreamEx.of(objects).map(YangListBindable::getYangListBinding).flatMap(Optional::stream)
                        .map(YangListBinding::operationalDataApplying).toArray(CompletableFuture.class))

                .thenApply(applied -> objects);
    }
}
