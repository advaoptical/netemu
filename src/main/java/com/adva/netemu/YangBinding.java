package com.adva.netemu;

import java.lang.reflect.InvocationTargetException;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.reflect.TypeToken;

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

    public void applyOperationalData(@Nonnull final YangData<Y> data) {
        @Nullable final var applier = this.dataAppliers.get(LogicalDatastoreType.OPERATIONAL);
        if (applier != null) {
            applier.accept(data);
        }
    }

    public void applyOperationalData(@Nullable final Y data) {
        if (data != null) {
            this.applyOperationalData(YangData.of(data));
        }
    }

    private void applyData(@Nonnull final LogicalDatastoreType storeType, @Nonnull final YangData<Y> data) {
        switch (storeType) {
            case CONFIGURATION:
                this.applyConfigurationData(data);
                return;

            case OPERATIONAL:
                this.applyOperationalData(data);
        }
    }

    private void applyData(@Nonnull final LogicalDatastoreType storeType, @Nullable final Y data) {
        if (data != null) {
            this.applyData(storeType, YangData.of(data));
        }
    }

    @Nullable
    private Function<B, B> operationalDataProvider = null;

    protected void providesOperationalDataUsing(@Nullable final Function<B, B> provider) {
        this.operationalDataProvider = provider;
    }

    @Nullable
    public Y provideOperationalData() {
        if (this.operationalDataProvider == null) {
            return null;
        }

        @Nonnull final B builder;
        try {
            builder = this.getBuilderClass().getDeclaredConstructor().newInstance();

        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new Error(e);
        }

        return this.operationalDataProvider.apply(builder).build();
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
