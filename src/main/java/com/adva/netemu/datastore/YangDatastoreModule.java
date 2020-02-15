package com.adva.netemu.datastore;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.squareup.inject.assisted.dagger2.AssistedModule;

import dagger.Module;
import dagger.Provides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;

import com.adva.netemu.YangBinding;


@AssistedModule
@Module(includes = {AssistedInject_YangDatastoreModule.class})
class YangDatastoreModule {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(YangDatastoreModule.class);

    @Provides @Nonnull
    static YangDatastore.ReadingFutureCallback provideReadingFutureCallback() {
        return new YangDatastore.ReadingFutureCallback() {

            @Override
            public void onSuccess(@Nullable final Optional<NormalizedNode<?, ?>> result) {
                LOG.info("TODO: {}", result);
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
                LOG.error("Failed writing to {} Datastore: {}", this.storeType, this.yangPath);
            }
        };
    }

    @Provides @Nonnull
    static YangDatastore.ModeledWritingTransactor provideModeledWritingTransactor() {
        return new YangDatastore.ModeledWritingTransactor() {

            @Nonnull @Override
            @SuppressWarnings({"UnstableApiUsage"})
            public <Y extends ChildOf<?>, B extends Builder<Y>>
            FluentFuture<? extends CommitInfo> apply(@Nonnull final DataBroker broker, @Nonnull final YangBinding<Y, B> object) {
                @SuppressWarnings("unchecked") final var path = (InstanceIdentifier<Y>) this.yangModeledPath;

                @Nullable final var data = object.provideOperationalData();
                if (data == null) {
                    return FluentFuture.from(Futures.immediateFuture(CommitInfo.empty()));
                }

                LOG.info("Writing to {} Datastore: {}", this.storeType, path);

                @Nonnull final var txn = broker.newWriteOnlyTransaction();
                txn.put(this.storeType, path, data);
                return txn.commit();
            }
        };
    }

    @Provides @Nonnull
    static YangDatastore.ModeledWritingFutureCallback provideModeledWritingFutureCallback() {
        return new YangDatastore.ModeledWritingFutureCallback() {

            @Override
            @SuppressWarnings({"UnstableApiUsage"})
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info("TODO: {}", result);
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                LOG.error("While writing to {} Datastore:", this.storeType, t);
                LOG.error("Failed writing to {} Datastore: {}", this.storeType, this.yangModeledPath);
            }
        };
    }
}
