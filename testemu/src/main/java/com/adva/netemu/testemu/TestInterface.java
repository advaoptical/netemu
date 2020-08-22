package com.adva.netemu.testemu;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;


@YangListBound(
        context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces/interface")

public class TestInterface implements YangListBindable {
e
    @Nonnull
    private static final Class<? extends InterfaceType> IETF_INTERFACE_TYPE = EthernetCsmacd.class;

    @Nonnull
    private final TestInterface_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final NetworkInterface adapter;

    @Nonnull
    public String name() {
        return this.adapter.getName();
    }

    @Nonnull
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return this.enabled.get();
    }

    public boolean isDisabled() {
        return !this.enabled.get();
    }

    private TestInterface(@Nonnull final NetworkInterface adapter) {
        this.adapter = adapter;

        @Nonnull final Optional<String> macAddress;
        try {
            macAddress = Optional.ofNullable(adapter.getHardwareAddress()).map(DatatypeConverter::printHexBinary)
                    .map(hex -> hex.replaceAll("(..)", ":$1").substring(1));

        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }

        this.yangBinding = TestInterface_YangBinding.withKey(TestInterface_Yang.ListKey.from(adapter.getName()))

                .appliesConfigurationDataUsing(data -> {
                    data.getEnabled().ifPresent(this.enabled::set);
                })

                .appliesOperationalDataUsing(data -> {
                    data.getEnabled().ifPresent(this.enabled::set);
                })

                .providesOperationalDataUsing(builder -> builder
                        .setType(IETF_INTERFACE_TYPE)
                        .setName(adapter.getName())
                        .setEnabled(this.enabled.get())

                        .setDescription(adapter.getDisplayName())
                        .setPhysAddress(macAddress.map(PhysAddress::new)));
    }

    @Nonnull
    public static TestInterface from(@Nonnull final NetworkInterface adapter) {
        return new TestInterface(adapter);
    }

    @Nonnull
    public static TestInterface withName(@Nonnull final String name) {
        try {
            return new TestInterface(NetworkInterface.getByName(name));

        } catch (SocketException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nonnull
    public static TestInterface fromConfigurationData(@Nonnull final TestInterface_Yang.Data data) {
        @Nonnull final var instance = TestInterface.withName(data.getName().orElseThrow(() -> new IllegalArgumentException(
                "No 'name' leaf value present in YANG Data")));

        instance.yangBinding.applyConfigurationData(data);
        return instance;
    }

    public void enable() {
        this.enabled.set(true);
    }

    public void disable() {
        this.enabled.set(false);
    }
}
