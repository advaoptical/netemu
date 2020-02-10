package com.adva.netemu;

import java.util.Optional;

import javax.annotation.Nonnull;


public interface YangListBindable {

    @Nonnull
    Optional<YangListBinding<?, ?, ?>> getYangListBinding();
}
