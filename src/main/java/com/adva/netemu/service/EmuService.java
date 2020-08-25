package com.adva.netemu.service;

import javax.annotation.Nonnull;

import com.adva.netemu.YangPool;


public abstract class EmuService implements Runnable {

    public interface Settings<S extends EmuService> {}

    @Nonnull
    private final YangPool yangPool;

    @Nonnull
    public YangPool yangPool() {
        return this.yangPool;
    }

    @Nonnull
    private final Settings<? extends EmuService> settings;

    @Nonnull
    public Settings<? extends EmuService> settings() {
        return this.settings;
    }

    protected EmuService(@Nonnull final YangPool pool, @Nonnull final Settings<? extends EmuService> settings) {
        this.yangPool = pool;
        this.settings = settings;
    }
}
