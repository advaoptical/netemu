package com.adva.netemu;

import java.util.Optional;

import javax.annotation.Nonnull;


public interface YangBindable {

    @Nonnull
    Optional<YangBinding<?, ?>> getYangBinding();
}
