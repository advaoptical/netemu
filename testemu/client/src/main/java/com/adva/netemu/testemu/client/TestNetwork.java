package com.adva.netemu.testemu.client;

import java.util.List;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.adva.netemu.YangModeled;


@YangModeled(context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces")
public class TestNetwork extends TestNetwork$YangModel {

    @Nonnull
    public List<TestInterface> getInterfaces() {
        return this.getInterface()
                .map(intfDataList -> StreamEx.of(intfDataList).map(TestInterface.Yang.Data::of)
                        .map(TestInterface::fromOperationalData)
                        .toImmutableList())

                .orElse(List.of());
    }
}
