package emu.skeleton.renameIt;

import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import com.adva.netemu.NetEmu;
import com.adva.netemu.northbound.NetconfService;
import com.adva.netemu.northbound.RestconfService;

import emu.skeleton.renameIt.networkExample.NetEmuDefined;
import emu.skeleton.renameIt.networkExample.Network;


public class Main {
    public static void main(@Nonnull final String[] args) throws ExecutionException, InterruptedException {

        @Nonnull final var networkExampleEmu = NetEmu.withId("rename-this-network-example-id")
                .fromYangModuleInfos(NetEmuDefined.YANG_MODULE_INFOS);

        networkExampleEmu.loadConfiguration().thenAcceptAsync(ignoredCommitInfos -> {
            networkExampleEmu.getYangPool().registerYangBindable(Network.create());

            networkExampleEmu.registerService(NetconfService.class).setStartingPort(17830);
            networkExampleEmu.registerService(RestconfService.class).setPort(18080);
            networkExampleEmu.start();

        }).get();
    }
}