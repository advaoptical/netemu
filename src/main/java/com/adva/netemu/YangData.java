package com.adva.netemu;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yangtools.yang.binding.DataObject;


public class YangData<Y extends DataObject> {

    @Nullable
    private final Y _object;

    @Nonnull
    public Y get() {
        if (this._object == null) {
            throw new NullPointerException("YangData is empty."
                    + " Check YangData::isPresent before YangData::get");
        }

        return this._object;
    }

    protected YangData(@Nullable final Y object) {
        this._object = object;
    }

    @Nonnull
    public static YangData<? extends DataObject> of(
            @Nonnull final DataObject object) {

        return new YangData<>(object);
    }

    @Nonnull
    public static YangData<? extends DataObject> empty() {
        return new YangData<>(null);
    }

    public boolean isEmpty() {
        return this._object == null;
    }

    public boolean isPresent() {
        return this._object != null;
    }

    public <T> Optional<T> map(@Nonnull final Function<Y, T> mapper) {
        return Optional.ofNullable(this._object).map(mapper);
    }
}
