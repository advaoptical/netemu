package com.adva.netemu.service;

import javax.annotation.Nonnull;

import com.google.common.reflect.TypeToken;

import com.adva.netemu.YangPool;


public abstract class EmuService<T extends EmuService.Settings<?>> implements Runnable {

    public interface Settings<S extends EmuService<?>> {}

    @Nonnull
    private final YangPool yangPool;

    @Nonnull
    public YangPool yangPool() {
        return this.yangPool;
    }

    @Nonnull
    private final T settings;

    @Nonnull @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public Class<T> getSettingsClass() {
        return (Class<T>) (new TypeToken<T>(this.getClass()) {}).getRawType();
    }

    @Nonnull
    public T settings() {
        return this.settings;
    }

    protected EmuService(@Nonnull final YangPool pool, @Nonnull final T settings) {
        this.yangPool = pool;
        this.settings = settings;
    }
}
