package com.adva.netemu.testemu;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.Interfaces;

import com.adva.netemu.Owned;
import com.adva.netemu.YangBindable;
import com.adva.netemu.YangBinding;
import com.adva.netemu.YangBound;


@YangBound(
        context = NetEmuDefined.class,
        namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces",
        value = "interfaces")

public class TestDevice implements YangBindable {

    @Nonnull
    private final TestDevice$YangBinding _yangBinding =
            new TestDevice$YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this._yangBinding);
    }

    @Nonnull
    private final AtomicReference<List<TestInterface>> _interfaces =
            new AtomicReference<>(List.of());

    @Nonnull
    public List<TestInterface> getInterfaces() {
        return List.copyOf(this._interfaces.get());
    }

    private TestDevice(@Nonnegative final Integer numberOfInterfaces) {
        this();
        this._interfaces.set(Owned.by(this,
                IntStreamEx.range(0, numberOfInterfaces)
                        .mapToObj(n -> TestInterface.withName("test" + n))
                        .toImmutableList()));
    }

    public TestDevice() {
        this._yangBinding.appliesConfigurationDataUsing(data -> {
            this._interfaces.set(Owned.by(this,
                    StreamEx.of(data.nonnullInterface())
                            .map(TestInterface::fromConfigurationData)
                            .toImmutableList()));
        });

        this._yangBinding.appliesOperationalDataUsing(data -> {
            synchronized (this._interfaces) {
                for (final var intfData : data.nonnullInterface()) {
                    TestInterface$Yang.bindingStreamOf(this._interfaces.get())
                            .findFirst(intf ->
                                    intf.getKey().equals(intfData.key()))

                            .ifPresent(intf ->
                                    intf.applyOperationalData(intfData));
                }
            }
        });

        this._yangBinding.providesOperationalDataUsing(builder -> builder
                .setInterface(TestInterface$Yang
                        .streamOperationalDataFrom(this._interfaces.get())
                        .toImmutableList()));
    }

    @Nonnull
    public static TestDevice withNumberOfInterfaces(
            @Nonnegative final Integer number) {

        return new TestDevice(number);
    }

    @Nonnull
    public static TestDevice fromConfigurationData(
            @Nonnull final Interfaces data) {

        final var device = new TestDevice();
        device._yangBinding.applyConfigurationData(data);
        return device;
    }
}
