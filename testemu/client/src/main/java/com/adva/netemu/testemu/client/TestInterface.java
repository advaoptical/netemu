package com.adva.netemu.testemu.client;

import javax.annotation.Nonnull;

import com.adva.netemu.YangListModeled;


@YangListModeled(
        context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces/interface")

public class TestInterface extends TestInterface$YangListModel {

    private TestInterface(@Nonnull final TestInterface.Yang.ListKey key) {
        super(key);
    }

    private TestInterface(@Nonnull final TestInterface.Yang.Data data) {
        super(TestInterface.Yang.ListKey.from(data.getName().orElseThrow(() -> new IllegalArgumentException(String.format(
                "No 'name' leaf present in %s", data)))));

        TestInterface.Yang.bindingOf(this).ifPresent(binding -> binding.applyOperationalData(data));
    }

    @Nonnull
    public static TestInterface withKey(@Nonnull final TestInterface.Yang.ListKey key) {
        return new TestInterface(key);
    }

    @Nonnull
    public static TestInterface fromOperationalData(@Nonnull final TestInterface.Yang.Data data) {
        return new TestInterface(data);
    }
}
