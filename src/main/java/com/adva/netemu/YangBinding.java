package com.adva.netemu;

import java.lang.reflect.InvocationTargetException;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;


public abstract class YangBinding<Y extends ChildOf, B extends Builder<Y>> // TODO: ChildOf<?>
        implements YangBindable, AutoCloseable {

    @Nonnull
    protected static final Logger LOG = LoggerFactory.getLogger(YangBinding.class);

    @Nonnull
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(0); // 0 -> no idle threads

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this);
    }

    public abstract class DatastoreBinding implements DataTreeChangeListener<Y> {

        @Nonnull
        private final LogicalDatastoreType storeType;

        @Nonnull
        private final YangBinding<Y, B> object;

        @Nonnull
        public InstanceIdentifier<Y> getIid() {
            return this.object.getIid();
        }

        @Nonnull
        public DataTreeIdentifier<Y> getDataTreeId() {
            return DataTreeIdentifier.create(this.storeType, this.getIid());
        }

        protected DatastoreBinding(@Nonnull final LogicalDatastoreType storeType, @Nonnull final YangBinding<Y, B> object) {
            this.storeType = storeType;
            this.object = object;
        }

        @Override
        public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Y>> changes) {
            LOG.info("Applying changed {} Data to: {}", this.storeType, this.object);

            for (@Nonnull final var change : changes) {
                @Nonnull final var node = change.getRootNode();
                switch (node.getModificationType()) {
                    case WRITE:
                        this.object.applyData(this.storeType, node.getDataAfter());
                        continue;

                    case SUBTREE_MODIFIED:
                        // TODO!
                        // continue;

                    case DELETE:
                        // TODO!
                }
            }
        }
    }

    public final class ConfigurationDatastoreBinding extends DatastoreBinding {

        public ConfigurationDatastoreBinding(@Nonnull final YangBinding<Y, B> object) {
            super(LogicalDatastoreType.CONFIGURATION, object);
        }
    }

    @Nonnull
    public ConfigurationDatastoreBinding createConfigurationDatastoreBinding() {
        return new ConfigurationDatastoreBinding(this);
    }

    public final class OperationalDatastoreBinding extends DatastoreBinding {

        public OperationalDatastoreBinding(@Nonnull final YangBinding<Y, B> object) {
            super(LogicalDatastoreType.OPERATIONAL, object);
        }
    }

    @Nonnull
    public OperationalDatastoreBinding createOperationalDatastoreBinding() {
        return new OperationalDatastoreBinding(this);
    }

    @Nonnull
    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public Class<Y> getDataClass() {
        return (Class<Y>) (new TypeToken<Y>(this.getClass()) {}).getRawType();
    }

    @Nonnull
    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public Class<B> getBuilderClass() {
        return (Class<B>) (new TypeToken<B>(this.getClass()) {}).getRawType();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public InstanceIdentifier.InstanceIdentifierBuilder<Y> getIidBuilder() {
        if (this.owner == null) {
            return InstanceIdentifier.builder(this.getDataClass());
        }

        return this.owner.getIidBuilder().child(this.getDataClass());
    }

    @Nonnull
    public InstanceIdentifier<Y> getIid() {
        return this.getIidBuilder().build();
    }

    @Nullable
    protected YangBinding<?, ?> owner = null;

    @Nullable
    public Optional<YangBinding<?, ?>> getOwner() {
        return Optional.ofNullable(this.owner);
    }

    public void makeOwned(
            @Nonnull @SuppressWarnings({"unused"}) final Owned.Maker maker, @Nullable final YangBinding<?, ?> owner) {

        this.owner = owner;
    }

    @Nonnull
    private final Map<LogicalDatastoreType, Consumer<YangData<Y>>> dataAppliers = Collections.synchronizedMap(
                    new EnumMap<>(LogicalDatastoreType.class));

    protected void appliesConfigurationDataUsing(@Nullable final Consumer<YangData<Y>> applier) {
        this.dataAppliers.put(LogicalDatastoreType.CONFIGURATION, applier);
    }

    protected void appliesOperationalDataUsing(@Nullable final Consumer<YangData<Y>> applier) {
        this.dataAppliers.put(LogicalDatastoreType.OPERATIONAL, applier);
    }

    public void applyConfigurationData(@Nonnull final YangData<Y> data) {
        @Nullable final var applier = this.dataAppliers.get(LogicalDatastoreType.CONFIGURATION);
        if (applier != null) {
            applier.accept(data);
        }
    }

    public void applyConfigurationData(@Nullable final Y data) {
        if (data != null) {
            this.applyConfigurationData(YangData.of(data));
        }
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private final AtomicReference<FluentFuture<Boolean>> dataApplyingFuture =
            new AtomicReference<>(FluentFuture.from(Futures.immediateFuture(false)));

    @Nonnull
    public CompletableFuture<Boolean> awaitOperationalDataApplying() {
        return FutureConverter.toCompletableFuture(this.dataApplyingFuture.get());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public final synchronized FluentFuture<Boolean> applyOperationalData(@Nonnull final YangData<Y> data) {
        @Nullable final var applier = this.dataAppliers.get(LogicalDatastoreType.OPERATIONAL);

        if (applier != null) {
            @Nonnull final var future = this.dataApplyingFuture.get().transform(applied -> {
                applier.accept(data);
                return true;

            }, this.executor);

            this.dataApplyingFuture.set(future);
            return future;
        }

        return this.dataApplyingFuture.get();
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public final synchronized FluentFuture<Boolean> applyOperationalData(@Nullable final Y data) {
        if (data != null) {
            return this.applyOperationalData(YangData.of(data));
        }

        return this.dataApplyingFuture.get().transform(applied -> false, MoreExecutors.directExecutor());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private FluentFuture<Boolean> applyData(@Nonnull final LogicalDatastoreType storeType, @Nonnull final YangData<Y> data) {
        switch (storeType) {
            case OPERATIONAL:
                return this.applyOperationalData(data);

            case CONFIGURATION:
                this.applyConfigurationData(data);
        }

        return FluentFuture.from(Futures.immediateFuture(false));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private FluentFuture<Boolean> applyData(@Nonnull final LogicalDatastoreType storeType, @Nullable final Y data) {
        if (data != null) {
            return this.applyData(storeType, YangData.of(data));
        }

        return FluentFuture.from(Futures.immediateFuture(false));
    }

    @Nullable
    private Function<B, B> operationalDataProvider = null;

    protected void providesOperationalDataUsing(@Nullable final Function<B, B> provider) {
        this.operationalDataProvider = provider;
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public synchronized FluentFuture<YangData<Y>> provideOperationalData() {
        @Nullable final var provider = this.operationalDataProvider;
        if (provider == null) {
            return this.dataApplyingFuture.get().transform(applied -> YangData.empty(), MoreExecutors.directExecutor());
        }

        return this.dataApplyingFuture.get().transform(applied -> {
            @Nonnull final B builder;
            try {
                builder = this.getBuilderClass().getDeclaredConstructor().newInstance();

            } catch (final
                    NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {

                throw new Error(e);
            }

            return YangData.of(provider.apply(builder).build());

        }, this.executor);
    }

    public void writeDataTo(@Nonnull final YangPool pool, @Nonnull final LogicalDatastoreType storeType) {
        pool.writeData(storeType, this);
    }

    public void deleteDataFrom(@Nonnull final YangPool pool, @Nonnull final LogicalDatastoreType storeType) {
        pool.deleteData(storeType, this);
    }

    @Override
    public void close() {
        // this.deleteDataFrom(LogicalDatastoreType.OPERATIONAL);
    }
}
