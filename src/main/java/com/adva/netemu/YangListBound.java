package com.adva.netemu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import org.opendaylight.yangtools.yang.binding.Key;


@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
public @interface YangListBound {

    @Nonnull
    Class<?> context();

    @Nonnull
    String namespace();

    @Nonnull
    String value();

    class Collection<E extends YangListBindable> implements java.util.Collection<E> {

        @Nonnull
        private final YangBinding<?, ?> parentYangBinding;

        @Nonnull
        private final Set<E> items = Collections.synchronizedSet(new HashSet<>());

        public Collection(@Nonnull final YangBinding<?, ?> parentYangBinding) {
            this.parentYangBinding = parentYangBinding;
        }

        @Nonnull
        public E get(@Nonnull final Key<?> key) {
            return this.find(key).orElseThrow(() -> new  IllegalArgumentException(String.format("%s not in %s", key, this)));
        }

        @Nonnull
        public Optional<E> find(@Nonnull final Key<?> key) {
            return StreamEx.of(this.items)
                    .findFirst(item -> item.getYangListBinding().map(YangListBinding::getKey)
                            .orElseThrow(() -> new RuntimeException(String.format("%s has no YANG-Binding", item)))
                            .equals(key));
        }

        @Nonnull
        public Set<? extends Key<?>> keySet() {
            return StreamEx.of(this.items)
                    .map(item -> item.getYangListBinding().map(YangListBinding::getKey).orElseThrow(() -> new RuntimeException(
                            String.format("%s has no YANG-Binding", item))))

                    .toImmutableSet();
        }

        @Override @Nonnegative
        public int size() {
            return this.items.size();
        }

        @Override
        public boolean isEmpty() {
            return this.items.isEmpty();
        }

        @Override
        public boolean contains(@Nonnull final Object item) {
            return this.items.contains(item);
        }

        public boolean containsKey(@Nonnull final Key<?> key) {
            return this.find(key).isPresent();
        }

        @Override @Nonnull
        public Iterator<E> iterator() {
            return Set.copyOf(this.items).iterator();
        }

        @Override @Nonnull
        public Object[] toArray() {
            return this.items.toArray();
        }

        @Override @Nonnull
        public <T> T[] toArray(@Nonnull final T[] array) {
            return this.items.toArray(array);
        }

        @Override
        public boolean add(@Nonnull final E item) {
            @Nonnull final var key = item.getYangListBinding().map(YangListBinding::getKey).orElseThrow(() ->
                    new IllegalArgumentException(String.format("%s has no YANG-Binding", item)));

            if (this.containsKey(key)) {
                throw new IllegalArgumentException(String.format("%s already exists in %s", key, this));
            }

            if (!this.items.add(item)) {
                throw new AssertionError(String.format("%s was not added to internal Set<> of %s", item, this));
            }

            this.parentYangBinding.registerChild(item);
            return true;
        }

        public <I extends E>
        Optional<I> merge(@Nonnull final I item, @Nonnull final BiConsumer<E, I> merger) {
            @Nonnull final var existingItem = this.find(item.getYangListBinding().map(YangListBinding::getKey).orElseThrow(() ->
                    new IllegalArgumentException(String.format("%s has no YANG-Binding", item))));

            if (existingItem.isPresent()) {
                merger.accept(existingItem.get(), item);
                return Optional.empty();
            }

            this.add(item);
            return Optional.of(item);
        }

        @Override
        public boolean remove(@Nonnull final Object item) {
            if (!this.contains(item)) {
                throw new IllegalArgumentException(String.format("%s not in %s", item, this));
            }

            if (!this.items.remove(item)) {
                throw new AssertionError(String.format("%s was not removed from internal Set<> of %s", item, this));
            }

            return true;
        }

        /*
        public boolean removeKey(@Nonnull final Identifier<?> key) {

        }
        */

        @Override
        public boolean containsAll(@Nonnull final java.util.Collection<?> items) {
            return this.items.containsAll(items);
        }

        public boolean containsAllKeys(@Nonnull final java.util.Collection<?> keys) {
            return this.keySet().containsAll(keys);
        }

        @Override
        public boolean addAll(@Nonnull final java.util.Collection<? extends E> items) {
            for (@Nonnull final var item : items) {
                this.add(item);
            }

            return !items.isEmpty();
        }

        @Nonnull
        public <I extends E>
        List<I> mergeAll(@Nonnull final java.util.Collection<I> items, @Nonnull final BiConsumer<E, I> merger) {
            return this.mergeAll(items.stream(), merger);
        }

        @Nonnull
        public <I extends E>
        List<I> mergeAll(@Nonnull final Stream<I> items, @Nonnull final BiConsumer<E, I> merger) {
            return StreamEx.of(items).flatMap(item -> this.merge(item, merger).stream()).nonNull().toImmutableList();
        }

        @Override
        public boolean removeAll(@Nonnull final java.util.Collection<?> items) {
            return false;
        }

        @Override
        public boolean retainAll(@Nonnull final java.util.Collection<?> items) {
            return false;
        }

        @Override
        public void clear() {
            this.items.clear();
        }
    }
}
