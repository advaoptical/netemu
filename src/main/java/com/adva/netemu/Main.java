package com.adva.netemu;

// import org.graalvm.compiler.hotspot.stubs.StubUtil;

import com.google.common.collect.ImmutableSet;

// import org.opendaylight.netconf.sal.connect.netconf.NetconfDevice;
// import org.opendaylight.netconf.sal.connect.netconf.NetconfDeviceBuilder;

import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.ConfigurationBuilder;

// import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
//     .ietf.yang.types.rev130715.*; // .`$YangModuleInfoImpl`;
//     as IetfYangTypes;

// import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
//     .ietf.interfaces.rev180220.*; // .`$YangModuleInfoImpl`;
//     as IetfInterfaces;

public final class Main {

    public static void main(final String[] args) {
        System.out.println(
                "NETEMU :: Copyright (C) 2019 ADVA Optical Networking");

/*
    val ncDevice = NetconfDeviceBuilder()
            .setReconnectOnSchemasChange(true)
            .build()

    ncDevice.start()
*/

        final var yangPool = new YangPool("netemu",
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.yang.types.rev130715.$YangModuleInfoImpl
                        .getInstance(),

                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.interfaces.rev180220.$YangModuleInfoImpl
                        .getInstance());

        final var yangModels = ImmutableSet.of(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.yang.types.rev130715.$YangModuleInfoImpl
                        .getInstance(),

                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                        .ietf.interfaces.rev180220.$YangModuleInfoImpl
                        .getInstance());

        final var ncCaps = ImmutableSet.of(
                "urn:ietf:params:netconf:base:1.0",
                "urn:ietf:params:netconf:base:1.1");

        final var ncConfig = new ConfigurationBuilder()
                .setCapabilities(ncCaps)
                .setModels(yangModels)
                .setRpcMapping(new NetconfRequest(yangPool.getYangContext()))
                .build();

        final var ncDevice = new NetconfDeviceSimulator(ncConfig);
        ncDevice.start();
    }
}
