package com.adva.netemu.testemu.client;

import javax.annotation.Nonnull;

import com.adva.netemu.NetEmu;
import com.adva.netemu.YangPool;
import com.adva.netemu.northbound.PythonKernelService;
import com.adva.netemu.southbound.NetconfDriver;

public class Main {

    public static void main(@Nonnull final String[] args) {
        @Nonnull final var yangPool = new YangPool("testemu-client", NetEmuDefined.YANG_MODULE_INFOS);
        @Nonnull final var network = yangPool.registerYangBindable(new TestNetwork());

        @Nonnull final var emu = new NetEmu(yangPool);
        emu.registerService(PythonKernelService.class);

        network.attachTo(emu.registerDriver(NetconfDriver.class).newSessionFrom(new NetconfDriver.Settings()
                .setHost("localhost")
                .setPort(17830)
                .setUser("admin")
                .setPassword("CHGME.1a")));

        emu.start();
    }
}
