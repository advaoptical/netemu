package com.adva.netemu.example.jukebox;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.Album;

import org.opendaylight.yangtools.yang.common.Uint32;

import com.adva.netemu.YangBindable;
import com.adva.netemu.YangBinding;
import com.adva.netemu.YangBound;


@YangBound(context = NetEmuDefined.class, namespace = "http://example.com/ns/example-jukebox", value = "jukebox")
public class EmuJukebox implements YangBindable {

    @Nonnull
    private static Logger LOG = LoggerFactory.getLogger(EmuJukebox.class);

    @Nonnull
    private final EmuJukebox_YangBinding yangBinding = new EmuJukebox_YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nullable
    private static EmuJukebox INSTANCE = null;

    @Nonnull
    private final EmuLibrary library;

    @Nonnull
    public static Set<EmuArtist> artists() {
        return INSTANCE.library.artists();
    }

    @Nonnull
    public static Set<EmuAlbum> albums() {
        return INSTANCE.library.albums();
    }

    @Nonnull
    public static Set<EmuSong> songs() {
        return INSTANCE.library.songs();
    }

    @Nonnull
    private final Set<EmuPlaylist> playlists = Collections.synchronizedSet(new HashSet<>());

    /** Time gap between songs in seconds.
      */
    private double playerGap = 0.0;

    private EmuJukebox(@Nonnull final SpotifyApi spotify, @Nonnull final Collection<Album> spotifyAlbums) {
        this.library = this.yangBinding.registerChild(EmuLibrary.fromSpotifyAlbums(spotifyAlbums));

        this.yangBinding.appliesConfigurationDataUsing(data -> {
            data.getPlayer().flatMap(player -> Optional.ofNullable(player.getGap())).ifPresent(gap -> {
                this.playerGap = gap.doubleValue() / 10.0;
                LOG.info("Player gap set to {} seconds", this.playerGap);
            });

            @Nonnull final var playlists = data.streamPlaylist()
                    .map(playlistData -> EmuPlaylist.fromConfiguration(EmuPlaylist_Yang.Data.of(playlistData)))
                    .toImmutableSet();

            if (playlists.size() > 0) {
                this.playlists.addAll(playlists);
                LOG.info("Added {} playlists", playlists.size());
            }

        }).providesConfigurationDataUsing(builder -> builder
                .setLibrary(EmuLibrary_Yang.configurationDataFrom(this.library))

        ).providesOperationalDataUsing(builder -> builder
                .setLibrary(EmuLibrary_Yang.operationalDataFrom(this.library))
                .setPlaylist(EmuPlaylist_Yang.listOperationalDataFrom(this.playlists))

                .setPlayer(playerBuilder -> playerBuilder
                        .setGap(BigDecimal.valueOf(this.playerGap * 10.0))));
    }

    @Nonnull
    public static EmuJukebox createSingletonUsingSpotify(
            @Nonnull final SpotifyApi spotify, @Nonnull final Collection<Album> albums) {

        if (INSTANCE != null) {
            throw new RuntimeException(String.format("Singleton instance of %s was already created.", EmuJukebox.class));
        }

        INSTANCE = new EmuJukebox(spotify, albums);
        return INSTANCE;
    }

    public CompletableFuture<Void> play(@Nonnull final String playlistName) {
        return CompletableFuture.runAsync(() -> {
            for (@Nonnull final var song : StreamEx.of(this.playlists).findFirst(playlist -> playlist.name().equals(playlistName))
                    .orElseThrow(() -> new NoSuchElementException(String.format("No playlist named %s", playlistName)))
                    .songs()) {

                final int seconds = song.seconds();
                LOG.info("Playing song: {} ... Duration: {}:{}", song.name(), seconds / 60, seconds % 60);
                LOG.info("Waiting {} seconds", this.playerGap);
            }
        });
    }
}
