package com.adva.netemu.gradle

import com.google.common.io.Files
import org.apache.maven.project.MavenProject

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.plugins.JavaPluginConvention
import org.opendaylight.mdsal.binding.spec.naming.BindingMapping
import org.opendaylight.yangtools.yang2sources.plugin.
        NetEmuYangToSourcesProcessor

import static org.opendaylight.mdsal.binding.maven.api.gen.plugin.
        NetEmuCodeGenerator.YANG_MODULES


class NetEmuPlugin implements Plugin<Project> {

    final String CONTEXT_CLASS_NAME = "NetEmuDefined"

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

            final yang = resources.resolve "META-INF/yang"
            final yangFiles = yang.toFile().listFiles().collect { it.name }

            final packageName = ext.contextPackage ?: prj.group.toString()
            final packagePath = output.resolve packageName.replace('.', '/')

            final classFile = packagePath.resolve CONTEXT_CLASS_NAME + ".java"
            Files.createParentDirs classFile.toFile()
            classFile.write """
            package ${packageName};

            import java.util.Set;

            import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

            \npublic final class ${CONTEXT_CLASS_NAME} {

                private ${CONTEXT_CLASS_NAME}() {
                    throw new UnsupportedOperationException();
                }

                public static
                final Set<YangModuleInfo> YANG_MODULE_INFOS =
                        Set.of(${String.join",\n\n",
                                YANG_MODULES.get().stream().map() {
                                    final pkg = BindingMapping
                                            .getRootPackageName(
                                                    it.getQNameModule());

                                    "${pkg}.\$YangModuleInfoImpl" +
                                            ".getInstance()"

                                }.collect()});

                class CompileTime {

                    class YangModules {

                        class NorevLookup {

                            ${String.join """\n
                            """, YANG_MODULES.get().stream().map() {
                                final name = it.getName()
                                final revision = it.getRevision()
                                        .map { it.toString() }.orElse ''

                                final namespace = it.getNamespace().toString()
                                final pkg = BindingMapping.getRootPackageName(
                                        it.getQNameModule())

                                "class " + pkg
                                        .replaceAll(/\.[^.]+$/, '.norev')
                                        .replace('.', '$') + " {\n" +

                                "    final String NAME =\n" +
                                "            \"${name}\";\n\n" +

                                "    final String REVISION =\n" +
                                "            \"${revision}\";\n\n" +

                                "    final String NAMESPACE =\n" +
                                "            \"${namespace}\";\n\n" +

                                "    final String PACKAGE =\n" +
                                "            \"${pkg}\";\n\n" +

                                "    final String RESOURCE =\n" +
                                "            \"/META-INF/yang/" +

                                yangFiles.find {
                                    it.matches "^${name}(@[0-9-]+)?\\.yang\$"

                                } + "\";\n}"

                            }.collect()}
                        }
                    }
                }
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
