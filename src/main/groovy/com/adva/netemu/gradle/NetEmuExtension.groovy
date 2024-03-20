package com.adva.netemu.gradle

import java.nio.file.Path
import java.nio.file.Paths

import javax.annotation.Nonnull
import javax.annotation.Nullable

import org.gradle.api.Action
// import org.gradle.util.ConfigureUtil /// Deprecated, & to be removed in Gradle 9.0


class NetEmuExtension {

    static class YangToSources {

        // String yangDir = "META-INF/yang"

        @Nonnull
        private String mdSalOutputDir = "generated/sources/mdsal"

        @Nonnull
        String getMdSalOutputDir() {
            return this.mdSalOutputDir
        }

        @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
        String generateMdSalDataClassesIn(@Nonnull final String outputDir) {
            this.mdSalOutputDir = outputDir
            return outputDir
        }

        private String netEmuOutputDir = "generated/sources/netemu"

        @Nonnull
        String getNetEmuOutputDir() {
            return this.netEmuOutputDir
        }

        @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
        String generateNetEmuContextClassesIn(@Nonnull final String outputDir) {
            this.netEmuOutputDir = outputDir
            return outputDir
        }
    }

    @Nonnull
    final YangToSources yangToSources = new YangToSources()

    /*
    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    YangToSources yangToSources(final Closure closure) {
        ConfigureUtil.configure(closure, this.yangToSources)
        return this.yangToSources
    }
    */

    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    YangToSources yangToSources(final Action<? extends YangToSources> action) {
        action.execute(this.yangToSources)
        return this.yangToSources
    }

    static class Pythonizer {

        @Nullable
        private File yangModelsFile = null

        @Nullable @SuppressWarnings("GroovyUnusedDeclaration")
        File getYangModelsFile() {
            return this.yangModelsFile
        }

        @Nullable
        File appendsYangModelsToFile(@Nullable final File file) {
            this.yangModelsFile = file
            return this.yangModelsFile
        }

        @Nullable
        File appendsYangModelsToFile(@Nullable final Path path) {
            return this.appendsYangModelsToFile(path.toFile())
        }

        @Nullable @SuppressWarnings("GroovyUnusedDeclaration")
        File appendsYangModelsToFile(@Nullable final String path) {
            return this.appendsYangModelsToFile(Paths.get(path))
        }
    }

    @Nonnull
    final Pythonizer pythonizer = new Pythonizer()

    /*
    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    Pythonizer pythonizer(final Closure closure) {
        ConfigureUtil.configure(closure, this.pythonizer)
        return this.pythonizer
    }
    */

    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    Pythonizer pythonizer(final Action<? extends Pythonizer> action) {
        action.execute(this.pythonizer)
        return this.pythonizer
    }

    @Nullable
    private String contextPackage = null

    @Nullable
    String getContextPackage() {
        return this.contextPackage
    }

    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    String definesContextInPackage(@Nonnull final String name) {
        this.contextPackage = name
        return name
    }
}
