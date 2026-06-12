package com.adtran.at.sdn4j.yang;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/// A YANG data element with async serialization and southbound access.
///
public interface YangElement extends YangObject {

    /// Get the YANG element's data in JSON format.
    ///
    /// @return A future JSON data object in mapping form – or empty Optional if the element is empty.
    ///
    @NonNull CompletableFuture<@NonNull Optional<@Unmodifiable Map<?, ?>>> yang$toJson();

    /// Get the YANG element's config data in JSON format.
    ///
    /// @return A future JSON data object in mapping form – or empty Optional if the element is empty.
    ///
    @NonNull CompletableFuture<@NonNull Optional<@Unmodifiable Map<?, ?>>> yang$configToJson();

    /// Pull the YANG element's data from its southbound interface(s).
    ///
    /// @return A future that completes when the pull operation is finished.
    ///
    @NonNull CompletableFuture<@Nullable Void> yang$southPull();
}
