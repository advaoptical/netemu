package com.adva.netemu.example.jukebox;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Jooby;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.model_objects.specification.SavedAlbum;

import com.adva.netemu.NetEmu;
import com.adva.netemu.northbound.NetconfService;
import com.adva.netemu.northbound.RestconfService;


/** Main entry-point class for EMU-Jukebox.
  */
public class Main {

    /** Default logger for main class.
      */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /** Spotify Web API accessor.
      */
    @Nonnull
    private static final SpotifyApi SPOTIFY = new SpotifyApi.Builder()
            .setClientId("242fc96ffba444f298e99c14469e5027")
            .setClientSecret("dba0db6c64054425bab065385b25448f")
            .setRedirectUri(SpotifyHttpManager.makeUri("http://127.0.0.1:8080/spotify-authorization-callback"))
            .build();

    /** Minimal local webserver for receiving an authorization code from Spotify.
      */
    private static class SpotifyAuthorizationCallback extends Jooby {

        /** Defines the GET request handler for Spotify's callback, and continues the authentication process.

          * After successful authentication, the actual jukebox is started
          */
        private SpotifyAuthorizationCallback() {

            this.get("/spotify-authorization-callback", context -> {
                runJukeboxAsyncUsingSpotify(context.query("code").value()).get();
                return "";
            });
        }
    }

    /** Main entry-point function for EMU-Jukebox.

      * <p>Obtains an authentication URI from {@link #SPOTIFY} and starts the local callback webserver, which
        handles the remainder of Spotify authentication and starts the actual jukebox

      * <p>The URI is printed as info logging output and can be copied from there into a browser
      */
    public static void main(@Nonnull final String[] args) throws ExecutionException, InterruptedException {

        SPOTIFY.authorizationCodeUri().scope("user-library-read").build().executeAsync().thenAcceptAsync(uri -> {
            LOG.info("Log in to Spotify: {}", uri);

            Jooby.runApp(new String[0], SpotifyAuthorizationCallback::new);

        }).get(); // Makes the JVM wait for the asynchronous operation chain to finish!
    }

    /** Handles the remaining Spotify authentication steps and starts the actual jukebox.

      * This is an asynchronous operation!

      * <p>Northbound is attached a {@link NetconfService} and a {@link RestconfService} to the jukebox

      * @param spotifyAuthorizationCode
            The code obtained from {@link #SPOTIFY}'s first authentication step

      * @return
            An asynchronous operation handler!
      */
    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private static CompletableFuture<Void> runJukeboxAsyncUsingSpotify(@Nonnull final String spotifyAuthorizationCode) {

        return SPOTIFY.authorizationCode(spotifyAuthorizationCode).build().executeAsync().thenComposeAsync(credentials -> {
            SPOTIFY.setAccessToken(credentials.getAccessToken());
            SPOTIFY.setRefreshToken(credentials.getRefreshToken());
            return SPOTIFY.getCurrentUsersSavedAlbums().build().executeAsync();

        }).thenComposeAsync(savedSpotifyAlbums -> {
            @Nonnull final var jukeboxEmu = NetEmu.withId("example-jukebox")
                    .fromYangModuleInfos(NetEmuDefined.YANG_MODULE_INFOS);

            @Nonnull final var jukeboxYangPool = jukeboxEmu.getYangPool();
            jukeboxYangPool.registerYangBindable(EmuJukebox.createSingletonUsingSpotify(SPOTIFY,
                    StreamEx.of(savedSpotifyAlbums.getItems()).map(SavedAlbum::getAlbum).toImmutableSet()));

            return jukeboxYangPool.updateConfigurationData().thenComposeAsync(ignoredUpdateInfos -> jukeboxEmu
                    .loadConfiguration()
                    .thenAcceptAsync(ignoredCommitInfos -> {
                        jukeboxEmu.registerService(NetconfService.class)
                                .setStartingPort(17830);

                        jukeboxEmu.registerService(RestconfService.class)
                                .setPort(17888);

                        jukeboxEmu.start();
                    }));
        });
    }
}
