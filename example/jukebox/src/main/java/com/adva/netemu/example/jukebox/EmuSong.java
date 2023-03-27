package com.adva.netemu.example.jukebox;

import java.util.Optional;

import javax.annotation.Nonnull;

import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

import org.opendaylight.yangtools.yang.common.Uint32;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


/** A song in the jukebox library.

  * <p>Maps a Spotify track to a YANG {@code /example-jukebox:jukebox/library/artist/album/song} list item
  */
@YangListBound(
        context = NetEmuDefined.class,
        namespace = "http://example.com/ns/example-jukebox",
        value = "jukebox/library/artist/album/song")

public class EmuSong implements YangListBindable {

    /** YANG datastore binding for this jukebox song.
      */
    @Nonnull
    private final EmuSong_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    /** Underlying Spotify track.
      */
    @Nonnull
    private final TrackSimplified spotifyTrack;

    /** Returns the name of this jukebox song.

      * @return
            The name string
      */
    @Nonnull
    public String name() {
        return this.spotifyTrack.getName();
    }

    /** Returns the length of this jukebox song.

      * @return
            The song duration in seconds
      */
    public int seconds() {
        return this.spotifyTrack.getDurationMs() / 1000;
    }

    /** Creates event handlers for YANG datastore binding.

      * @param spotifyTrack
            Spotify track data
      */
    @SuppressWarnings({"UnstableApiUsage"})
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

    /** Creates a new jukebox song from given Spotify track.

      * @param spotifyTrack
            Spotify track data

      * @return
            A new instance
      */
    @Nonnull
    public static EmuSong fromSpotify(@Nonnull final TrackSimplified spotifyTrack) {
        return new EmuSong(spotifyTrack);
    }
}
