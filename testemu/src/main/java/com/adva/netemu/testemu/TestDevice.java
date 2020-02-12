package com.adva.netemu.testemu;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.adva.netemu.Owned;
import com.adva.netemu.YangBindable;
import com.adva.netemu.YangBinding;
import com.adva.netemu.YangBound;


@YangBound(
        context = NetEmuDefined.class,
        namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces",
        value = "interfaces")

public class TestDevice implements YangBindable {

    static class YangBindingConnector {

        private YangBindingConnector() {}
    }

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

    public TestDevice() {
        try {
            this._interfaces.set(StreamEx
                    .of(NetworkInterface.getNetworkInterfaces()).nonNull()
                    .map(TestInterface::from)
                    .toImmutableList());

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        @Nonnull final var connector = new YangBindingConnector();

        this._yangBinding.appliesConfigurationDataUsing(connector, data -> {
            this._interfaces.set(Owned.by(this, StreamEx
                    .of(data.map(yang -> yang.nonnullInterface())
                            .orElse(List.of()))

                    .map(TestInterface$Yang.Data::of)
                    .map(TestInterface::fromConfigurationData)
                    .toImmutableList()));
        });

        this._yangBinding.appliesOperationalDataUsing(connector, data -> {
            synchronized (this._interfaces) {
                for (final var intfData : StreamEx
                        .of(data.map(yang -> yang.nonnullInterface())
                                .orElse(List.of()))

                        .map(TestInterface$Yang.Data::of)) {

                    TestInterface$Yang.bindingStreamOf(this._interfaces.get())
                            .findFirst(intf -> intfData.getName()
                                    .map(intf.getKey().getName()::equals)
                                    .orElse(false))

                            .ifPresent(intf ->
                                    intf.applyOperationalData(intfData));
                }
            }
        });

        this._yangBinding
                .providesOperationalDataUsing(connector, builder -> builder
                        .setInterface(TestInterface$Yang
                                .streamOperationalDataFrom(
                                        this._interfaces.get())

                                .toImmutableList()));
    }

    @Nonnull
    public static TestDevice fromConfigurationData(
            @Nonnull final TestDevice$Yang.Data data) {

        final var device = new TestDevice();
        device._yangBinding.applyConfigurationData(data);
        return device;
    }
}
