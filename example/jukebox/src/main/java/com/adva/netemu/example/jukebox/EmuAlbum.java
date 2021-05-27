package com.adva.netemu.example.jukebox;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.wrapper.spotify.model_objects.specification.Album;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


/** An album in the jukebox library.

  * <p>Maps a Spotify album to a YANG {@code /example-jukebox:jukebox/library/artist/album} list item
  */
@YangListBound(
        context = NetEmuDefined.class,
        namespace = "http://example.com/ns/example-jukebox",
        value = "jukebox/library/artist/album")

public class EmuAlbum implements YangListBindable {

    /** YANG datastore binding for this jukebox album.
      */
    @Nonnull
    private final EmuAlbum_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    /** Underlying Spotify album.
      */
    @Nonnull
    private final Album spotifyAlbum;

    /** Returns the name of this jukebox album.

      * @return
            The name string
      */
    @Nonnull
    public String name() {
        return this.spotifyAlbum.getName();
    }

    /** All songs of this jukebox album.
     */
    @Nonnull
    private final Set<EmuSong> songs;

    /** Returns all songs of this jukebox album.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<EmuSong> songs() {
        return Set.copyOf(this.songs);
    }

    /** Creates event handlers for YANG datastore binding and extracts songs from given Spotify album.

      * @param spotifyAlbum
            Spotify album data
      */
    private EmuAlbum(@Nonnull final Album spotifyAlbum) {
        this.spotifyAlbum = spotifyAlbum;

        this.yangBinding = EmuAlbum_YangBinding.withKey(EmuAlbum_Yang.ListKey.from(this.name()));
        this.songs = this.yangBinding.registerChildren(StreamEx.of(this.spotifyAlbum.getTracks().getItems())
                .map(EmuSong::fromSpotify)
                .toImmutableSet());

        this.yangBinding.providesConfigurationDataUsing(builder -> builder
                .setName(this.name())
                .setSong(EmuSong_Yang.listOperationalDataFrom(this.songs()))

        ).providesOperationalDataUsing(builder -> builder
                .setName(this.name())
                .setSong(EmuSong_Yang.listOperationalDataFrom(this.songs())));
    }

    /** Creates a new jukebox album from given Spotify album.

      * @param spotifyAlbum
            Spotify album data

      * @return
            A new instance
      */
    public static EmuAlbum fromSpotify(@Nonnull final Album spotifyAlbum) {
        return new EmuAlbum(spotifyAlbum);
    }
}
