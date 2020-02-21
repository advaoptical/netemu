package com.adva.netemu.gradle

import java.nio.file.Path
import java.nio.file.Paths

import javax.annotation.Nonnull
import javax.annotation.Nullable

import org.gradle.api.Action
import org.gradle.util.ConfigureUtil


class NetEmuExtension {

    static class YangToSources {

        // String yangDir = "META-INF/yang"

        @Nonnull
        private String outputDir = "generated/sources/mdsal"

        @Nonnull
        String getOutputDir() {
            return this.outputDir
        }

        @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
        String generateIn(@Nonnull final String outputDir) {
            this.outputDir = outputDir
            return outputDir
        }
    }

    @Nonnull
    final YangToSources yangToSources = new YangToSources()

    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    YangToSources yangToSources(final Closure closure) {
        ConfigureUtil.configure(closure, this.yangToSources)
        return this.yangToSources
    }

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
    final Pythonizer pythonizer = new Pythonizer();

    @Nonnull @SuppressWarnings("GroovyUnusedDeclaration")
    Pythonizer pythonizer(final Closure closure) {
        ConfigureUtil.configure(closure, this.pythonizer)
        return this.pythonizer
    }

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
