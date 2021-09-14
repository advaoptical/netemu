package com.adva.netemu;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;


public interface YangBindable {

    @Nonnull
    Optional<YangBinding<?, ?>> getYangBinding();

    @Nonnull
    default YangBinding<?, ?> requireYangBinding() {
        return this.getYangBinding().orElseThrow(() -> new NoSuchElementException(String.format("%s has no YANG binding", this)));
    }
}
