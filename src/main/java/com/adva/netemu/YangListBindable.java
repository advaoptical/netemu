package com.adva.netemu;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;


public interface YangListBindable extends YangBindable {

    @Nonnull @Override
    default Optional<YangBinding<?, ?>> getYangBinding() {
        return this.getYangListBinding().map(YangBinding.class::cast);
    }

    @Nonnull
    Optional<YangListBinding<?, ?, ?>> getYangListBinding();

    @Nonnull
    default YangListBinding<?, ?, ?> requireYangListBinding() {
        return this.getYangListBinding().orElseThrow(() -> new NoSuchElementException(
                String.format("%s has no YANG list binding", this)));
    }
}
