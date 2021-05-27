package com.adva.netemu.example.jukebox;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wrapper.spotify.model_objects.specification.Album;

import org.opendaylight.yangtools.yang.common.Uint32;

import com.adva.netemu.YangBindable;
import com.adva.netemu.YangBinding;
import com.adva.netemu.YangBound;


/** The music library of the jukebox.

  * <p>Manages the YANG {@code /example-jukebox:jukebox/library} container
  */
@YangBound(context = NetEmuDefined.class, namespace = "http://example.com/ns/example-jukebox", value = "jukebox/library")
public class EmuLibrary implements YangBindable {

    /** Default logger for jukebox library class.
      */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(EmuLibrary.class);

    /** YANG datastore binding for this jukebox library.
     */
    @Nonnull
    private final EmuLibrary_YangBinding yangBinding = new EmuLibrary_YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this.yangBinding);
    }

    /** All artists of this jukebox library.
      */
    @Nonnull
    private final Set<EmuArtist> artists;

    /** Returns all artists of this jukebox library.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<EmuArtist> artists() {
        return Set.copyOf(this.artists);
    }

    /** Returns all albums of this jukebox library.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<EmuAlbum> albums() {
        return StreamEx.of(this.artists).flatMap(artist -> artist.albums().stream()).toImmutableSet();
    }

    /** Returns all songs of this jukebox library.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<EmuSong> songs() {
        return StreamEx.of(this.albums()).flatMap(album -> album.songs().stream()).toImmutableSet();
    }

    /** Creates event handlers for YANG datastore binding and creates jukebox artists from given Spotify albums.

      * @param spotifyAlbums
            Spotify albums data
      */
    @SuppressWarnings({"UnstableApiUsage"})
    private EmuLibrary(@Nonnull final Collection<Album> spotifyAlbums) {

        this.artists = this.yangBinding.registerChildren(StreamEx
                // Grouping by artist names instead of artist objects, because the latter are not properly comparable
                .of(StreamEx.of(spotifyAlbums).groupingBy(spotifyAlbum -> spotifyAlbum.getArtists()[0].getName())
                        .values()) // Therefore, name keys are discarded ...

                .map(spotifyArtistAlbums -> EmuArtist.fromSpotify(
                        // ... and artist objects are extracted again
                        spotifyArtistAlbums.get(0).getArtists()[0], spotifyArtistAlbums))

                .toImmutableSet());

        this.yangBinding.providesConfigurationDataUsing(builder -> builder
                .setArtist(EmuArtist_Yang.listConfigurationDataFrom(this.artists))

        ).providesOperationalDataUsing(builder -> builder
                .setArtist(EmuArtist_Yang.listOperationalDataFrom(this.artists))

                .setArtistCount(Uint32.valueOf(this.artists.size()))
                .setAlbumCount(Uint32.valueOf(EmuJukebox.albums().size()))
                .setSongCount(Uint32.valueOf(EmuJukebox.songs().size())));
    }

    /** Creates a new jukebox library from given Spotify albums.

      * @param spotifyAlbums
            Spotify albums data

      * @return
            A new instance
      */
    @Nonnull
    public static EmuLibrary fromSpotifyAlbums(@Nonnull final Collection<Album> spotifyAlbums) {
        return new EmuLibrary(spotifyAlbums);
    }
}
