package com.adva.netemu;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yangtools.yang.binding.DataObject;


public class YangData<Y extends DataObject> {

    @Nonnull
    private final static YangData<? extends DataObject> EMPTY =
            new YangData<>(null);

    @Nullable
    private final Y _object;

    @Nonnull
    public Y get() {
        if (this._object == null) {
            throw new NullPointerException("Empty YangData<>!"
                    + " Check YangData::isPresent before YangData::get");
        }

        return this._object;
    }

    protected YangData(@Nullable final Y object) {
        this._object = object;
    }

    @Nonnull
    public static <Y extends DataObject> YangData<Y> of(
            @Nonnull final Y object) {

        return new YangData<>(object);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <Y extends DataObject> YangData<Y> empty() {
        return (YangData<Y>) EMPTY;
    }

    public boolean isEmpty() {
        return this._object == null;
    }

    public boolean isPresent() {
        return this._object != null;
    }

    public void ifPresent(@Nonnull final Consumer<Y> action) {
        Optional.ofNullable(this._object).ifPresent(action);
    }

    @Nonnull
    public <T> Optional<T> map(@Nonnull final Function<Y, T> mapper) {
        return Optional.ofNullable(this._object).map(mapper);
    }
}
