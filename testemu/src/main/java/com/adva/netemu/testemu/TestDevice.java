package com.adva.netemu.testemu;

import java.util.List;
import java.util.stream.IntStream;
import static java.util.stream.Collectors.toList;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import static com.google.common.collect.ImmutableList.toImmutableList;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.Interfaces;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.InterfacesBuilder;

import com.adva.netemu.Owned;
import com.adva.netemu.YangModeled;
import static com.adva.netemu.YangProviders.streamOperationalDataFrom;


public class TestDevice extends YangModeled<Interfaces, InterfacesBuilder> {

    @Nonnull
    private List<TestInterface> _interfaces;

    @Nonnull
    public ImmutableList<TestInterface> getInterfaces() {
        return ImmutableList.copyOf(this._interfaces);
    }

    public TestDevice(@Nonnegative final Integer intfCount) {
        this._interfaces = Owned.by(
                this, IntStream.range(0, intfCount)
                        .mapToObj((n) -> TestInterface.withName("test" + n))
                        .collect(toImmutableList()));

        super.providesOperationalDataUsing(builder -> builder
                .setInterface(streamOperationalDataFrom(this._interfaces)
                        .collect(toList())));
    }

    @Override
    public void applyConfigurationData(@Nonnull final Interfaces data) {
        this._interfaces = Owned.by(this, data.nonnullInterface().stream()
                .map(TestInterface::fromConfigurationData)
                .collect(toImmutableList()));
    }

    @Override
    public void applyOperationalData(@Nonnull final Interfaces data) {
        for (final var intfData: data.nonnullInterface()) {
            this._interfaces.stream()
                    .filter(intf -> intf.getKey().equals(intfData.key()))
                    .findFirst().ifPresent(
                            intf -> intf.applyOperationalData(intfData));
        }
    }
}
