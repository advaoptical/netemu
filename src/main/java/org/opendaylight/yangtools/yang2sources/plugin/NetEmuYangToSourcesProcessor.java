package org.opendaylight.yangtools.yang2sources.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.maven.project.MavenProject;

import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

import org.opendaylight.mdsal.binding.maven.api.gen.plugin.NetEmuCodeGenerator;


public class NetEmuYangToSourcesProcessor extends YangToSourcesProcessor {

    private static final class YangProviderDummy extends YangProvider {

        @Override @SuppressWarnings({"UnstableApiUsage"})
        void addYangsToMetaInf(final MavenProject project, final Collection<YangTextSchemaSource> __) {}
    }

    @Nonnull
    private static final Path YANG_META_PATH = Paths.get("META-INF", "yang");

    @Nonnull
    private static final String CODE_GENERATOR_CLASS = NetEmuCodeGenerator.class.getName();

    public NetEmuYangToSourcesProcessor(
            @Nonnull final Path resourcesPath, @Nonnull final Path mdSalOutputPath, @Nonnull final String yangPackageName,
            @Nonnull final MavenProject project) {

        super(
                resourcesPath.resolve(YANG_META_PATH).resolve(yangPackageName).toFile(), /* excludedFiles = */ List.of(),
                List.of(new ConfigArg.CodeGeneratorArg(
                        CODE_GENERATOR_CLASS, mdSalOutputPath.toString(), resourcesPath.toString())),

                project, /* inspectDependencies = */ true, new YangProviderDummy());

        NetEmuCodeGenerator.YANG_PACKAGE_NAME.set(yangPackageName);
    }
}
