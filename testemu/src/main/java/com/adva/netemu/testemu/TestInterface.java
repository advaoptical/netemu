package com.adva.netemu.testemu;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.interfaces.Interface;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.interfaces.InterfaceBuilder;

import com.adva.netemu.YangModeled;


public class TestInterface extends YangModeled<Interface, InterfaceBuilder> {

    private @Nonnull final String _name;

    @Nonnull
    public String getName() {
        return this._name;
    }

    private AtomicBoolean _enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return this._enabled.get();
    }

    public boolean isDisabled() {
        return !this._enabled.get();
    }

    public TestInterface(@Nonnull final String name) {
        this._name = name;

        super.provideYangData((builder) -> builder
                .setName(this._name)
                .setEnabled(this._enabled.get()));
    }

    public void enable() {
        this._enabled.set(true);
    }

    public void disable() {
        this._enabled.set(false);
    }
}
