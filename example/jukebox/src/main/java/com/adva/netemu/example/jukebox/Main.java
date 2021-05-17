package com.adva.netemu.example.jukebox;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Jooby;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.model_objects.specification.SavedAlbum;

import com.adva.netemu.NetEmu;
import com.adva.netemu.YangPool;
import com.adva.netemu.northbound.NetconfService;
import com.adva.netemu.northbound.RestconfService;


public class Main {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Nonnull
    private static final SpotifyApi SPOTIFY = new SpotifyApi.Builder()
            .setClientId("")
            .setClientSecret("")
            .setRedirectUri(SpotifyHttpManager.makeUri("http://127.0.0.1:8080/spotify-authorization-callback"))
            .build();

    private static class SpotifyAuthorizationCallback extends Jooby {

        private SpotifyAuthorizationCallback() {
            this.get("/spotify-authorization-callback", context -> {
                runJukeboxAsyncUsingSpotify(context.query("code").value()).get();
                return "";
            });
        }
    }

    private static CompletableFuture<Void> runJukeboxAsyncUsingSpotify(@Nonnull final String spotifyAuthorizationCode) {
        return SPOTIFY.authorizationCode(spotifyAuthorizationCode).build().executeAsync().thenComposeAsync(credentials -> {
            SPOTIFY.setAccessToken(credentials.getAccessToken());
            SPOTIFY.setRefreshToken(credentials.getRefreshToken());
            return SPOTIFY.getCurrentUsersSavedAlbums().build().executeAsync();

        }).thenComposeAsync(savedAlbums -> {
            @Nonnull final var jukeboxYangPool = new YangPool("example-jukebox", NetEmuDefined.YANG_MODULE_INFOS);
            jukeboxYangPool.registerYangBindable(EmuJukebox.createSingletonUsingSpotify(SPOTIFY,
                    StreamEx.of(savedAlbums.getItems()).map(SavedAlbum::getAlbum).toImmutableSet()));

            return jukeboxYangPool.updateConfigurationData().thenComposeAsync(ignoredUpdateInfos -> {

                @Nonnull final var jukeboxEmu = new NetEmu(jukeboxYangPool);
                return jukeboxEmu.loadConfiguration().thenAcceptAsync(ignoredCommitInfos -> {
                    jukeboxEmu.registerService(NetconfService.class, (NetconfService.Settings) new NetconfService.Settings()
                            .setStartingPort(17830));

                    jukeboxEmu.registerService(RestconfService.class, new RestconfService.Settings()
                            .setPort(17888));

                    jukeboxEmu.start();
                });
            });
        });
    }

    public static void main(@Nonnull final String[] args) throws ExecutionException, InterruptedException {

        SPOTIFY.authorizationCodeUri().scope("user-library-read").build().executeAsync().thenAcceptAsync(uri -> {
            LOG.info("Log in to Spotify: {}", uri);

            Jooby.runApp(new String[0], SpotifyAuthorizationCallback::new);

        }).get();
    }
}
