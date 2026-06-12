package com.adtran.at.sdn4j.yang;

import lombok.NonNull;

/// A qualified YANG name – composed of a qualified YANG module name and a local name.
///
/// The string form is `module-name:local-name`, as used in JSON and gNMI path contexts.
///
public interface YangQName extends Comparable<YangQName> {

    /// Get the qualified YANG module name.
    ///
    @NonNull YangModuleQName moduleQName();

    /// Get the local name.
    ///
    @NonNull YangName name();

    /// Get the namespace, delegating to the module QName.
    ///
    default @NonNull YangNamespace namespace() {
        return this.moduleQName().namespace();
    }

    /// Get the module name, delegating to the module QName.
    ///
    default @NonNull YangName moduleName() {
        return this.moduleQName().name();
    }

    /// Get the `module-name:local-name` prefixed form.
    ///
    default @NonNull String prefixedName() {
        return this.moduleName().toString().concat(":").concat(this.name().toString());
    }

    /// Order qualified YANG names by their prefixed form.
    ///
    @Override
    default int compareTo(final @NonNull YangQName other) {
        return this.prefixedName().compareTo(other.prefixedName());
    }
}
