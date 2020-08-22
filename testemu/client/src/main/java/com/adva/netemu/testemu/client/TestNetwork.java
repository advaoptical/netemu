package com.adva.netemu.testemu.client;

import java.util.List;

import javax.annotation.Nonnull;

import com.adva.netemu.YangModeled;


@YangModeled(context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces")
public class TestNetwork extends TestNetwork_YangModel {

    @Nonnull
    public List<TestInterface> interfaces() {
        return super.streamInterface().map(TestInterface_YangModel.Data::of).map(TestInterface::fromOperationalData)
                .toImmutableList();
    }
}
