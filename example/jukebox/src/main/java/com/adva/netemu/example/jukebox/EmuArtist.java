package com.adva.netemu.example.jukebox;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


@YangListBound(
        context = NetEmuDefined.class,
        namespace = "http://example.com/ns/example-jukebox",
        value = "jukebox/library/artist")

public class EmuArtist implements YangListBindable {

    @Nonnull
    private final EmuArtist_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final ArtistSimplified spotifyArtist;

    @Nonnull
    public String name() {
        return this.spotifyArtist.getName();
    }

    @Nonnull
    private final Set<EmuAlbum> albums;

    @Nonnull
    public Set<EmuAlbum> albums() {
        return Set.copyOf(this.albums);
    }

    @Nonnull
    public Set<EmuSong> songs() {
        return StreamEx.of(this.albums).flatMap(album -> album.songs().stream()).toImmutableSet();
    }

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

    @Nonnull
    public static EmuArtist fromSpotify(
            @Nonnull final ArtistSimplified spotifyArtist,
            @Nonnull final Collection<Album> spotifyArtistAlbums) {

        return new EmuArtist(spotifyArtist, spotifyArtistAlbums);
    }
}
