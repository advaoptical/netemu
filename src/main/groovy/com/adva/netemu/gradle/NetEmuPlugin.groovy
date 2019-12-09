package com.adva.netemu.gradle

import org.apache.maven.project.MavenProject

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.plugins.JavaPluginConvention

import org.opendaylight.yangtools.yang2sources.plugin.
        NetEmuYangToSourcesProcessor


class NetEmuPlugin implements Plugin<Project> {

    @Override
    void apply(final Project prj) {
        final def ext = prj.extensions.create('netEmu', NetEmuExtension)

        final def mavenPrj = new MavenProject()

        final def task = prj.tasks.create('yangToSources').doFirst {
            final def buildRoot = prj.buildDir.toPath()

            final def resources = buildRoot.resolve("resources/main")
            final def output = buildRoot.resolve(ext.yangToSources.outputDir)

            final def outputDir = output.toString()
            final def proc = new NetEmuYangToSourcesProcessor(
                    resources, outputDir, mavenPrj)

            proc.execute()
            final def javaCon =
                    prj.convention.plugins['java'] as JavaPluginConvention

            javaCon.sourceSets['main'].java.srcDirs += outputDir
        }

        task.dependsOn prj.tasks['processResources']
        prj.tasks['compileJava'].dependsOn task
    }
}
