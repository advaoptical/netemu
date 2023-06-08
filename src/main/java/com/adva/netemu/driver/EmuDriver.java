package com.adva.netemu.driver;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FluentFuture;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import org.opendaylight.mdsal.common.api.CommitInfo;

import com.adva.netemu.YangBinding;
import com.adva.netemu.YangPool;


public abstract class EmuDriver {

    public interface Settings<D extends EmuDriver> {}

    public static abstract class Binding<D extends EmuDriver> {

        @Nonnull @SuppressWarnings({"UnstableApiUsage", "unchecked"})
        public Class<D> getDriverClass() {
            return (Class<D>) (new TypeToken<D>(this.getClass()) {}).getRawType();
        }

        @Nonnull
        private final D driver;

        @Nonnull
        public D driver() {
            return this.driver;
        }

        @Nonnull
        private final YangBinding<?, ?> yangBinding;

        protected Binding(@Nonnull final D driver, @Nonnull final YangBinding<?, ?> yangBinding) {
            this.yangBinding = yangBinding;
            this.driver = driver;
        }
    }

    @Nonnull
    private final YangPool yangPool;

    @Nonnull
    public YangPool yangPool() {
        return this.yangPool;
    }

    @Nonnull
    private final Settings<? extends EmuDriver> settings;

    @Nonnull
    public Settings<? extends EmuDriver> settings() {
        return this.settings;
    }

    /*
    @Nonnull
    protected final Executor executor = new ScheduledThreadPoolExecutor(0);
    */

    protected EmuDriver(@Nonnull final YangPool pool, @Nonnull final Settings<? extends EmuDriver> settings) {
        this.yangPool = pool;
        this.settings = settings;
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public abstract FluentFuture<List<CommitInfo>> fetchConfigurationData(@Nonnull final YangInstanceIdentifier iid);

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchConfigurationData() {
        return this.fetchConfigurationData(YangInstanceIdentifier.empty());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public abstract FluentFuture<List<CommitInfo>> fetchOperationalData(@Nonnull final YangInstanceIdentifier iid);

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchOperationalData() {
        return this.fetchOperationalData(YangInstanceIdentifier.empty());
    }
}
