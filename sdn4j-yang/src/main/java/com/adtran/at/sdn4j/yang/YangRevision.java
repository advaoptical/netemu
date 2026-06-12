package com.adtran.at.sdn4j.yang;

import java.time.LocalDate;

import lombok.NonNull;

/// A YANG revision date, as defined in
/// [RFC 7950 §7.1.9](https://www.rfc-editor.org/rfc/rfc7950#section-7.1.9).
///
/// Revisions are ISO 8601 dates (`YYYY-MM-DD`) identifying a specific version of a YANG module.
///
public interface YangRevision extends Comparable<YangRevision> {

    /// Get the revision date.
    ///
    @NonNull LocalDate date();

    /// Returns the ISO 8601 string form of this revision, e.g. `2018-02-20`.
    ///
    /// Implementing classes must override this to return `this.date().toString()`.
    ///
    @Override
    @NonNull String toString();

    /// Order revisions chronologically by date.
    ///
    @Override
    default int compareTo(final @NonNull YangRevision other) {
        return this.date().compareTo(other.date());
    }
}
