package com.adva.netemu.driver;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import com.adva.netemu.YangPool;

import java.util.List;


public abstract class EmuDriver {

    public interface Settings<D extends EmuDriver> {}

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

    protected EmuDriver(@Nonnull final YangPool pool, @Nonnull final Settings<? extends EmuDriver> settings) {
        this.yangPool = pool;
        this.settings = settings;
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public abstract FluentFuture<List<CommitInfo>> fetchOperationalData(@Nonnull final YangInstanceIdentifier iid);

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchOperationalData() {
        return this.fetchOperationalData(YangInstanceIdentifier.empty());
    }
}
