package emu.skeleton.renameIt.networkExample;

import java.net.SocketException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.adva.netemu.YangBindable;
import com.adva.netemu.YangBinding;
import com.adva.netemu.YangBound;
import com.adva.netemu.YangListBound;


@YangBound(context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces")
public class Network implements YangBindable {

    @Nonnull
    private final Network_YangBinding yangBinding = new Network_YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final YangListBound.Collection<NetworkInterface> interfaces;

    private Network() {
        this.interfaces = new YangListBound.Collection<>(this.yangBinding);

        this.yangBinding.providesConfigurationDataUsing(builder -> {
            @Nonnull final List<? extends NetworkInterface> newInterfaces;
            try {
                newInterfaces = this.interfaces.mergeAll(
                        StreamEx.of(java.net.NetworkInterface.getNetworkInterfaces()).nonNull().map(NetworkInterface::fromAdapter),

                        (existingItem, item) -> {
                            item.getAdapter().ifPresent(adapter -> {
                                existingItem.setAdapter(adapter);
                            });
                        });

            } catch (final SocketException e) {
                throw new RuntimeException("Failed querying system network adapters", e);
            }

            return builder.setInterface(NetworkInterface_Yang.listConfigurationDataFrom(newInterfaces));

        }).appliesConfigurationDataUsing(data -> {
            @Nonnull final var newInterfaces = this.interfaces.mergeAll(
                    data.streamInterface().map(interfaceData -> NetworkInterface
                            .fromConfiguration(NetworkInterface_Yang.Data.from(interfaceData))),

                    (existingItem, item) -> {
                        item.getAdapter().ifPresent(adapter -> {
                            existingItem.setAdapter(adapter);
                        });
                    });
        });
    }

    @Nonnull
    public static Network create() {
        return new Network();
    }
}
