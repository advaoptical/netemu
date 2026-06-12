package com.adtran.at.sdn4j.yang;

import java.net.URI;

import lombok.NonNull;

/// A YANG namespace – identified by either URL or URN.
///
/// YANG namespaces are URIs as defined in
/// [RFC 7950 §7.1.3](https://www.rfc-editor.org/rfc/rfc7950#section-7.1.3).
/// They uniquely identify a YANG module and appear in XML encodings as the element namespace.
/// Common form examples:
///
/// - **URL**: `http://openconfig.net/yang/interfaces`
/// - **URN**: `urn:ietf:params:xml:ns:yang:ietf-interfaces`
///
public interface YangNamespace extends Comparable<YangNamespace> {

    /// Get the namespace as a {@link URI} instance.
    ///
    @NonNull URI uri();

    /// Returns `true` if this namespace is a URN (scheme is `urn`).
    ///
    default boolean isUrn() {
        return "urn".equalsIgnoreCase(this.uri().getScheme());
    }

    /// Returns `true` if this namespace is a URL (any scheme other than `urn`).
    ///
    default boolean isUrl() {
        return !this.isUrn();
    }

    /// Returns the URI string representation of this namespace.
    ///
    /// Implementing classes must override this to return {@code this.uri().toString()}.
    ///
    @Override
    @NonNull String toString();

    /// Order namespaces lexicographically by their URI string.
    ///
    @Override
    default int compareTo(final @NonNull YangNamespace other) {
        return this.uri().compareTo(other.uri());
    }
}
