package com.adtran.at.sdn4j.yang;

import lombok.NonNull;

/// Any YANG entity that has a qualified name.
///
public interface YangObject {

    /// Get the YANG entity's qualified name.
    ///
    @NonNull YangQName yang$qName();

    /// Get the YANG entity's local name.
    ///
    default @NonNull YangName yang$name() {
        return this.yang$qName().name();
    }

    /// Get the YANG entity's namespace.
    ///
    default @NonNull YangNamespace yang$namespace() {
        return this.yang$qName().namespace();
    }

    /// Get the entity's qualified YANG module name.
    ///
    default @NonNull YangModuleQName yang$moduleQName() {
        return this.yang$qName().moduleQName();
    }

    /// Get the entity's YANG module name.
    ///
    default @NonNull YangName yang$moduleName() {
        return this.yang$qName().moduleName();
    }
}
