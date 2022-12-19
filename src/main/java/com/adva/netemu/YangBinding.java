package com.adva.netemu;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
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
import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

import com.adva.netemu.driver.EmuDriver;


public abstract class YangBinding<Y extends ChildOf, B extends YangBuilder<Y>> // TODO: ChildOf<?>
        implements YangBindable, AutoCloseable {

    @Nonnull
    protected static final Logger LOG = LoggerFactory.getLogger(YangBinding.class);

    @Nonnull
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(0); // 0 -> no idle threads

    @Nonnull
    private final AtomicReference<YangPool> yangPool = new AtomicReference<>();

    @Nonnull
    public Optional<YangPool> getYangPool() {
        return Optional.ofNullable(this.yangPool.get());
    }

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this);
    }

    public abstract class DatastoreBinding implements DataTreeChangeListener<Y> {

        @Nonnull
        private final LogicalDatastoreType storeType;

        @Nonnull
        public LogicalDatastoreType storeType() {
            return this.storeType;
        }

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
            LOG.debug("Applying changed {} Data to: {}", this.storeType, this.object);

            for (@Nonnull final var change : changes) {
                @Nonnull final var node = change.getRootNode();
                switch (node.getModificationType()) {
                    case WRITE -> this.object.applyData(this.storeType, node.getDataAfter());

                    // TODO: Different application method (?)
                    case SUBTREE_MODIFIED -> this.object.applyData(this.storeType, node.getDataAfter());

                    // TODO!
                    case DELETE -> {}
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

    public static abstract class ChildBinding<C_Y extends ChildOf, C_B extends YangBuilder<C_Y>> extends YangBinding<C_Y, C_B> {

        @Nonnull
        private final YangBinding<?, ?> parentBinding;

        @Nonnull
        public YangBinding<?, ?> parentBinding() {
            return this.parentBinding;
        }

        protected <P_Y extends ChildOf, P_B extends YangBuilder<P_Y>> ChildBinding(
                @Nonnull final Class<C_Y> dataClass,
                @Nonnull final Class<C_B> builderClass,
                @Nonnull final YangBinding<P_Y, P_B> parentBinding) {

            this.parentBinding = parentBinding;
        }
    }

    public static abstract class ChildListBinding
            <C_Y extends ChildOf & Identifiable<C_K>, C_K extends Identifier<C_Y>, C_B extends YangBuilder<C_Y>>
            extends YangListBinding<C_Y, C_K, C_B> {

        @Nonnull
        private final YangBinding<?, ?> parentBinding;

        @Nonnull
        public YangBinding<?, ?> parentBinding() {
            return this.parentBinding;
        }

        @Nonnull
        private final C_K listKey;

        protected <P_Y extends ChildOf, P_B extends YangBuilder<P_Y>> ChildListBinding(
                @Nonnull final Class<C_Y> dataClass,
                @Nonnull final Class<C_K> listKeyClass,
                @Nonnull final Class<C_B> builderClass,
                @Nonnull final YangBinding<P_Y, P_B> parentBinding,
                @Nonnull final C_K listKey) {

            this.parentBinding = parentBinding;
            this.listKey = listKey;
        }

        @Nonnull @Override
        public C_K getKey() {
            return this.listKey;
        }
    }

    @Nonnull
    public Optional<Class<? extends DataObject>> getParentDataClass() {
        return Optional.empty();
    }

    @Nonnull
    public Optional<Class<? extends Augmentation>> getParentAugmentationClass() {
        return Optional.empty();
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage", "unchecked"})
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

        return this.getParentAugmentationClass().map(augmentationClass ->
                this.owner.getIidBuilder().augmentation(augmentationClass).child(this.getDataClass())

        ).orElseGet(() -> this.owner.getIidBuilder().child(this.getDataClass()));
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

    public void setYangPool(@Nonnull final YangPool yangPool) {
        this.yangPool.set(yangPool);
    }

    @Nonnull
    public <T extends YangBindable> T registerChild(@Nonnull final T object) {
        object.getYangBinding().ifPresent(binding -> {
            binding.owner = this;
        });

        this.getYangPool().ifPresent(yangPool -> {
            yangPool.registerYangBindable(object);
        });

        return object;
    }

    @Nonnull
    public <T extends YangListBindable> T registerChild(@Nonnull final T object) {
        object.getYangListBinding().ifPresent(binding -> {
            binding.owner = this;
        });

        this.getYangPool().ifPresent(yangPool -> {
            yangPool.registerYangBindable(object);
        });

        return object;
    }

    @Nonnull
    public <C extends Collection<T>, T extends YangListBindable> C registerChildren(@Nonnull final C objects) {
        for (@Nonnull final var object : objects) {
            this.registerChild(object);
        }

        return objects;
    }

    @Nonnull
    private final List<EmuDriver.Binding<?>> driverBindings = Collections.synchronizedList(new ArrayList<>());

    @Nonnull @SuppressWarnings({"unchecked"})
    public <D extends EmuDriver.Binding<?>>
    Optional<D> findDriverBindingOf(@Nonnull final Class<? extends EmuDriver.Binding<?>> driverBindingClass) {
        synchronized (this.driverBindings) {
            return StreamEx.of(this.driverBindings).findFirst(driverBindingClass::isInstance)
                    .map(driverBinding -> (D) driverBinding);
        }
    }

    @Nonnull @SuppressWarnings({"unchecked"})
    public <D extends EmuDriver> Optional<EmuDriver.Binding<D>> findDriverBindingFor(@Nonnull final Class<D> driverClass) {
        synchronized (this.driverBindings) {
            return StreamEx.of(this.driverBindings).findFirst(driverBinding -> driverBinding.getDriverClass().equals(driverClass))
                    .map(driverBinding -> (EmuDriver.Binding<D>) driverBinding);
        }
    }

    public void addDriverBinding(@Nonnull final EmuDriver.Binding<?> driverBinding) {
        this.driverBindings.add(driverBinding);
    }

    @Nonnull
    private final Map<LogicalDatastoreType, Consumer<YangData<Y>>> dataAppliers = Collections.synchronizedMap(
                    new EnumMap<>(LogicalDatastoreType.class));

    protected void setConfigurationDataApplier(@Nullable final Consumer<YangData<Y>> applier) {
        this.dataAppliers.put(LogicalDatastoreType.CONFIGURATION, applier);
    }

    protected void setOperationalDataApplier(@Nullable final Consumer<YangData<Y>> applier) {
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
    public FluentFuture<Boolean> applyOperationalData(@Nonnull final YangData<Y> data) {
        return this.applyOperationalData(data, this.dataAppliers.get(LogicalDatastoreType.OPERATIONAL));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<Boolean> applyOperationalData(
            @Nonnull final YangData<Y> data, @Nullable final Consumer<YangData<Y>> applier) {

        synchronized (this.dataApplyingFuture) {
            if (applier != null) {
                this.dataApplyingFuture.set(this.dataApplyingFuture.get().transform(applied -> {
                    applier.accept(data);
                    return true;

                }, this.executor));
            }

            return this.dataApplyingFuture.get();
        }
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<Boolean> applyOperationalData(@Nonnull final YangData<Y> data, @Nonnull final Runnable finalizer) {
        @Nullable final var applier = this.dataAppliers.get(LogicalDatastoreType.OPERATIONAL);

        synchronized (this.dataApplyingFuture) {
            if (applier != null) {
                this.dataApplyingFuture.set(this.dataApplyingFuture.get().transform(applied -> {
                    applier.accept(data);
                    return true;

                }, this.executor));
            }

            return this.dataApplyingFuture.get().transform(applied -> {
                finalizer.run();
                return applied;

            }, this.executor);
        }
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<Boolean> applyOperationalData(@Nullable final Y data) {
        if (data != null) {
            return this.applyOperationalData(YangData.of(data));
        }

        return this.dataApplyingFuture.get().transform(applied -> false, MoreExecutors.directExecutor());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private FluentFuture<Boolean> applyData(@Nonnull final LogicalDatastoreType storeType, @Nonnull final YangData<Y> data) {
        return switch (storeType) {
            case OPERATIONAL -> this.applyOperationalData(data);

            case CONFIGURATION -> {
                this.applyConfigurationData(data);
                yield FluentFuture.from(Futures.immediateFuture(false));
            }
        };
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private FluentFuture<Boolean> applyData(@Nonnull final LogicalDatastoreType storeType, @Nullable final Y data) {
        if (data != null) {
            return this.applyData(storeType, YangData.of(data));
        }

        return FluentFuture.from(Futures.immediateFuture(false));
    }

    @Nullable
    private Function<B, B> configurationDataProvider = null;

    protected void setConfigurationDataProvider(@Nullable final Function<B, B> provider) {
        this.configurationDataProvider = provider;
    }

    @Nullable
    private Function<B, B> operationalDataProvider = null;

    protected void setOperationalDataProvider(@Nullable final Function<B, B> provider) {
        this.operationalDataProvider = provider;
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public synchronized FluentFuture<YangData<Y>> provideConfigurationData() {
        @Nullable final var provider = this.configurationDataProvider;
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
        pool.writeBindingData(storeType, this);
    }

    public void deleteDataFrom(@Nonnull final YangPool pool, @Nonnull final LogicalDatastoreType storeType) {
        pool.deleteData(storeType, this);
    }

    @Override
    public void close() {
        // this.deleteDataFrom(LogicalDatastoreType.OPERATIONAL);
    }
}
