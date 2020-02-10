package com.adva.netemu.testemu;

import com.adva.netemu.NetEmu;
import com.adva.netemu.YangPool;


public final class Main {

    public static void main(final String[] args) {

        final var pool = new YangPool("testemu",
                org.opendaylight.yang.gen.v1
                        .urn.ietf.params.xml.ns.yang
                        .ietf.yang.types.rev130715
                        .$YangModuleInfoImpl.getInstance(),

                org.opendaylight.yang.gen.v1
                        .urn.ietf.params.xml.ns.yang
                        .ietf.interfaces.rev180220
                        .$YangModuleInfoImpl.getInstance(),

                org.opendaylight.yang.gen.v1
                        .urn.ietf.params.xml.ns.yang
                        .iana._if.type.rev170119
                        .$YangModuleInfoImpl.getInstance());

        final var device = pool.registerYangBindable(new TestDevice());
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

        final var emu = new NetEmu(pool);
        emu.loadConfigurationFromXml();

        // pool.writeOperationalDataFrom(device);

        emu.start();
    }
}
