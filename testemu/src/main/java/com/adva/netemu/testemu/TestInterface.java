package com.adva.netemu.testemu;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.interfaces.Interface;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.interfaces.InterfaceKey;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.interfaces.InterfaceBuilder;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.InterfaceType;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .iana._if.type.rev170119.EthernetCsmacd;

import com.adva.netemu.YangModeled;


public class TestInterface extends YangModeled.ListItem<
        Interface, InterfaceKey, InterfaceBuilder> {

    @Nonnull
    private static Class<? extends InterfaceType> IETF_INTERFACE_TYPE =
            EthernetCsmacd.class;

    @Nonnull
    private final String _name;

    @Nonnull
    public String getName() {
        return this._name;
    }

    @Nonnull
    private final AtomicBoolean _enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return this._enabled.get();
    }

    public boolean isDisabled() {
        return !this._enabled.get();
    }

    @Nonnull @Override
    public InterfaceKey getKey() {
        return new InterfaceKey(this._name);
    }

    public TestInterface(@Nonnull final String name) {
        this._name = name;

        super.providesOperationalDataUsing((builder) -> builder
                .setType(IETF_INTERFACE_TYPE)
                .setName(this._name)
                .setEnabled(this._enabled.get()));
    }

    @Override
    public void applyConfigurationData(@Nonnull final Interface data) {
        this._enabled.set(data.isEnabled());
    }

    public void enable() {
        this._enabled.set(true);
    }

    public void disable() {
        this._enabled.set(false);
    }
}
