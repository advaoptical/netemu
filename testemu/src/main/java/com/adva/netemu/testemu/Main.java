package com.adva.netemu.testemu;

import javax.annotation.Nonnull;

import com.adva.netemu.NetEmu;
import com.adva.netemu.YangPool;
import com.adva.netemu.northbound.NetconfService;
import com.adva.netemu.northbound.PythonKernelService;
import com.adva.netemu.northbound.WebService;


public final class Main {

    public static void main(@Nonnull final String[] args) {

        @Nonnull final var pool = new YangPool("testemu", NetEmuDefined.YANG_MODULE_INFOS);

        @Nonnull final var device = pool.registerYangBindable(new TestDevice());
        // final var intf = device.getInterfaces().get(0);
        // intf._buildIid();

        /*
        device.setDataBroker(pool.getDataBroker());
        device.writeDataTo(LogicalDatastoreType.OPERATIONAL);
        */

        /*
        System.out.println(intf.toYangData().key());
        System.out.println(YangData.of(intf).get().getName());
        System.out.println(intf.getIidBuilder().build());
        */

        @Nonnull final var emu = new NetEmu(pool);
        emu.registerService(NetconfService.class);
        emu.registerService(PythonKernelService.class);
        emu.registerService(WebService.class);

        // emu.loadConfigurationFromXml();

        // pool.writeOperationalDataFrom(device);

        emu.start();
    }
}
