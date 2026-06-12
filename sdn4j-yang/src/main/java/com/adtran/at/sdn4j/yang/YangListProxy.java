package com.adtran.at.sdn4j.yang;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import lombok.NonNull;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.Unmodifiable;

public interface YangListProxy<IT> extends YangSubtree, YangElementProxy<List<@NonNull IT>> {
    @NonNull CompletableFuture<@Unmodifiable @NonNull List<? extends @NonNull IT>> yang$list();

    @NonNull CompletableFuture<@NonNull Stream<? extends @NonNull IT>> yang$stream();

    @NonNull CompletableFuture<@NonNull StreamEx<? extends @NonNull IT>> yang$streamEx();
}
