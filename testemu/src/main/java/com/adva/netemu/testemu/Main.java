package com.adva.netemu.testemu;

import javax.annotation.Nonnull;

import com.adva.netemu.NetEmu;
import com.adva.netemu.northbound.NetconfService;
// import com.adva.netemu.northbound.PythonKernelService;
import com.adva.netemu.northbound.RestconfService;
// import com.adva.netemu.northbound.WebService;


public final class Main {

    public static void main(@Nonnull final String[] args) {
        @Nonnull final var testEmu = NetEmu.withId("testemu").fromYangModuleInfos(NetEmuDefined.YANG_MODULE_INFOS);
        @Nonnull final var testYangPool = testEmu.getYangPool();

        @Nonnull final var device = testYangPool.registerYangBindable(new TestDevice());
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

        testEmu.registerService(NetconfService.class)
                .setStartingPort(17830);

        testEmu.registerService(RestconfService.class)
                .setPort(18080);

        // emu.registerService(PythonKernelService.class);
        // emu.registerService(WebService.class);

        // emu.loadConfigurationFromXml();

        // pool.writeOperationalDataFrom(device);

        testEmu.start();
    }
}
