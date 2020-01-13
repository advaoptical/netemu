package com.adva.netemu.datastore;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.FutureCallback;

import com.squareup.inject.assisted.Assisted;
import com.squareup.inject.assisted.AssistedInject;

import dagger.Component;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;


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
}
