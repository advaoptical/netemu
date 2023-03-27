package com.adva.netemu.example.jukebox;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


/** An artist in the jukebox library.

  * <p>Maps a Spotify artist and selected albums to a YANG {@code /example-jukebox:jukebox/library/artist} list item
  */
@YangListBound(
        context = NetEmuDefined.class,
        namespace = "http://example.com/ns/example-jukebox",
        value = "jukebox/library/artist")

public class EmuArtist implements YangListBindable {

    /** YANG datastore binding for this jukebox artist.
     */
    @Nonnull
    private final EmuArtist_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    /** Underlying Spotify artist.
     */
    @Nonnull
    private final ArtistSimplified spotifyArtist;

    @Nonnull
    public String name() {
        return this.spotifyArtist.getName();
    }

    /** Available albums of this jukebox artist.
     */
    @Nonnull
    private final Set<EmuAlbum> albums;

    /** Returns available albums of this jukebox artist.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<EmuAlbum> albums() {
        return Set.copyOf(this.albums);
    }

    /** Returns all songs of the available albums of this jukebox artist.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<EmuSong> songs() {
        return StreamEx.of(this.albums).flatMap(album -> album.songs().stream()).toImmutableSet();
    }

    /** Creates event handlers for YANG datastore binding and creates jukebox albums from given Spotify artist and albums.

      * @param spotifyArtist
            Spotify artist data

      * @param spotifyArtistAlbums
            Spotify albums data
      */
    private EmuArtist(@Nonnull final ArtistSimplified spotifyArtist, @Nonnull final Collection<Album> spotifyArtistAlbums) {
        this.spotifyArtist = spotifyArtist;

        this.yangBinding = EmuArtist_YangBinding.withKey(EmuArtist_Yang.ListKey.from(this.name()));
        this.albums = this.yangBinding.registerChildren(StreamEx.of(spotifyArtistAlbums).map(EmuAlbum::fromSpotify)
                .toImmutableSet());

        this.yangBinding.providesConfigurationDataUsing(builder -> builder
                .setName(this.name())
                .setAlbum(EmuAlbum_Yang.listOperationalDataFrom(this.albums))

        ).providesOperationalDataUsing(builder -> builder
                .setName(this.name())
                .setAlbum(EmuAlbum_Yang.listOperationalDataFrom(this.albums)));
    }

    /** Creates a new jukebox artist from given Spotify artist and selection of artist's albums.

      * @param spotifyArtist
            Spotify artist data

      * @param spotifyArtistAlbums
            Spotify albums data

      * @return
            A new instance
      */
    @Nonnull
    public static EmuArtist fromSpotify(
            @Nonnull final ArtistSimplified spotifyArtist,
            @Nonnull final Collection<Album> spotifyArtistAlbums) {

        return new EmuArtist(spotifyArtist, spotifyArtistAlbums);
    }
}
