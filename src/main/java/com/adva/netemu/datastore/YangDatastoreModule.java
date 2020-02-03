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

import com.adva.netemu.YangModeled;


@AssistedModule
@Module(includes = {AssistedInject_YangDatastoreModule.class})
class YangDatastoreModule {

    private static final Logger LOG = LoggerFactory.getLogger(
            YangDatastoreModule.class);

    @Provides
    static
    YangDatastore.ReadingFutureCallback provideReadingFutureCallback() {
        return new YangDatastore.ReadingFutureCallback() {

            @Override
            public void onSuccess(
                    @Nullable final Optional<NormalizedNode<?, ?>> result) {

                LOG.info("TODO: " + result);
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                t.printStackTrace();
                LOG.error("Failed reading from "
                        + this.storeType + " Datastore");
            }
        };
    }

    @Provides
    static
    YangDatastore.WritingFutureCallback provideWritingFutureCallback() {
        return new YangDatastore.WritingFutureCallback() {

            @Override
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info(result.toString());
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                t.printStackTrace();
                LOG.error("Failed writing to "
                        + this.storeType + " Datastore: " + this.yangPath);
            }
        };
    }

    @Provides
    static
    YangDatastore.ModeledWritingTransactor
    provideModeledWritingTransactor() {
        return new YangDatastore.ModeledWritingTransactor() {

            @Override
            public <Y extends ChildOf>
            FluentFuture<? extends CommitInfo> apply(
                    @Nonnull final DataBroker dataBroker,
                    @Nonnull final YangModeled<Y, Builder<Y>> object) {

                final var data = object.provideOperationalData();
                if (data == null) {
                    return FluentFuture.from(Futures.immediateFuture(
                            CommitInfo.empty()));
                }

                LOG.info("Writing to " + this.storeType + " Datastore: "
                        + this.yangModeledPath);

                final var txn = dataBroker.newWriteOnlyTransaction();
                txn.put(this.storeType,
                        (InstanceIdentifier<Y>) this.yangModeledPath,
                        data);

                return txn.commit();
            }
        };
    }

    @Provides
    static
    YangDatastore.ModeledWritingFutureCallback
    provideModeledWritingFutureCallback() {
        return new YangDatastore.ModeledWritingFutureCallback() {

            @Override
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info(result.toString());
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                t.printStackTrace();
                LOG.error("Failed writing to "
                        + this.storeType + " Datastore: "
                        + this.yangModeledPath);
            }
        };
    }
}
