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

    @Nonnull
    default Optional<YangPool> getYangPool() {
        return this.getYangBinding().flatMap(YangBinding::getYangPool);
    }

    @Nonnull
    default YangPool requireYangPool() {
        return this.requireYangBinding().requireYangPool();
    }

    @Nonnull
    default Optional<NetEmu> getNetEmu() {
        return this.getYangPool().flatMap(YangPool::getNetEmu);
    }

    @Nonnull
    default NetEmu requireNetEmu() {
        return this.requireYangPool().requireNetEmu();
    }
}
