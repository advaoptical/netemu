package com.adva.netemu;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.reflect.TypeToken;

import org.opendaylight.yangtools.concepts.Builder;

import org.opendaylight.yangtools.yang.binding.ChildOf;

// import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;


public abstract class YangModeled<T extends ChildOf, B extends Builder<T>>
        implements AutoCloseable {

/*
    public abstract class WithKey<K> extends YangModeled<T> {

        public WithKey(@Nonnull final YangModeled owner) {
            super(owner);
        }

        @Override
        public InstanceIdentifier.InstanceIdentifierBuilder getIidBuilder() {
            return this.getOwner().getIidBuilder().child(
                    this.getDataClass(), this.getKey());
        }
    }
*/

    public Class<T> getDataClass() {
        return (Class<T>) (new TypeToken<T>(this.getClass()) {}).getRawType();
    }

    public Class<B> getBuilderClass() {
        return (Class<B>) (new TypeToken<B>(this.getClass()) {}).getRawType();
    }

    private DataBroker _broker;

    public void setDataBroker(@Nonnull final DataBroker broker) {
        this._broker = broker;
    }

    private InstanceIdentifier<T> _iid;

    public InstanceIdentifier<T> getIid() {
        return this._iid;
    }

    public InstanceIdentifier.InstanceIdentifierBuilder<T> getIidBuilder() {
        if (this._owner == null) {
            return InstanceIdentifier.builder(this.getDataClass());
        }

        return this._owner.getIidBuilder().child(this.getDataClass());
    }

    public void _buildIid() {
        this._iid = this.getIidBuilder().build();
    }

    private YangModeled _owner;

    public YangModeled getOwner() {
        return this._owner;
    }

    public void makeOwned(
            @Nonnull final Owned.Maker __, @Nonnull final YangModeled owner) {

        this._owner = owner;
    }

    private @Nonnull Function<B, B> _providerAction;

    protected void provideYangData(@Nonnull final Function<B, B> action) {
        this._providerAction = action;
    }

    public T toYangData() {
        B builder;
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

    public void init() {
        if (this._broker == null) {
            return;
        }

        final var data = this.toYangData();
        this._buildIid();

        final var txn = this._broker.newWriteOnlyTransaction();
        txn.put(LogicalDatastoreType.OPERATIONAL, this._iid, data);
    }

    @Override
    public void close() throws Exception {
        if ((this._broker == null) || (this._iid == null)) {
            return;
        }

        final var txn = this._broker.newWriteOnlyTransaction();
        txn.delete(LogicalDatastoreType.OPERATIONAL, this._iid);
    }
}
