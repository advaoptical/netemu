package com.adtran.at.sdn4j.yang;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.NonNull;

public interface YangElementProxy<VT> extends YangElement {
    @NonNull
    CompletableFuture<Optional<VT>> yang$get();
}
