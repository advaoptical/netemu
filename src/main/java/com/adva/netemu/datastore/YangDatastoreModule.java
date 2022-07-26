package com.adva.netemu.datastore;

import java.util.Collection;
import java.util.Optional;
// import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.inject.assisted.dagger2.AssistedModule;
import dagger.Module;
import dagger.Provides;

/*
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
*/

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

import com.adva.netemu.YangBinding;
// import com.adva.netemu.YangData;


@AssistedModule
@Module(includes = {AssistedInject_YangDatastoreModule.class})
class YangDatastoreModule {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(YangDatastoreModule.class);

    @Provides @Nonnull
    static YangDatastore.ReadingFutureCallback provideReadingFutureCallback() {
        return new YangDatastore.ReadingFutureCallback() {

            @Override
            public void onSuccess(@Nullable final Optional<NormalizedNode> result) {
                LOG.info("TODO: "); // {}", result);
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                LOG.error("While reading to {} Datastore:", this.storeType, t);
                LOG.error("Failed reading from {} Datastore", this.storeType);
            }
        };
    }

    @Provides @Nonnull
    static YangDatastore.WritingFutureCallback provideWritingFutureCallback() {
        return new YangDatastore.WritingFutureCallback() {

            @Override
            @SuppressWarnings({"UnstableApiUsage"})
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info("TODO: {}", result);
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                LOG.error("While writing to {} Datastore:", this.storeType, t);
                LOG.error("Failed writing to {} Datastore: {}", this.storeType, this.yangPaths);
            }
        };
    }

    @Provides @Nonnull
    static YangDatastore.ModeledWritingTransactor provideModeledWritingTransactor() {
        return new YangDatastore.ModeledWritingTransactor() {

            @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
            public // <Y extends ChildOf<?>, B extends Builder<Y>>
            FluentFuture<? extends CommitInfo> apply(
                    @Nonnull final DataBroker broker,
                    @Nonnull final LogicalDatastoreType storeType,
                    @Nonnull final Collection<? extends YangBinding<?, ?>> bindings) {

                /*
                @SuppressWarnings("unchecked") final var path = (InstanceIdentifier<Y>) this.yangModeledPath;

                @Nullable final YangData<Y> data;
                try {
                    data = ((storeType == LogicalDatastoreType.OPERATIONAL) ? object.provideOperationalData()
                            : object.provideConfigurationData()

                    ).get();

                } catch (final InterruptedException | ExecutionException e) {
                    return FluentFuture.from(Futures.immediateFailedFuture(e.getCause()));
                }

                if (data.isEmpty()) {
                    return FluentFuture.from(Futures.immediateFuture(CommitInfo.empty()));
                }

                LOG.debug("Writing to {} Datastore: {}", this.storeType, path);

                @Nonnull final var txn = broker.newWriteOnlyTransaction();
                txn.merge(this.storeType, path, data.get());
                return txn.commit();
                */

                return FluentFuture.from(Futures.immediateFuture(CommitInfo.empty()));
            }
        };
    }

    @Provides @Nonnull
    static YangDatastore.ModeledWritingFutureCallback provideModeledWritingFutureCallback() {
        return new YangDatastore.ModeledWritingFutureCallback() {

            @Override @SuppressWarnings({"UnstableApiUsage"})
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info("TODO: {}", result);
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                LOG.error("While writing to {} Datastore:", this.storeType, t);
                LOG.error("Failed writing to {} Datastore: {}", this.storeType, this.yangModeledPaths);
            }
        };
    }
}
