package com.adva.netemu.testemu;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.InterfaceType;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .iana._if.type.rev170119.EthernetCsmacd;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.interfaces.Interface;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


@YangListBound(
        context = NetEmuDefined.class,
        namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces",
        value = "interfaces/interface")

public class TestInterface implements YangListBindable {

    @Nonnull
    private static final Class<? extends InterfaceType> IETF_INTERFACE_TYPE =
            EthernetCsmacd.class;

    @Nonnull
    private final TestInterface$YangListBinding _yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this._yangBinding);
    }

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

    private TestInterface(@Nonnull final String name) {
        this._name = name;
        this._yangBinding =
                TestInterface$YangListBinding.withKey(
                        TestInterface$Yang.ListKey.from(name));

        this._yangBinding.appliesConfigurationDataUsing(data -> {
            this._enabled.set(data.isEnabled());
        });

        this._yangBinding.appliesOperationalDataUsing(data -> {
            this._enabled.set(data.isEnabled());
        });

        this._yangBinding.providesOperationalDataUsing((builder) -> builder
                .setType(IETF_INTERFACE_TYPE)
                .setName(this._name)
                .setEnabled(this._enabled.get()));
    }

    @Nonnull
    public static TestInterface withName(@Nonnull final String name) {
        return new TestInterface(name);
    }

    @Nonnull
    public static TestInterface fromConfigurationData(
            @Nonnull final Interface data) {

        final var intf = TestInterface.withName(data.getName());
        intf._yangBinding.applyConfigurationData(data);
        return intf;
    }

    public void enable() {
        this._enabled.set(true);
    }

    public void disable() {
        this._enabled.set(false);
    }
}
