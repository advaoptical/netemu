package com.adva.netemu;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;


public interface YangListBindable {

    @Nonnull
    Optional<YangListBinding<?, ?, ?>> getYangListBinding();

    @Nonnull
    default YangListBinding<?, ?, ?> requireYangListBinding() {
        return this.getYangListBinding().orElseThrow(() -> new NoSuchElementException(
                String.format("%s has no YANG list binding", this)));
    }
}
