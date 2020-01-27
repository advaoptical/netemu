package org.opendaylight.yangtools.yang2sources.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import org.apache.maven.project.MavenProject;

import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

import org.opendaylight.mdsal.binding.maven.api.gen.plugin
        .NetEmuCodeGenerator;


public class NetEmuYangToSourcesProcessor extends YangToSourcesProcessor {

    private static final class YangProviderDummy extends YangProvider {

        @Override
        void addYangsToMetaInf(
                final MavenProject project,
                final Collection<YangTextSchemaSource> __) {}
    }

    @Nonnull
    public static final Path YANG_PATH = Paths.get(
            "META-INF", "yang");

    @Nonnull
    public static final String CODE_GENERATOR_CLASS
            = NetEmuCodeGenerator.class.getName();

    public NetEmuYangToSourcesProcessor(
            @Nonnull final Path resources,
            @Nonnull final String outputDir,
            @Nonnull final MavenProject project) {

        super(
                resources.resolve(YANG_PATH).toFile(),
                ImmutableList.of(),
                ImmutableList.of(new ConfigArg.CodeGeneratorArg(
                        CODE_GENERATOR_CLASS,
                        outputDir, resources.toString())),

                project,
                true,
                new YangProviderDummy());
    }
}
