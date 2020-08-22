package com.adva.netemu.testemu.client;

import javax.annotation.Nonnull;

import com.adva.netemu.YangListModeled;


@YangListModeled(
        context = NetEmuDefined.class, namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces", value = "interfaces/interface")

public class TestInterface extends TestInterface_YangModel {

    private TestInterface(@Nonnull final TestInterface_YangModel.ListKey key) {
        super(key);
    }

    private TestInterface(@Nonnull final TestInterface_YangModel.Data data) {
        super(TestInterface_YangModel.ListKey.from(data.getName().orElseThrow(() -> new IllegalArgumentException(String.format(
                "No 'name' leaf present in %s", data)))));

        TestInterface_YangModel.bindingOf(this).ifPresent(binding -> binding.applyOperationalData(data));
    }

    @Nonnull
    public static TestInterface withKey(@Nonnull final TestInterface_YangModel.ListKey key) {
        return new TestInterface(key);
    }

    @Nonnull
    public static TestInterface fromOperationalData(@Nonnull final TestInterface_YangModel.Data data) {
        return new TestInterface(data);
    }
}
