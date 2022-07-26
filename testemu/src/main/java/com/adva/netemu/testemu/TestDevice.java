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


@YangBound(context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces")
public class TestDevice implements YangBindable {

    @Nonnull
    private final TestDevice_YangBinding yangBinding = new TestDevice_YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final AtomicReference<List<TestInterface>> interfaces = new AtomicReference<>(List.of());

    @Nonnull
    public List<TestInterface> interfaces() {
        return List.copyOf(this.interfaces.get());
    }

    public TestDevice() {
        try {
            this.interfaces.set(StreamEx.of(NetworkInterface.getNetworkInterfaces()).nonNull().map(TestInterface::from)
                    .toImmutableList());

        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }

        this.yangBinding.appliesConfigurationDataUsing(data -> {
            this.interfaces.set(Owned.by(this, data.streamInterface().map(TestInterface_Yang.Data::from)
                    .map(TestInterface::fromConfigurationData)
                    .toImmutableList()));

        }).appliesOperationalDataUsing(data -> {
            synchronized (this.interfaces) {
                for (@Nonnull final var interfaceData : data.streamInterface().map(TestInterface_Yang.Data::from)) {
                    TestInterface_Yang.bindingStreamOf(this.interfaces.get())
                            .findFirst(binding -> interfaceData.getName().map(binding.getKey().getName()::equals)
                                    .orElseThrow(() -> new IllegalArgumentException("No 'name' leaf value present in YANG Data")))

                            .ifPresent(binding -> binding.applyOperationalData(interfaceData));
                }
            }

        }).providesOperationalDataUsing(builder -> builder
                .setInterface(TestInterface_Yang.listOperationalDataFrom(this.interfaces.get())));
    }

    @Nonnull
    public static TestDevice fromConfigurationData(@Nonnull final TestDevice_Yang.Data data) {
        @Nonnull final var instance = new TestDevice();
        instance.yangBinding.applyConfigurationData(data);
        return instance;
    }
}
