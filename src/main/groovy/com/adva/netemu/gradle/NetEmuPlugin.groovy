package com.adva.netemu.gradle

import com.google.common.io.Files
import org.apache.maven.project.MavenProject

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.plugins.JavaPluginConvention

import org.opendaylight.yangtools.yang2sources.plugin.
        NetEmuYangToSourcesProcessor

import java.util.stream.Collectors

import static org.opendaylight.mdsal.binding.maven.api.gen.plugin.
        NetEmuCodeGenerator.YANG_MODULE_PACKAGES


class NetEmuPlugin implements Plugin<Project> {

    @Override
    void apply(final Project prj) {
        final ext = prj.extensions.create 'netEmu', NetEmuExtension

        final mavenPrj = new MavenProject()
        mavenPrj.file = prj.buildFile.canonicalFile

        final task = prj.tasks.create('yangToSources').doFirst {
            final buildRoot = prj.buildDir.toPath()

            final resources = buildRoot.resolve "resources/main"
            final output = buildRoot.resolve ext.yangToSources.outputDir

            final outputDir = output.toString()
            final proc = new NetEmuYangToSourcesProcessor(
                    resources, outputDir, mavenPrj)

            proc.execute()

            final packageName = prj.group.toString()
            final packagePath = output.resolve packageName.replace('.', '/')

            final className = "YangToSources"
            final classFile = packagePath.resolve className + ".java"
            Files.createParentDirs classFile.toFile()
            classFile.write """
            package ${packageName};

            import com.google.common.collect.ImmutableSet;

            import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

            \nclass ${className} {

                public static
                final ImmutableSet<YangModuleInfo> YANG_MODULE_INFOS =
                        ImmutableSet.of(${String.join",\n\n",
                                YANG_MODULE_PACKAGES.get().stream().map() {
                                    it + ".\$YangModuleInfoImpl.getInstance()"
                                }.collect()});
            }
            """.stripIndent()

            final javaCon =
                    prj.convention.plugins['java'] as JavaPluginConvention

            javaCon.sourceSets['main'].java.srcDirs += outputDir
        }

        task.dependsOn prj.tasks['processResources']
        prj.tasks['compileJava'].dependsOn task
    }
}
