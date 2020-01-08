package com.adva.netemu;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.opendaylight.yangtools.yang.binding.InstanceIdentifier
        .InstanceIdentifierBuilder;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;


public abstract class YangModeled<T extends ChildOf, B extends Builder<T>>
        implements AutoCloseable {

    protected static final Logger LOG = LoggerFactory.getLogger(
            YangModeled.class);

    public static abstract class ListItem<
            T extends ChildOf & Identifiable<K>,
            K extends Identifier<T>,
            B extends Builder<T>>

            extends YangModeled<T, B> {

        @Nonnull @Override
        public InstanceIdentifierBuilder<T> getIidBuilder() {
            return this._owner.getIidBuilder().child(
                    this.getDataClass(), this.getKey());
        }

        @Nonnull
        public abstract K getKey();
    }

    @Nonnull
    public Class<T> getDataClass() {
        return (Class<T>) (new TypeToken<T>(this.getClass()) {}).getRawType();
    }

    @Nonnull
    public Class<B> getBuilderClass() {
        return (Class<B>) (new TypeToken<B>(this.getClass()) {}).getRawType();
    }

    private DataBroker _broker;

    public void setDataBroker(@Nonnull final DataBroker broker) {
        this._broker = broker;
    }

    @Nonnull
    public InstanceIdentifierBuilder<T> getIidBuilder() {
        if (this._owner == null) {
            return InstanceIdentifier.builder(this.getDataClass());
        }

        return this._owner.getIidBuilder().child(this.getDataClass());
    }

    public InstanceIdentifier<T> getIid() {
        return this.getIidBuilder().build();
    }

    protected YangModeled _owner = null;

    @Nullable
    public YangModeled getOwner() {
        return this._owner;
    }

    public void makeOwned(
            @Nonnull final Owned.Maker __, @Nonnull final YangModeled owner) {

        this._owner = owner;
    }

    private Function<B, B> _providerAction = null;

    protected void provideYangData(@Nonnull final Function<B, B> action) {
        this._providerAction = action;
    }

    @Nullable
    public T toYangData() {
        if (this._providerAction == null) {
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

        return this._providerAction.apply(builder).build();
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
