package com.adva.netemu.service;

import javax.annotation.Nonnull;

import com.adva.netemu.YangPool;


public abstract class EmuService implements Runnable {

    @Nonnull
    private final YangPool yangPool;

    @Nonnull
    public YangPool yangPool() {
        return this.yangPool;
    }

    protected EmuService(@Nonnull final YangPool pool) {
        this.yangPool = pool;
    }
}
