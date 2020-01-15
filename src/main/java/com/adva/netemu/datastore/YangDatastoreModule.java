package com.adva.netemu.datastore;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.squareup.inject.assisted.dagger2.AssistedModule;

import dagger.Module;
import dagger.Provides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import org.opendaylight.mdsal.common.api.CommitInfo;


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
