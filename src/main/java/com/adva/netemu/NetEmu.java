package com.adva.netemu;

// import org.graalvm.compiler.hotspot.stubs.StubUtil;

import com.google.common.collect.ImmutableSet;

import org.eclipse.jdt.annotation.NonNull;

import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.ConfigurationBuilder;


public final class NetEmu extends NetconfDeviceSimulator {

    @NonNull
    private final YangPool _pool;

    public YangPool getYangPool() {
        return this._pool;
    }

    public NetEmu(@NonNull final YangPool pool) {
        super(new ConfigurationBuilder()
                .setCapabilities(ImmutableSet.of(
                        "urn:ietf:params:netconf:base:1.0",
                        "urn:ietf:params:netconf:base:1.1"))

                .setModels(ImmutableSet.copyOf(pool.getModules()))
                .setRpcMapping(new NetconfRequest(pool.getYangContext()))
                .build());

        this._pool = pool;
    }
}
