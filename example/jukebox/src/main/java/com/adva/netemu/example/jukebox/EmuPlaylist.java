package com.adva.netemu.example.jukebox;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import org.opendaylight.yangtools.yang.common.Uint32;

import com.adva.netemu.YangBinding;
import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


@YangListBound(context = NetEmuDefined.class, namespace = "http://example.com/ns/example-jukebox", value = "jukebox/playlist")
public class EmuPlaylist implements YangListBindable {

    @Nonnull
    private final EmuPlaylist_YangBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final String name;

    @Nonnull
    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(@Nullable final Object object) {
        return (object instanceof EmuPlaylist) && this.name.equals(((EmuPlaylist) object).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Nonnull
    private final AtomicReference<List<EmuSong>> songs = new AtomicReference<>();

    @Nonnull
    public List<EmuSong> songs() {
        return List.copyOf(this.songs.get());
    }

    private EmuPlaylist(@Nonnull final String name) {
        this.name = name;
        this.yangBinding = EmuPlaylist_YangBinding.withKey(EmuPlaylist_Yang.ListKey.from(name))

                .appliesConfigurationDataUsing(data -> {
                    this.songs.set(data.streamSong()
                            .map(playlistSongData -> StreamEx.of(EmuJukebox.songs())
                                    .findFirst(song -> song.getYangListBinding()
                                            .map(binding -> binding.getIid().equals(playlistSongData.getId()))
                                            .orElse(false))

                                    .orElseThrow(() -> new NoSuchElementException(String.format("No such song: %s",
                                            playlistSongData.getId()))))

                            .toImmutableList());

                }).providesOperationalDataUsing(builder -> builder
                        .setName(name)
                        .setSong(EntryStream.of(this.songs.get()), (songWithIndex, playlistSongBuilder) -> playlistSongBuilder
                                .setIndex(Uint32.valueOf(songWithIndex.getKey()))
                                .setId(songWithIndex.getValue().getYangListBinding().map(YangBinding::getIid))));
    }

    @Nonnull
    public static EmuPlaylist fromConfiguration(@Nonnull final EmuPlaylist_Yang.Data data) {
        @Nonnull final var instance = new EmuPlaylist(data.requireName());
        EmuPlaylist_Yang.bindingOf(instance).ifPresent(binding -> binding.applyConfigurationData(data));
        return instance;
    }
}
