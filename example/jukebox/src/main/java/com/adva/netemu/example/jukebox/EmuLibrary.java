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


@YangBound(context = NetEmuDefined.class, namespace = "http://example.com/ns/example-jukebox", value = "jukebox/library")
public class EmuLibrary implements YangBindable {

    @Nonnull
    private static Logger LOG = LoggerFactory.getLogger(EmuLibrary.class);

    @Nonnull
    private final EmuLibrary_YangBinding yangBinding = new EmuLibrary_YangBinding();

    @Nonnull @Override
    public Optional<YangBinding<?, ?>> getYangBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final Set<EmuArtist> artists;

    @Nonnull
    public Set<EmuArtist> artists() {
        return Set.copyOf(this.artists);
    }

    @Nonnull
    public Set<EmuAlbum> albums() {
        return StreamEx.of(this.artists).flatMap(artist -> artist.albums().stream()).toImmutableSet();
    }

    @Nonnull
    public Set<EmuSong> songs() {
        return StreamEx.of(this.albums()).flatMap(album -> album.songs().stream()).toImmutableSet();
    }

    private EmuLibrary(@Nonnull final Collection<Album> spotifyAlbums) {
        this.artists = this.yangBinding.registerChildren(StreamEx
                .of(StreamEx.of(spotifyAlbums).groupingBy(spotifyAlbum -> spotifyAlbum.getArtists()[0].getName()).values())
                .map(spotifyArtistAlbums -> EmuArtist.fromSpotify(
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

    @Nonnull
    public static EmuLibrary fromSpotifyAlbums(@Nonnull final Collection<Album> albums) {
        return new EmuLibrary(albums);
    }
}
