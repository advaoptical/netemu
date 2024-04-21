package org.opendaylight.mdsal.binding.java.api.generator;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Table;

import org.opendaylight.yangtools.plugin.generator.api.FileGenerator;
import org.opendaylight.yangtools.plugin.generator.api.FileGeneratorException;
import org.opendaylight.yangtools.plugin.generator.api.GeneratedFile;
import org.opendaylight.yangtools.plugin.generator.api.GeneratedFilePath;
import org.opendaylight.yangtools.plugin.generator.api.GeneratedFileType;
import org.opendaylight.yangtools.plugin.generator.api.ModuleResourceResolver;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleLike;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;

import org.opendaylight.yangtools.yang2sources.plugin.NetEmuYangToSourcesProcessor;


/** Wraps MD-SAL's {@link JavaFileGenerator} for interop with {@link com.adva.netemu.gradle.NetEmuPlugin}.

    <p> Allows NetEMU's Gradle plugin to generate Java sources from multiple sets of YANG modules -- each located in a
    {@code META-INF/yang/<Java package>} sub-directory --& to further generate NetEMU-specific MD-SAL/Advanced Java sources.

    <p> It is exclusively instantiated by {@link NetEmuFileGeneratorFactory}, and its instances' life-cycle is managed by
    {@link org.opendaylight.yangtools.yang2sources.plugin.YangToSourcesProcessor} -- which {@link NetEmuYangToSourcesProcessor}
    is derived from. Therefore, additional input & output parameters are passed & returned via static fields
    {@link #YANG_PACKAGE_NAME} and {@link #YANG_MODULES}, respectively.
*/
@Slf4j @SuppressWarnings({"UnstableApiUsage"})
public class NetEmuFileGenerator implements FileGenerator {

    /** The Java package associated with the YANG modules to be processed by this class' next instance.
    */
    @NonNull
    public static final AtomicReference<String> YANG_PACKAGE_NAME = new AtomicReference<>();

    /** The parsed YANG modules to be further processed by {@link com.adva.netemu.gradle.NetEmuPlugin} after
        {@link #generateFiles} is finished.
    */
    @NonNull
    public static final AtomicReference<Set<Module>> YANG_MODULES = new AtomicReference<>(Set.of());

    /** The wrapped MD-SAL code generator instance -- which all instances of this class delegate their {@link #generateFiles}
        calls to, using a {@link ModuleResourceResolver} wrapper aware of current {@link #YANG_PACKAGE_NAME}.
    */
    @NonNull
    private static final JavaFileGenerator JAVA_FILE_GENERATOR = new JavaFileGeneratorFactory().newFileGenerator(Map.of());

    /** Generate MD-SAL Java sources from YANG modules located in the sub-directory of {@code META-INF/yang} specified by
        {@link #YANG_PACKAGE_NAME}.

        @param yangContext the pre-parsed YANG model context
        @param yangModules the pre-parsed YANG modules
        @param moduleResourcePathResolver the non-sub-directory-aware YANG module file path resolver

        @return an overview of all generated Java sources
    */
    @NonNull @Override
    public Table<GeneratedFileType, GeneratedFilePath, GeneratedFile> generateFiles(
            @NonNull final EffectiveModelContext yangContext,
            @NonNull final Set<Module> yangModules,
            @NonNull final ModuleResourceResolver moduleResourcePathResolver) throws FileGeneratorException {

        YANG_MODULES.set(Set.copyOf(yangModules));
        return JAVA_FILE_GENERATOR.generateFiles(yangContext, yangModules, new ModuleResourceResolver() {

            @NonNull @Override
            public Optional<String> findModuleResourcePath(
                    @NonNull final ModuleLike module,
                    @NonNull final Class<? extends SchemaSourceRepresentation> representation) {

                return moduleResourcePathResolver.findModuleResourcePath(module, representation).map(pathString -> {
                    @NonNull final var path = Paths.get(pathString);
                    return path.getParent().resolve(YANG_PACKAGE_NAME.get()).resolve(path.getFileName()).toString()
                            .replace('\\', '/');
                });
            }
        });
    }
}
