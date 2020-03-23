package com.adva.netemu;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.xml.namespace.NamespaceContext;

import one.util.streamex.StreamEx;

import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;


public class YangNamespaceMap extends AbstractMap<String, URI> implements NamespaceContext {

    @Nonnull
    private final SchemaContext yangContext;

    @Nonnull
    public SchemaContext yangContext() {
        return this.yangContext;
    }

    private YangNamespaceMap(@Nonnull final SchemaContext yangContext) {
        this.yangContext = yangContext;
    }

    @Nonnull
    public static YangNamespaceMap from(@Nonnull final SchemaContext yangContext) {
        return new YangNamespaceMap(yangContext);
    }

    @Nonnull @Override
    public Set<Entry<String, URI>> entrySet() {
        return StreamEx.of(this.yangContext.getModules()).mapToEntry(Module::getPrefix, Module::getNamespace).toImmutableSet();
    }

    @Nonnull @Override
    public String getNamespaceURI(String prefix) {
        return StreamEx.of(this.yangContext.getModules()).findFirst(module -> module.getPrefix().equals(prefix))
                .map(module -> module.getNamespace().toString())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown YANG Module prefix: %s", prefix)));
    }

    @Nonnull @Override
    public String getPrefix(String namespace) {
        return this.yangContext.findModule(namespace).map(Module::getPrefix).orElseThrow(() -> new IllegalArgumentException(
                String.format("Unknown YANG Module namespace: %s", namespace)));
    }

    @Nonnull @Override
    public Iterator<String> getPrefixes(String namespace) {
        return StreamEx.of(this.getPrefix(namespace)).iterator();
    }
}
