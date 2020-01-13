package com.adva.netemu.datastore;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.FutureCallback;

import com.squareup.inject.assisted.Assisted;
import com.squareup.inject.assisted.AssistedInject;

import dagger.Component;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;


@Component(modules = {YangDatastoreModule.class})
public interface YangDatastore {

    abstract class ReadingFutureCallback
            implements FutureCallback<Optional<NormalizedNode<?, ?>>> {

        LogicalDatastoreType storeType;

        ReadingFutureCallback of(
                @Nonnull final LogicalDatastoreType storeType) {

            this.storeType = storeType;
            return this;
        }
    }

    class Reading {

        public final ReadingFutureCallback futureCallback;

        @AssistedInject
        Reading(@Nonnull final ReadingFutureCallback callback,
                @Assisted @Nonnull final LogicalDatastoreType storeType) {

            this.futureCallback = callback.of(storeType);
        }

        @AssistedInject.Factory
        interface Factory {
            Reading of(final LogicalDatastoreType storeType);
        }
    }

    YangDatastore$Reading_AssistedFactory injectReading();

    abstract class WritingFutureCallback
            implements FutureCallback<CommitInfo> {

        LogicalDatastoreType storeType;
        YangInstanceIdentifier yangPath;

        WritingFutureCallback of(
                @Nonnull final LogicalDatastoreType storeType,
                @Nonnull final YangInstanceIdentifier yangPath) {

            this.storeType = storeType;
            this.yangPath = yangPath;
            return this;
        }
    }

    class Writing {

        public final WritingFutureCallback futureCallback;

        @AssistedInject
        Writing(@Nonnull final WritingFutureCallback callback,
                @Assisted @Nonnull final LogicalDatastoreType storeType,
                @Assisted @Nonnull final YangInstanceIdentifier yangPath) {

            this.futureCallback = callback.of(storeType, yangPath);
        }

        @AssistedInject.Factory
        interface Factory {
            Writing of(
                    @Nonnull final LogicalDatastoreType storeType,
                    @Nonnull final YangInstanceIdentifier yangPath);
        }
    }

    YangDatastore$Writing_AssistedFactory injectWriting();
}
