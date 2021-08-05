package com.adva.netemu.datastore;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

import com.adva.netemu.YangBinding;


@Component(modules = {YangDatastoreModule.class})
public interface YangDatastore {

    abstract class ReadingFutureCallback implements FutureCallback<Optional<NormalizedNode<?, ?>>> {

        @Nullable
        LogicalDatastoreType storeType = null;

        @Nonnull
        ReadingFutureCallback of(@Nonnull final LogicalDatastoreType storeType) {
            this.storeType = storeType;
            return this;
        }
    }

    class Reading {

        @Nonnull
        public final ReadingFutureCallback futureCallback;

        @AssistedInject
        Reading(@Nonnull final ReadingFutureCallback callback, @Assisted @Nonnull final LogicalDatastoreType storeType) {
            this.futureCallback = callback.of(storeType);
        }

        @AssistedInject.Factory
        interface Factory {

            @Nonnull
            Reading of(final LogicalDatastoreType storeType);
        }
    }

    @Nonnull
    YangDatastore$Reading_AssistedFactory injectReading();

    @SuppressWarnings({"UnstableApiUsage"})
    abstract class WritingFutureCallback implements FutureCallback<CommitInfo> {

        @Nullable
        LogicalDatastoreType storeType = null;

        @Nullable
        Collection<? extends YangInstanceIdentifier> yangPaths = null;

        @Nonnull
        WritingFutureCallback of(
                @Nonnull final LogicalDatastoreType storeType,
                @Nonnull final Collection<? extends YangInstanceIdentifier> yangPaths) {

            this.storeType = storeType;
            this.yangPaths = yangPaths;
            return this;
        }
    }

    class Writing {

        @Nonnull
        public final WritingFutureCallback futureCallback;

        @AssistedInject
        Writing(@Nonnull final WritingFutureCallback callback,
                @Assisted @Nonnull final LogicalDatastoreType storeType,
                @Assisted @Nonnull final Collection<? extends YangInstanceIdentifier> yangPaths) {

            this.futureCallback = callback.of(storeType, yangPaths);
        }

        @AssistedInject.Factory
        interface Factory {

            @Nonnull
            Writing of(final LogicalDatastoreType storeType, final Collection<? extends YangInstanceIdentifier> yangPaths);
        }
    }

    @Nonnull
    YangDatastore$Writing_AssistedFactory injectWriting();

    @FunctionalInterface
    interface ModeledWritingFunction {

        @Nonnull
        @SuppressWarnings({"UnstableApiUsage"})
        <Y extends ChildOf<?>, B extends Builder<Y>>
        FluentFuture<? extends CommitInfo> apply(
                @Nonnull final DataBroker broker,
                @Nonnull final LogicalDatastoreType storeType,
                @Nonnull final YangBinding<Y, B> object);
    }

    abstract class ModeledWritingTransactor implements ModeledWritingFunction {

        @Nullable
        LogicalDatastoreType storeType = null;

        @Nullable
        InstanceIdentifier<?> yangModeledPath = null;

        @Nonnull
        ModeledWritingTransactor of(
                @Nonnull final LogicalDatastoreType storeType, @Nonnull final InstanceIdentifier<?> yangModeledPath) {

            this.storeType = storeType;
            this.yangModeledPath = yangModeledPath;
            return this;
        }
    }

    @SuppressWarnings({"UnstableApiUsage"})
    abstract class ModeledWritingFutureCallback implements FutureCallback<CommitInfo> {

        @Nullable
        LogicalDatastoreType storeType = null;

        @Nullable
        InstanceIdentifier<?> yangModeledPath = null;

        @Nonnull
        ModeledWritingFutureCallback of(
                @Nonnull final LogicalDatastoreType storeType, @Nonnull final InstanceIdentifier<?> yangModeledPath) {

            this.storeType = storeType;
            this.yangModeledPath = yangModeledPath;
            return this;
        }
    }

    class ModeledWriting {

        @Nonnull
        public final ModeledWritingTransactor transactor;

        @Nonnull
        public final ModeledWritingFutureCallback futureCallback;

        @AssistedInject
        ModeledWriting(
                @Nonnull final ModeledWritingTransactor transactor,
                @Nonnull final ModeledWritingFutureCallback callback,
                @Assisted @Nonnull final LogicalDatastoreType storeType,
                @Assisted @Nonnull final InstanceIdentifier<?> yangModeledPath) {

            this.transactor = transactor.of(storeType, yangModeledPath);
            this.futureCallback = callback.of(storeType, yangModeledPath);
        }

        @AssistedInject.Factory
        interface Factory {

            @Nonnull
            ModeledWriting of(final LogicalDatastoreType storeType, final InstanceIdentifier<?> yangModeledPath);
        }
    }

    @Nonnull
    YangDatastore$ModeledWriting_AssistedFactory injectModeledWriting();
}
