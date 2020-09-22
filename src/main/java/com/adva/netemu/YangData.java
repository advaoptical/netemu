package com.adva.netemu;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yangtools.yang.binding.DataObject;


public class YangData<Y extends DataObject> {

    @Nonnull
    private final static YangData<? extends DataObject> EMPTY = new YangData<>(null);

    @Nullable
    private final Y object;

    @Nonnull
    public Y get() {
        if (this.object == null) {
            throw new NullPointerException("Empty YangData<>!"
                    + " Check YangData::isPresent before YangData::get");
        }

        return this.object;
    }

    protected YangData(@Nullable final Y object) {
        this.object = object;
    }

    @Nonnull
    public static <Y extends DataObject> YangData<Y> of(@Nonnull final Y object) {
        return new YangData<>(object);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <Y extends DataObject> YangData<Y> empty() {
        return (YangData<Y>) EMPTY;
    }

    public boolean isEmpty() {
        return this.object == null;
    }

    public boolean isPresent() {
        return this.object != null;
    }

    @Nullable
    public Y orElse(@Nullable final Y object) {
        return (this.object != null) ? this.object : object;
    }

    @Nullable
    public Y orElseGet(@Nonnull final Supplier<Y> supplier) {
        return supplier.get();
    }

    public void ifPresent(@Nonnull final Consumer<Y> action) {
        Optional.ofNullable(this.object).ifPresent(action);
    }

    @Nonnull
    public <T> Optional<T> map(@Nonnull final Function<Y, T> mapper) {
        return Optional.ofNullable(this.object).map(mapper);
    }
}
