package com.adva.netemu.example.jukebox;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import com.wrapper.spotify.model_objects.specification.Album;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


@YangListBound(
        context = NetEmuDefined.class,
        namespace = "http://example.com/ns/example-jukebox",
        value = "jukebox/library/artist/album")

public class EmuAlbum implements YangListBindable {

    @Nonnull
    private final EmuAlbum_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final Album spotifyAlbum;

    @Nonnull
    public String name() {
        return this.spotifyAlbum.getName();
    }

    @Nonnull
    private final Set<EmuSong> songs;

    @Nonnull
    public Set<EmuSong> songs() {
        return Set.copyOf(this.songs);
    }

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

    public static EmuAlbum fromSpotify(@Nonnull final Album spotifyAlbum) {
        return new EmuAlbum(spotifyAlbum);
    }
}
