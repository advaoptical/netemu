package com.adva.netemu.datastore;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;

import com.squareup.inject.assisted.Assisted;
import com.squareup.inject.assisted.AssistedInject;

import dagger.Component;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

import com.adva.netemu.YangModeled;


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
                    final LogicalDatastoreType storeType,
                    final YangInstanceIdentifier yangPath);
        }
    }

    YangDatastore$Writing_AssistedFactory injectWriting();

    @FunctionalInterface
    interface ModeledWritingFunction {

        @Nonnull
        <Y extends ChildOf> FluentFuture<? extends CommitInfo> apply(
                @Nonnull final DataBroker dataBroker,
                @Nonnull final YangModeled<Y, Builder<Y>> object);
    }

    abstract class ModeledWritingTransactor
            implements ModeledWritingFunction {

        LogicalDatastoreType storeType;
        InstanceIdentifier<?> yangModeledPath;

        ModeledWritingTransactor of(
                @Nonnull final LogicalDatastoreType storeType,
                @Nonnull final InstanceIdentifier<?> yangModeledPath) {

            this.storeType = storeType;
            this.yangModeledPath = yangModeledPath;
            return this;
        }
    }

    abstract class ModeledWritingFutureCallback
            implements FutureCallback<CommitInfo> {

        LogicalDatastoreType storeType;
        InstanceIdentifier<?> yangModeledPath;

        ModeledWritingFutureCallback of(
                @Nonnull final LogicalDatastoreType storeType,
                @Nonnull final InstanceIdentifier<?> yangModeledPath) {

            this.storeType = storeType;
            this.yangModeledPath = yangModeledPath;
            return this;
        }
    }

    class ModeledWriting {

        public final ModeledWritingTransactor transactor;

        public final ModeledWritingFutureCallback futureCallback;

        @AssistedInject
        ModeledWriting(
                @Nonnull final ModeledWritingTransactor transactor,
                @Nonnull final ModeledWritingFutureCallback callback,
                @Assisted @Nonnull final LogicalDatastoreType storeType,
                @Assisted @Nonnull
                final InstanceIdentifier<?> yangModeledPath) {

            this.transactor = transactor.of(storeType, yangModeledPath);
            this.futureCallback = callback.of(storeType, yangModeledPath);
        }

        @AssistedInject.Factory
        interface Factory {
            ModeledWriting of(
                    final LogicalDatastoreType storeType,
                    final InstanceIdentifier<?> yangModeledPath);
        }
    }

    YangDatastore$ModeledWriting_AssistedFactory injectModeledWriting();
}