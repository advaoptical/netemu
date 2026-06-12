package com.adtran.at.sdn4j.yang;

import java.util.NoSuchElementException;
import java.util.Optional;

import lombok.NonNull;

/// A qualified YANG module name – composed of name, optional revision, & namespace.
///
/// The string form is `name@revision` when a revision is present, or just `name` otherwise,
/// as used in the YANG module declaration:
///
/// ```yang
/// module ietf-interfaces {
///   namespace "urn:ietf:params:xml:ns:yang:ietf-interfaces";
///   revision 2018-02-20 { ... }
/// }
/// ```
///
public interface YangModuleQName extends Comparable<YangModuleQName> {

    /// Get the YANG module's name.
    ///
    @NonNull YangName name();

    /// Get the YANG module's namespace.
    ///
    @NonNull YangNamespace namespace();

    /// Get the YANG module's revision, if present.
    ///
    /// The revision is optional per [RFC 7950 §7.1.9](https://www.rfc-editor.org/rfc/rfc7950#section-7.1.9).
    ///
    @NonNull Optional<YangRevision> getRevision();

    default @NonNull YangRevision requireRevision() {
        return this.getRevision().orElseThrow(() -> new NoSuchElementException("No YANG module revision present."));
    }

    /// Returns the `name@revision` string form, or just `name` if no revision is present.
    ///
    /// Implementing classes must override this to return the qualified string directly.
    ///
    @Override
    @NonNull String toString();

    /// Order module names lexicographically by their string form.
    ///
    @Override
    default int compareTo(final @NonNull YangModuleQName other) {
        return this.toString().compareTo(other.toString());
    }
}
