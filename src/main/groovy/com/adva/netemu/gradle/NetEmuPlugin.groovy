package com.adva.netemu.gradle

import javax.annotation.Nonnull
import javax.annotation.Nullable

import com.google.common.io.Files
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.FileUtils

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

import org.opendaylight.yangtools.yang2sources.plugin.NetEmuYangToSourcesProcessor

import org.opendaylight.mdsal.binding.spec.naming.BindingMapping

import org.opendaylight.mdsal.binding.maven.api.gen.plugin.NetEmuCodeGenerator


@SuppressWarnings("GroovyUnusedDeclaration")
class NetEmuPlugin implements Plugin<Project> {

    @Nonnull
    private final String CONTEXT_CLASS_NAME = "NetEmuDefined"

    @Override
    void apply(@Nonnull final Project project) {
        @Nonnull final extension = project.extensions.create 'netEmu', NetEmuExtension

        @Nonnull final mavenProject = new MavenProject()
        mavenProject.file = project.buildFile.canonicalFile

        @Nonnull final yangToSourcesTask = project.tasks.create('yangToSources').doFirst {
            @Nonnull final buildRoot = project.buildDir.toPath()
            @Nonnull final resourcesPath = buildRoot.resolve "resources/main"
            @Nonnull final mdSalOutputPath = buildRoot.resolve extension.yangToSources.mdSalOutputDir
            @Nonnull final netEmuOutputPath = buildRoot.resolve extension.yangToSources.netEmuOutputDir

            @Nonnull final netEmuMdSalMergingPath = netEmuOutputPath.resolve "mdsal"
            Files.createParentDirs(netEmuMdSalMergingPath.toFile())

            @Nonnull final yangMetaPath = resourcesPath.resolve "META-INF/yang"
            yangMetaPath.eachDir {
                @Nonnull final yangFileNames = it.toFile().listFiles().collect { it.name }
                @Nonnull final yangPackageName = it.fileName.toString()

                new NetEmuYangToSourcesProcessor(resourcesPath, mdSalOutputPath, yangPackageName, mavenProject).execute()
                FileUtils.copyDirectoryStructure mdSalOutputPath.toFile(), netEmuMdSalMergingPath.toFile()

            @Nullable final pythonYangModelsFile = extension.pythonizer.yangModelsFile
            @Nullable pythonYangModelsFilePath = null
            if (pythonYangModelsFile != null) {
                pythonYangModelsFilePath = project.rootDir.toPath().resolve pythonYangModelsFile.toPath()
            }

            // @Nonnull final yangPackageName = extension.contextPackage ?: project.group.toString()
            @Nonnull final yangPackagePath = netEmuOutputPath.resolve yangPackageName.replace('.', '/')

            @Nonnull final classFile = yangPackagePath.resolve "${CONTEXT_CLASS_NAME}.java"
            Files.createParentDirs classFile.toFile()
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
                        ${String.join ",\n\n", NetEmuCodeGenerator.YANG_MODULES.get().stream().map() {
                            "${BindingMapping.getRootPackageName it.getQNameModule()}.\$YangModuleInfoImpl.getInstance()"

                        }.collect()});

                @SuppressWarnings({"unused"})
                public static class CompileTime {

                    public static class YangModules {

                        public static class NorevLookup {

                            ${String.join """\n
                            """, NetEmuCodeGenerator.YANG_MODULES.get().stream().map() {
                                @Nonnull final yangPackage = BindingMapping.getRootPackageName(it.getQNameModule())
                                @Nonnull final yangNorevPackage = yangPackage.replaceAll(/\.[^.]+$/, '.norev')

                                """
                                public static class ${yangNorevPackage.replace '.', '$'} {

                                    @Nonnull
                                    public static final String NAME = "${it.getName()}";

                                    @Nonnull
                                    public static final String REVISION = "${it.getRevision().map { it.toString() }.orElse ''}";

                                    @Nonnull
                                    public static final String NAMESPACE = "${it.getNamespace().toString()}";

                                    @Nonnull
                                    public static final String PACKAGE = "${yangPackage}";

                                    @Nonnull
                                    public static final String RESOURCE = "/META-INF/yang/${yangPackageName}/${yangFileNames.find {
                                        it.matches "^${name}(@[0-9-]+)?\\.yang\$"
                                    } }";
                                }
                                """.trim()

                            }.collect()}
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

                /*
                @Nonnull final javaConvention = project.convention.plugins['java'] as JavaPluginConvention
                javaConvention.sourceSets['main'].java.srcDirs += yangPackagePath.toString()
                */
            }

            FileUtils.deleteDirectory mdSalOutputPath.toFile()
            FileUtils.copyDirectoryStructure netEmuMdSalMergingPath.toFile(), mdSalOutputPath.toFile()
            FileUtils.deleteDirectory netEmuMdSalMergingPath.toFile()

            @Nonnull final javaConvention = project.convention.plugins['java'] as JavaPluginConvention
            javaConvention.sourceSets['main'].java.srcDirs += [mdSalOutputPath.toString(), netEmuOutputPath.toString()]

        }.dependsOn project.tasks['processResources']

        project.tasks['compileJava'].doFirst {
            @Nullable final pythonYangModelsFile = extension.pythonizer.yangModelsFile
            if (pythonYangModelsFile != null) {
                @Nonnull final pythonYangModelsFilePath = project.rootDir.toPath().resolve pythonYangModelsFile.toPath()
                pythonYangModelsFilePath.text = ""
            }

        }.dependsOn yangToSourcesTask
    }
}
