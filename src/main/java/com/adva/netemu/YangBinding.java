package com.adva.netemu;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import static java.util.Collections.synchronizedMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.opendaylight.yangtools.yang.binding.InstanceIdentifier
        .InstanceIdentifierBuilder;

import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;


public abstract class YangBinding<T extends ChildOf, B extends Builder<T>>
        implements AutoCloseable {

    protected static final Logger LOG = LoggerFactory.getLogger(
            YangBinding.class);

    public abstract class DataBinding
            implements DataTreeChangeListener<T> {

        @Nonnull
        private final LogicalDatastoreType _storeType;

        @Nonnull
        private final YangBinding<T, B> _object;

        @Nonnull
        public InstanceIdentifier<T> getIid() {
            return this._object.getIid();
        }

        @Nonnull
        public DataTreeIdentifier<T> getDataTreeId() {
            return DataTreeIdentifier.create(this._storeType, this.getIid());
        }

        protected DataBinding(
                @Nonnull final LogicalDatastoreType storeType,
                @Nonnull final YangBinding<T, B> object) {

            this._storeType = storeType;
            this._object = object;
        }

        @Override
        public void onDataTreeChanged(
                @Nonnull final Collection<DataTreeModification<T>> changes) {

            LOG.info("Applying changed {} Data to: {}",
                    this._storeType, this._object);

            for (final var change : changes) {
                final var node = change.getRootNode();
                switch (node.getModificationType()) {
                    case WRITE:
                        this._object.applyData(
                                this._storeType, node.getDataAfter());

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

    public final class ConfigurationDataBinding extends DataBinding {

        public ConfigurationDataBinding(
                @Nonnull final YangBinding<T, B> object) {

            super(LogicalDatastoreType.CONFIGURATION, object);
        }
    }

    @Nonnull
    public ConfigurationDataBinding createConfigurationDataBinding() {
        return new ConfigurationDataBinding(this);
    }

    public final class OperationalDataBinding extends DataBinding {

        public OperationalDataBinding(
                @Nonnull final YangBinding<T, B> object) {

            super(LogicalDatastoreType.OPERATIONAL, object);
        }
    }

    @Nonnull
    public OperationalDataBinding createOperationalDataBinding() {
        return new OperationalDataBinding(this);
    }

    @Nonnull
    public Class<T> getDataClass() {
        return (Class<T>) (new TypeToken<T>(this.getClass()) {}).getRawType();
    }

    @Nonnull
    public Class<B> getBuilderClass() {
        return (Class<B>) (new TypeToken<B>(this.getClass()) {}).getRawType();
    }

    @Nonnull
    public InstanceIdentifierBuilder<T> getIidBuilder() {
        if (this._owner == null) {
            return InstanceIdentifier.builder(this.getDataClass());
        }

        return this._owner.getIidBuilder().child(this.getDataClass());
    }

    @Nonnull
    public InstanceIdentifier<T> getIid() {
        return this.getIidBuilder().build();
    }

    @Nullable
    protected YangBinding _owner = null;

    @Nullable
    public YangBinding getOwner() {
        return this._owner;
    }

    public void makeOwned(
            @Nonnull final Owned.Maker __, @Nonnull final YangBinding owner) {

        this._owner = owner;
    }

    @Nonnull
    private final Map<LogicalDatastoreType, Function<T, T>> _dataAppliers =
            synchronizedMap(new EnumMap<>(LogicalDatastoreType.class));

    protected void appliesConfigurationDataUsing(
            @Nullable final Function<T, T> applier) {

        this._dataAppliers.put(LogicalDatastoreType.CONFIGURATION, applier);
    }

    protected void appliesOperationalDataUsing(
            @Nullable final Function<T, T> applier) {

        this._dataAppliers.put(LogicalDatastoreType.OPERATIONAL, applier);
    }

    public void applyConfigurationData(@Nonnull final T data) {
        final var applier = this._dataAppliers.get(LogicalDatastoreType.CONFIGURATION);
        if (applier != null) {
            applier.apply(data);
        }
    }

    public void applyOperationalData(@Nonnull final T data) {
        final var applier = this._dataAppliers.get(LogicalDatastoreType.OPERATIONAL);
        if (applier != null) {
            applier.apply(data);
        }
    }

    private void applyData(
            @Nonnull final LogicalDatastoreType storeType,
            @Nonnull final T data) {

        switch (storeType) {
            case CONFIGURATION:
                this.applyConfigurationData(data);
                return;

            case OPERATIONAL:
                this.applyOperationalData(data);
        }
    }

    @Nullable
    private Function<B, B> _operationalDataProvider = null;

    protected void providesOperationalDataUsing(
            @Nullable final Function<B, B> provider) {

        this._operationalDataProvider = provider;
    }

    @Nullable
    public T provideOperationalData() {
        if (this._operationalDataProvider == null) {
            return null;
        }

        final B builder;
        try {
            builder = this.getBuilderClass()
                    .getDeclaredConstructor().newInstance();

        } catch (final
                NoSuchMethodException |
                IllegalAccessException |
                InvocationTargetException |
                InstantiationException e) {

            e.printStackTrace();
            return null;
        }

        return this._operationalDataProvider.apply(builder).build();
    }

    public void writeDataTo(
            @Nonnull final YangPool pool,
            @Nonnull final LogicalDatastoreType storeType) {

        pool.writeData(this, storeType);
    }

    public void deleteDataFrom(
            @Nonnull final YangPool pool,
            @Nonnull final LogicalDatastoreType storeType) {

        pool.deleteData(this, storeType);
    }

    @Override
    public void close() {
        // this.deleteDataFrom(LogicalDatastoreType.OPERATIONAL);
    }
}
