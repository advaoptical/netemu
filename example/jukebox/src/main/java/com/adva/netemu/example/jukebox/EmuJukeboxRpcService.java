package com.adva.netemu.example.jukebox;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import org.opendaylight.yang.gen.v1.http.example.com.ns.example.jukebox.rev160815.ExampleJukeboxService;
import org.opendaylight.yang.gen.v1.http.example.com.ns.example.jukebox.rev160815.PlayInput;
import org.opendaylight.yang.gen.v1.http.example.com.ns.example.jukebox.rev160815.PlayOutput;
import org.opendaylight.yang.gen.v1.http.example.com.ns.example.jukebox.rev160815.PlayOutputBuilder;


public class EmuJukeboxRpcService implements ExampleJukeboxService {

    @Nonnull
    private final EmuJukebox jukebox;

    private EmuJukeboxRpcService(@Nonnull final EmuJukebox jukebox) {
        this.jukebox = jukebox;
    }

    @Nonnull
    public static EmuJukeboxRpcService forJukebox(@Nonnull final EmuJukebox jukebox) {
        return new EmuJukeboxRpcService(jukebox);
    }

    @Nonnull @Override
    public ListenableFuture<RpcResult<PlayOutput>> play(@Nonnull final PlayInput inputData) {
        this.jukebox.play(inputData.getPlaylist());
        return RpcResultBuilder.success(new PlayOutputBuilder().build()).buildFuture();
    }
}
