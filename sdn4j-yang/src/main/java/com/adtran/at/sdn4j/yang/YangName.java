package com.adtran.at.sdn4j.yang;

import java.util.function.Predicate;
import lombok.NonNull;
import one.util.streamex.StreamEx;
import org.checkerframework.checker.index.qual.NonNegative;

/// A YANG identifier name (usually in `kebab-case`), as defined in
/// [RFC 7950 §6.2](https://www.rfc-editor.org/rfc/rfc7950#section-6.2).
///
/// YANG names match the pattern `[A-Za-z]([-0-9A-Za-z]*[0-9A-Za-z])?` — starting with a letter,
/// & containing only letters, digits, and hyphens.
///
/// Extends {@link CharSequence} to be directly usable in Java string-related APIs.
///
public interface YangName extends CharSequence, Comparable<YangName> {

    /// Convert this YANG kebab-case name to its Java `lowerCamelCase` equivalent.
    ///
    /// For example, `admin-status` becomes `adminStatus`.
    ///
    default @NonNull String toJavaName() {
        final var parts = this.get().split("-");

        return parts[0] + StreamEx.of(parts, 1, parts.length)
                .filter(Predicate.not(String::isEmpty))
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .joining();
    }

    @NonNull String get();

    /// {@inheritDoc}
    ///
    @Override
    default @NonNegative int length() {
        return this.get().length();
    }

    /// {@inheritDoc}
    ///
    @Override
    default char charAt(final @NonNegative int index) {
        return this.get().charAt(index);
    }

    /// {@inheritDoc}
    ///
    @Override
    default @NonNull CharSequence subSequence(final @NonNegative int start, final @NonNegative int end) {
        return this.get().subSequence(start, end);
    }

    /// Returns the kebab-case YANG identifier string.
    ///
    /// Implementing classes must override this to return the name string directly.
    ///
    @Override
    @NonNull String toString();

    /// Order names lexicographically.
    ///
    @Override
    default int compareTo(final @NonNull YangName other) {
        return this.get().compareTo(other.get());
    }
}
