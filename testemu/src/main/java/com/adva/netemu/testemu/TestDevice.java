package com.adva.netemu.testemu;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adva.netemu.YangBindable;
import com.adva.netemu.YangBinding;
import com.adva.netemu.YangBound;
import com.adva.netemu.YangListBound;


@YangBound(context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces")
public class TestDevice implements YangBindable {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(TestDevice.class);

    @Nonnull
    private final TestDevice_YangBinding yangBinding = new TestDevice_YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final List<String> interfaceIndexRegistry = new ArrayList<>();

    @Nonnegative
    private int getInterfaceIndexForName(@Nonnull final String interfaceName) {
        int interfaceIndex;
        synchronized (this.interfaceIndexRegistry) {
            interfaceIndex = this.interfaceIndexRegistry.indexOf(interfaceName);

            if (interfaceIndex == -1) {
                interfaceIndex = this.interfaceIndexRegistry.size();
                this.interfaceIndexRegistry.add(interfaceName);
            }
        }

        return interfaceIndex;
    }

    @Nonnull
    private final YangListBound.Collection<TestInterface> interfaces;

    @Nonnull
    public Set<TestInterface> interfaces() {
        return Set.copyOf(this.interfaces);
    }

    public TestDevice() {
        this.interfaceIndexRegistry.add(null); // preserve 0-index, because if-index must be >= 1
        this.interfaces = new YangListBound.Collection<>(this.yangBinding);

        this.yangBinding.appliesConfigurationDataUsing(data -> {
            @Nonnull final var newInterfaces = this.interfaces.mergeAll(
                    data.streamInterface().map(interfaceData -> TestInterface
                            .withIndex(this.getInterfaceIndexForName(interfaceData.requireName()))
                            .fromConfigurationData(TestInterface_Yang.Data.from(interfaceData))),

                    (existingInterface, iface) -> {});

            LOG.info("New interfaces: {}", newInterfaces);

        }).appliesOperationalDataUsing(data -> {
            synchronized (this.interfaces) {
                for (@Nonnull final var interfaceData : data.streamInterface().map(TestInterface_Yang.Data::from)) {
                    TestInterface_Yang.bindingStreamOf(this.interfaces)
                            .findFirst(binding -> interfaceData.requireName().equals(binding.getKey().getName()))
                            .ifPresent(binding -> {
                                binding.applyOperationalData(interfaceData);
                            });
                }
            }

        }).providesConfigurationDataUsing(builder -> {
            @Nonnull final List<TestInterface> newInterfaces;
            try {
                newInterfaces = this.interfaces.mergeAll(
                        StreamEx.of(NetworkInterface.getNetworkInterfaces()).nonNull().map(adapter ->
                                TestInterface.withIndex(this.getInterfaceIndexForName(adapter.getName())).fromAdapter(adapter)),

                        (existingInterface, iface) -> {});

            } catch (final SocketException e) {
                throw new RuntimeException(e);
            }

            LOG.info("New interfaces: {}", newInterfaces);
            return builder
                    .setInterface(TestInterface_Yang.listConfigurationDataFrom(newInterfaces));
        });
    }

    @Nonnull
    public static TestDevice fromConfigurationData(@Nonnull final TestDevice_Yang.Data data) {
        @Nonnull final var instance = new TestDevice();
        instance.yangBinding.applyConfigurationData(data);
        return instance;
    }
}
