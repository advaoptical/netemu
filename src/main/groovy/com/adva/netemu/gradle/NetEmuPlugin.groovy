package com.adva.netemu.gradle

import javax.annotation.Nonnull

import com.google.common.io.Files
import org.apache.maven.project.MavenProject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

import org.opendaylight.yangtools.yang2sources.plugin.NetEmuYangToSourcesProcessor

import org.opendaylight.mdsal.binding.spec.naming.BindingMapping

import org.opendaylight.mdsal.binding.maven.api.gen.plugin.NetEmuCodeGenerator

import javax.annotation.Nullable


@SuppressWarnings("GroovyUnusedDeclaration")
class NetEmuPlugin implements Plugin<Project> {

    @Nonnull
    private final String CONTEXT_CLASS_NAME = "NetEmuDefined"

    @Override
    void apply(final Project project) {
        @Nonnull final extension = project.extensions.create 'netEmu', NetEmuExtension

        @Nonnull final mavenProject = new MavenProject()
        mavenProject.file = project.buildFile.canonicalFile

        @Nonnull final yangToSourcesTask = project.tasks.create('yangToSources').doFirst {
            @Nonnull final buildRoot = project.buildDir.toPath()

            @Nonnull final resources = buildRoot.resolve "resources/main"
            @Nonnull final output = buildRoot.resolve extension.yangToSources.outputDir

            @Nonnull final outputDir = output.toString()
            new NetEmuYangToSourcesProcessor(resources, outputDir, mavenProject).execute()

            @Nonnull final yang = resources.resolve "META-INF/yang"
            @Nonnull final yangFiles = yang.toFile().listFiles().collect { it.name }

            @Nullable final pythonYangModelsFile = extension.pythonizer.yangModelsFile
            @Nullable pythonYangModelsFilePath = null
            if (pythonYangModelsFile != null) {
                pythonYangModelsFilePath = project.rootDir.toPath().resolve pythonYangModelsFile.toPath()
            }

            @Nonnull final packageName = extension.contextPackage ?: project.group.toString()
            @Nonnull final packagePath = output.resolve packageName.replace('.', '/')

            @Nonnull final classFile = packagePath.resolve "${CONTEXT_CLASS_NAME}.java"
            Files.createParentDirs classFile.toFile()
            classFile.write """
            package ${packageName};

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
                        ${String.join",\n\n", NetEmuCodeGenerator.YANG_MODULES.get().stream().map() {
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
                                public static class ${yangNorevPackage.replace'.', '$'} {

                                    @Nonnull
                                    public static final String NAME = "${it.getName()}";

                                    @Nonnull
                                    public static final String REVISION = "${it.getRevision().map { it.toString() }.orElse ''}";

                                    @Nonnull
                                    public static final String NAMESPACE = "${it.getNamespace().toString()}";

                                    @Nonnull
                                    public static final String PACKAGE = "${yangPackage}";

                                    @Nonnull
                                    public static final String RESOURCE = "/META-INF/yang/${ yangFiles.find {
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

            @Nonnull final javaConvention = project.convention.plugins['java'] as JavaPluginConvention
            javaConvention.sourceSets['main'].java.srcDirs += outputDir

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
