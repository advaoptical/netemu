package emu.skeleton.renameIt.networkExample;

import java.net.SocketException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;


@YangListBound(
        context = NetEmuDefined.class,
        namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces",
        value = "interfaces/interface")

public class NetworkInterface implements YangListBindable {

    @Nonnull
    private final Logger LOG = LoggerFactory.getLogger(NetworkInterface.class);

    @Nonnull
    private final NetworkInterface_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final String name;

    @Nonnull
    private final AtomicReference<java.net.NetworkInterface> adapter = new AtomicReference<>();

    @Nonnull
    public final Optional<java.net.NetworkInterface> getAdapter() {
        return Optional.ofNullable(this.adapter.get());
    }

    public void setAdapter(@Nonnull final java.net.NetworkInterface adapter) {
        this.adapter.set(adapter);
    }

    @Nonnull
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public void enable() {
        LOG.info("Enabling network interface '{}'", this.name);
        this.enabled.set(true);
    }

    public void disable() {
        LOG.info("Disabling network interface '{}'", this.name);
        this.enabled.set(false);
    }

    @Nonnull
    public Optional<String> getDescription() {
        return Optional.ofNullable(this.adapter.get()).map(java.net.NetworkInterface::getDisplayName);
    }

    @Nonnull
    public Optional<String> getMacAddress() {
        return Optional.ofNullable(this.adapter.get()).flatMap(adapter -> {
            try {
                return Optional.ofNullable(adapter.getHardwareAddress()).map(address -> DatatypeConverter.printHexBinary(address)
                        .replaceAll("(..)", ":$1").substring(1));

            } catch (final SocketException e) {
                throw new RuntimeException(String.format("Failed reading MAC address from network adapter %s", adapter), e);
            }
        });
    }

    private NetworkInterface(@Nonnull final String name) {
        this.name = name;

        this.yangBinding = NetworkInterface_YangBinding.withKey(NetworkInterface_Yang.ListKey.from(this.name));
        this.yangBinding.providesConfigurationDataUsing(builder -> builder
                .setName(this.name)
                .setDescription(this.getDescription())

                .setType(EthernetCsmacd.class)
                .setEnabled(this.enabled.get())

        ).providesOperationalDataUsing(builder -> builder
                .setName(this.name)
                .setIfIndex(1)
                .setPhysAddress(this.getMacAddress().map(PhysAddress::new))

                .setAdminStatus_Up()
                .setOperStatus_Up_if(this.enabled::get)
                .setOperStatus_Down_if(() -> !this.enabled.get())

                .setStatistics(statisticsBuilder -> statisticsBuilder
                        .setDiscontinuityTime(new DateAndTime("1970-01-01T00:00:00Z")))

        ).appliesConfigurationDataUsing(data -> {
            data.getEnabled().ifPresent(enabled -> {
                if (enabled) {
                    this.enable();

                } else {
                    this.disable();
                }
            });
        });
    }

    private NetworkInterface(@Nonnull final java.net.NetworkInterface adapter) {
        this(adapter.getName());
        this.adapter.set(adapter);
    }

    @Nonnull
    public static NetworkInterface fromAdapter(@Nonnull final java.net.NetworkInterface adapter) {
        return new NetworkInterface(adapter);
    }

    @Nonnull
    public static NetworkInterface withName(@Nonnull final String name) {
        return new NetworkInterface(name);
    }

    @Nonnull
    public static NetworkInterface fromConfiguration(@Nonnull final NetworkInterface_Yang.Data data) {
        @Nonnull final var instance = NetworkInterface.withName(data.requireName());
        instance.yangBinding.applyConfigurationData(data);
        return instance;
    }
}
