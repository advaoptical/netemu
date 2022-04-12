package org.opendaylight.mdsal.binding.maven.api.gen.plugin;

import java.io.File;
import java.io.IOException;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
// import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.maven.project.MavenProject;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.sonatype.plexus.build.incremental.BuildContext;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleLike;

import org.opendaylight.yangtools.yang2sources.spi.BasicCodeGenerator;
import org.opendaylight.yangtools.yang2sources.spi.BuildContextAware;
import org.opendaylight.yangtools.yang2sources.spi.MavenProjectAware;
import org.opendaylight.yangtools.yang2sources.spi.ModuleResourceResolver;


public class NetEmuCodeGenerator implements BasicCodeGenerator, BuildContextAware, MavenProjectAware {

    @Nonnull
    public static final AtomicReference<String> YANG_PACKAGE_NAME = new AtomicReference<>();

    @Nonnull
    public static final AtomicReference<Set<Module>> YANG_MODULES = new AtomicReference<>(Set.of());

    @Nonnull
    private final CodeGeneratorImpl _impl = new CodeGeneratorImpl();

    @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
    public Collection<File> generateSources(
            @Nonnull final EffectiveModelContext yangContext,
            @Nonnull final File outputDir,
            @Nonnull final Set<Module> yangModules,
            // @Nonnull final Function<Module, Optional<String>> resourcePathResolver)
            ModuleResourceResolver moduleResourcePathResolver)

            throws IOException {

        @Nonnull final var result = this._impl.generateSources(yangContext, outputDir, yangModules, new ModuleResourceResolver() {

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

        YANG_MODULES.set(Set.copyOf(yangModules));
        return result;
    }

    @Override
    public void setAdditionalConfig(@Nonnull final Map<String, String> config) {
        this._impl.setAdditionalConfig(config);
    }

    @Override
    public void setResourceBaseDir(@Nonnull final File resources) {
        this._impl.setResourceBaseDir(resources);
    }

    @Override
    public void setBuildContext(@Nonnull final BuildContext buildContext) {
        this._impl.setBuildContext(buildContext);
    }

    @Override
    public void setMavenProject(@Nonnull final MavenProject project) {
        this._impl.setMavenProject(project);
    }
}
