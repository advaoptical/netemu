package com.adva.netemu.example.jukebox;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.wrapper.spotify.model_objects.specification.TrackSimplified;

import org.opendaylight.yangtools.yang.common.Uint32;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


@YangListBound(
        context = NetEmuDefined.class,
        namespace = "http://example.com/ns/example-jukebox",
        value = "jukebox/library/artist/album/song")

public class EmuSong implements YangListBindable {

    @Nonnull
    private final EmuSong_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final TrackSimplified spotifyTrack;

    @Nonnull
    public String name() {
        return this.spotifyTrack.getName();
    }

    public int seconds() {
        return this.spotifyTrack.getDurationMs() / 1000;
    }

    private EmuSong(@Nonnull final TrackSimplified spotifyTrack) {
        this.spotifyTrack = spotifyTrack;

        this.yangBinding = EmuSong_YangBinding.withKey(EmuSong_Yang.ListKey.from(this.name()))
                .providesConfigurationDataUsing(builder -> builder
                        .setName(this.name())
                        .setLength(Uint32.valueOf(this.spotifyTrack.getDurationMs() / 1000))
                        .setLocation(this.spotifyTrack.getHref()))

                .providesOperationalDataUsing(builder -> builder
                        .setName(this.name())
                        .setLength(Uint32.valueOf(this.spotifyTrack.getDurationMs() / 1000))
                        .setLocation(this.spotifyTrack.getHref()));
    }

    @Nonnull
    public static EmuSong fromSpotify(@Nonnull final TrackSimplified spotifyTrack) {
        return new EmuSong(spotifyTrack);
    }
}
