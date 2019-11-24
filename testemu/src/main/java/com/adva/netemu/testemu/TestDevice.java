package com.adva.netemu.testemu;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.adva.netemu.Owned;
import com.adva.netemu.YangData;
import com.google.common.collect.ImmutableList;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.Interfaces;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
        .ietf.interfaces.rev180220.InterfacesBuilder;

import com.adva.netemu.YangModeled;


public class TestDevice extends YangModeled<Interfaces, InterfacesBuilder> {

    private @Nonnull final ImmutableList<TestInterface> _interfaces;

    @Nonnull
    public ImmutableList<TestInterface> getInterfaces() {
        return this._interfaces;
    }

    public TestDevice(@Nonnegative final Integer intfCount) {
        this._interfaces = Owned.by(
                this, IntStream.range(0, intfCount)
                        .mapToObj((n) -> new TestInterface("test" + n))
                        .collect(ImmutableList.toImmutableList()));

        super.provideYangData((builder) -> builder
                .setInterface(YangData.streamOf(this._interfaces)
                        .collect(Collectors.toList())));
    }
}
