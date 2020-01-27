package com.adva.netemu.testemu;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.Interfaces;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.InterfacesBuilder;

import com.adva.netemu.Owned;
import com.adva.netemu.YangData;
import com.adva.netemu.YangModeled;


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
                        .mapToObj((n) -> new TestInterface("test" + n))
                        .collect(ImmutableList.toImmutableList()));

        super.provideOperationalDataVia((builder) -> builder
                .setInterface(YangData.streamOf(this._interfaces)
                        .collect(Collectors.toList())));
    }

    @Override
    public void loadConfiguration(@Nonnull Interfaces data) {
        this._interfaces = Owned.by(this, data.nonnullInterface().stream()
                .map((intfData) -> {
                    final var intf = new TestInterface(intfData.key().getName());
                    intf.loadConfiguration(intfData);
                    return intf;

                }).collect(ImmutableList.toImmutableList()));
    }
}
