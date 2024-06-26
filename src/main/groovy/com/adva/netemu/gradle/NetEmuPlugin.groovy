package com.adva.netemu.gradle

import groovy.transform.NullCheck

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import javax.annotation.Nonnull
import javax.annotation.Nullable

import org.jetbrains.annotations.Contract

import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.FileUtils

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension

import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider
import org.opendaylight.yangtools.yang.binding.contract.Naming
import org.opendaylight.yangtools.yang2sources.plugin.NetEmuYangToSourcesProcessor

import org.opendaylight.mdsal.binding.java.api.generator.NetEmuFileGenerator


@SuppressWarnings(['GroovyUnusedDeclaration'])
class NetEmuPlugin implements Plugin<Project> {

    @Nonnull
    private final String CONTEXT_CLASS_NAME = 'NetEmuDefined'

    @Override @NullCheck @Contract(pure = false, value = 'null -> fail')
    void apply(@Nonnull final Project project) {
        @Nonnull final extension = project.extensions.create 'netEmu', NetEmuExtension

        @Nonnull final buildDir = project.layout.buildDirectory.asFile.get()

        /** Updated to use JavaPluginExtension instead of Gradle 8+ deprecated JavaPluginConvention -- according to:

            https://docs.gradle.org/8.4/userguide/upgrading_version_8.html#java_convention_deprecation
            https://docs.gradle.org/8.4/dsl/org.gradle.api.plugins.JavaPluginExtension.html
        */
        @Nonnull final javaExtension = project.extensions['java'] as JavaPluginExtension
        @Nonnull final javaSources = javaExtension.sourceSets.named 'main' get() java

        @Nonnull final mavenProject = new MavenProject()
        mavenProject.file = project.buildFile.canonicalFile
        mavenProject.build.directory = buildDir.toString()
        mavenProject.build.sourceDirectory = javaSources.getAsPath()

        @Nonnull final buildRoot = buildDir.toPath()
        @Nonnull final resourcesPath = buildRoot.resolve 'resources/main'

        @Nonnull final mdSalOutputPath = buildRoot.resolve extension.yangToSources.mdSalOutputDir
        @Nonnull final netEmuOutputPath = buildRoot.resolve extension.yangToSources.netEmuOutputDir

        javaSources.srcDirs += [mdSalOutputPath.toString(), netEmuOutputPath.toString()]

        @Nonnull final yangToSourcesTask = project.tasks.register 'yangToSources' get() doLast {

            @Nonnull final mavenGeneratedSourcesPath = buildRoot.resolve 'generated-sources'
            @Nonnull final javaFileGeneratorOutputPath = mavenGeneratedSourcesPath
                    .resolve NetEmuYangToSourcesProcessor.FILE_GENERATOR_IDENTIFIER

            @Nonnull final mavenGeneratedResourcesPath = buildRoot.resolve 'generated-resources'
            @Nonnull final serviceFileGeneratorOutputPath = mavenGeneratedResourcesPath
                    .resolve NetEmuYangToSourcesProcessor.FILE_GENERATOR_IDENTIFIER resolve NetEmuYangToSourcesProcessor
                            .SERVICE_META_PATH

            @Nonnull final netEmuMdSalMergingPath = netEmuOutputPath.resolve 'mdsal'
            Files.createDirectories netEmuMdSalMergingPath

            @Nonnull final serviceMetaPath = resourcesPath.resolve NetEmuYangToSourcesProcessor.SERVICE_META_PATH
            Files.createDirectories serviceMetaPath

            @Nonnull final yangBindingServiceFile = serviceMetaPath.resolve YangModelBindingProvider.class.name

            @Nonnull final yangMetaPath = resourcesPath.resolve NetEmuYangToSourcesProcessor.YANG_META_PATH
            yangMetaPath.eachDir {
                @Nonnull final yangFileNames = it.toFile() listFiles() collect { it.name }
                @Nonnull final yangPackageName = it.fileName.toString()

                new NetEmuYangToSourcesProcessor(resourcesPath, mdSalOutputPath, yangPackageName, mavenProject).execute()

                /// Check if any (changed) YANG modules were processed:
                if (NetEmuFileGenerator.YANG_MODULES.get() empty) {
                    return
                }

                // FileUtils.copyDirectoryStructure javaFileGeneratorOutputPath.toFile(), netEmuMdSalMergingPath.toFile()
                Files.walk javaFileGeneratorOutputPath forEach {
                    @Nonnull final destination = netEmuMdSalMergingPath.resolve (javaFileGeneratorOutputPath.relativize it)
                    if (Files.notExists destination) {
                        Files.createDirectories destination.parent
                        Files.copy it, destination
                    }
                }

                @Nonnull final yangPackageBindingServiceFile = serviceFileGeneratorOutputPath
                        .resolve YangModelBindingProvider.class.name toFile()

                if (yangPackageBindingServiceFile.exists()) {
                    yangBindingServiceFile.append "${yangPackageBindingServiceFile.text.trim()}\n"
                }

                // FileUtils.copyDirectoryStructure it.toFile(), yangMetaPath.toFile()
                @Nonnull final yangPackageMetaPath = it
                yangFileNames.forEach {
                    @Nonnull final destination = yangMetaPath.resolve it
                    if (Files.notExists destination) {
                        Files.copy yangPackageMetaPath.resolve(it), destination
                    }
                }

                @Nullable final pythonYangModelsFile = extension.pythonizer.yangModelsFile
                @Nullable pythonYangModelsFilePath = null
                if (pythonYangModelsFile != null) {
                    pythonYangModelsFilePath = project.rootDir.toPath().resolve pythonYangModelsFile.toPath()
                }

                // @Nonnull final yangPackageName = extension.contextPackage ?: project.group.toString()
                @Nonnull final yangPackagePath = netEmuOutputPath.resolve yangPackageName.replace('.', '/')

                @Nonnull final classFile = yangPackagePath.resolve "${CONTEXT_CLASS_NAME}.java"
                Files.createDirectories classFile.parent
                classFile.write """
                package ${yangPackageName};

                import java.util.Set;

                import javax.annotation.Nonnull;
                import javax.annotation.Nullable;

                import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

                \npublic final class ${CONTEXT_CLASS_NAME} {

                    private ${CONTEXT_CLASS_NAME}() {
                        throw new UnsupportedOperationException();
                    }

                    @Nonnull
                    public static final Set<YangModuleInfo> YANG_MODULE_INFOS = Set.of(
                            ${String.join ",\n\n", (NetEmuFileGenerator.YANG_MODULES.get() stream() map() {
                                "${Naming.getRootPackageName it.getQNameModule()}.\$YangModuleInfoImpl.getInstance()"

                            } collect())});

                    @SuppressWarnings({"unused"})
                    public static class CompileTime {

                        public static class YangModules {

                            public static class NorevLookup {

                                ${String.join """\n
                                """, (NetEmuFileGenerator.YANG_MODULES.get() stream() map() {
                                    @Nonnull final yangPackage = Naming.getRootPackageName it.getQNameModule()
                                    @Nonnull final yangNorevPackage = yangPackage.replaceAll(/\.[^.]+$/, '.norev')

                                    """
                                    public static class ${yangNorevPackage.replace '.', '$'} {

                                        @Nonnull
                                        public static final String NAME = "${it.getName()}";

                                        @Nonnull
                                        public static final String REVISION = "${it.getRevision()
                                                .map { it.toString() } orElse ''}";

                                        @Nonnull
                                        public static final String NAMESPACE = "${it.getNamespace() toString()}";

                                        @Nonnull
                                        public static final String PACKAGE = "${yangPackage}";

                                        @Nonnull
                                        public static final String RESOURCE = "/META-INF/yang/${yangPackageName}/${yangFileNames
                                            .find { it.matches "^${name}(@[0-9-]+)?\\.yang\$" } }";
                                    }
                                    """.trim()

                                } collect())}
                            }
                        }

                        public static class Pythonizer {

                            @Nullable
                            public static final String YANG_MODELS_FILE = ${pythonYangModelsFilePath ?
                                    "\"${pythonYangModelsFilePath}\"".replaceAll("\\\\", "/") : "null"};
                        }
                    }
                }
                """.stripIndent()
            }

            // FileUtils.deleteDirectory javaFileGeneratorOutputPath.toFile()
            // FileUtils.deleteDirectory mdSalOutputPath.toFile()

            if (Files.exists netEmuMdSalMergingPath) { /// ==> (changed) YANG models were processed.
                FileUtils.copyDirectoryStructure netEmuMdSalMergingPath.toFile(), mdSalOutputPath.toFile()
                FileUtils.deleteDirectory netEmuMdSalMergingPath.toFile()
            }

            /** Remove all non-ASCII characters from generated Java files -- which can occur in doc comments taken over from
                description statements in YANG modules -- because the Java compiler might not accept them:
            */
            Files.walk mdSalOutputPath filter { it.fileName.toString() endsWith '.java' } forEach {
                it.text = it.getText StandardCharsets.ISO_8859_1.toString() replaceAll "^\\P{ASCII}+", ""
            }

        } dependsOn(project.tasks.named 'processResources')

        project.tasks.named 'compileJava' get() doFirst {
            @Nullable final pythonYangModelsFile = extension.pythonizer.yangModelsFile
            if (pythonYangModelsFile != null) {
                @Nonnull final pythonYangModelsFilePath = project.rootDir.toPath() resolve pythonYangModelsFile.toPath()
                pythonYangModelsFilePath.text = ""
            }

        } dependsOn yangToSourcesTask
    }
}
