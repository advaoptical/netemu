package com.adva.netemu;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceListener;
import org.opendaylight.yangtools.yang.model.repo.api
        .SchemaSourceRepresentation;

import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo
        .TextToASTTransformer;

import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

import org.opendaylight.netconf.test.tool.schemacache.SchemaSourceCache;


public class YangPool {

    private static Logger LOG = LoggerFactory.getLogger(YangPool.class);

    @NonNull
    private final ImmutableSet<YangModuleInfo> _modules;

    public ImmutableSet<YangModuleInfo> getModules() {
        return this._modules;
    }

    @NonNull
    private final SchemaContext _context;

    public SchemaContext getYangContext() {
        return this._context;
    }

    public YangPool(
            @NonNull final String id, final YangModuleInfo... modules) {

        this._modules = ImmutableSet.copyOf(modules);

        /*  The following is adapted from
            org.opendaylight.netconf.test.tool.NetconfDeviceSimulator
                    ::parseSchemasToModuleCapabilities
        */

        final var repo = new SharedSchemaRepository("netemu-" + id);
        repo.registerSchemaSourceListener(
                TextToASTTransformer.create(repo, repo));

        final Set<SourceIdentifier> sources = Sets.newHashSet();
        repo.registerSchemaSourceListener(new SchemaSourceListener() {

            @Override
            public void schemaSourceEncountered(
                    final SchemaSourceRepresentation __) {
            }

            @Override
            public void schemaSourceRegistered(
                    final Iterable<PotentialSchemaSource<?>> source) {

                for (final var s : source) {
                    sources.add(s.getSourceIdentifier());
                }
            }

            @Override
            public void schemaSourceUnregistered(
                    final PotentialSchemaSource<?> __) {
            }
        });

        repo.registerSchemaSourceListener(new SchemaSourceCache<>(
                repo, YangTextSchemaSource.class, this._modules));

        try {
            this._context = repo.createEffectiveModelContextFactory()
                    .createEffectiveModelContext(sources).get();

        } catch (
                final
                InterruptedException |
                ExecutionException e) {

            e.printStackTrace();
            LOG.error("Parsing YANG modules failed!");
            throw new RuntimeException(
                    "Cannot continue without YANG context!", e);
        }
    }
}
