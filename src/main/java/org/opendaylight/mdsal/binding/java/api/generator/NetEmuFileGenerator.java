package org.opendaylight.mdsal.binding.java.api.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.google.common.collect.Table;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import org.opendaylight.yangtools.plugin.generator.api.FileGeneratorException;
import org.opendaylight.yangtools.plugin.generator.api.GeneratedFile;
import org.opendaylight.yangtools.plugin.generator.api.GeneratedFilePath;
import org.opendaylight.yangtools.plugin.generator.api.GeneratedFileType;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleLike;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;

import org.opendaylight.yangtools.yang2sources.spi.BasicCodeGenerator;
import org.opendaylight.yangtools.yang2sources.spi.BuildContextAware;
import org.opendaylight.yangtools.yang2sources.spi.MavenProjectAware;
import org.opendaylight.yangtools.yang2sources.spi.ModuleResourceResolver;


public class NetEmuFileGenerator implements BasicCodeGenerator, BuildContextAware, MavenProjectAware {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(NetEmuFileGenerator.class);

    @Nonnull
    public static final AtomicReference<String> YANG_PACKAGE_NAME = new AtomicReference<>();

    @Nonnull
    public static final AtomicReference<Set<Module>> YANG_MODULES = new AtomicReference<>(Set.of());

    @Nonnull
    private static final JavaFileGenerator JAVA_FILE_GENERATOR = new JavaFileGeneratorFactory().newFileGenerator(Map.of());

    @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
    public Collection<File> generateSources(
            @Nonnull final EffectiveModelContext yangContext,
            @Nonnull final File outputDir,
            @Nonnull final Set<Module> yangModules,
            // @Nonnull final Function<Module, Optional<String>> resourcePathResolver)
            ModuleResourceResolver moduleResourcePathResolver)

            throws IOException {

        @Nonnull final Table<GeneratedFileType, GeneratedFilePath, GeneratedFile> generatedFileTable;
        try {
            generatedFileTable = JAVA_FILE_GENERATOR.generateFiles(yangContext, yangModules, new ModuleResourceResolver() {

                @Nonnull @Override
                public Optional<String> findModuleResourcePath(
                        @Nonnull final ModuleLike module, final Class<? extends SchemaSourceRepresentation> representation) {

                    return moduleResourcePathResolver.findModuleResourcePath(module, representation).map(pathString -> {
                        @Nonnull final var path = Paths.get(pathString);
                        return path.getParent().resolve(YANG_PACKAGE_NAME.get()).resolve(path.getFileName()).toString()
                                .replace('\\', '/');
                    });
                }
            });

        } catch (final FileGeneratorException e) {
            throw new RuntimeException(String.format("Failed generating MD-SAL/Java sources from %s", yangContext), e);
        }

        @Nonnull final var writtenFiles = new HashSet<File>();
        for (@Nonnull final var generatedFile : generatedFileTable.cellSet()) {
            @Nonnull final var file = Paths.get(generatedFile.getColumnKey().getPath()).toFile();
            LOG.info("Writing generated {}", file);
            writtenFiles.add(file);
        }

        YANG_MODULES.set(Set.copyOf(yangModules));
        return writtenFiles;
    }

    @Override
    public void setAdditionalConfig(Map<String, String> additionalConfiguration) {}

    @Override
    public void setResourceBaseDir(File resourceBaseDir) {}

    @Override
    public void setBuildContext(BuildContext buildContext) {}

    @Override
    public void setMavenProject(MavenProject project) {}
}
