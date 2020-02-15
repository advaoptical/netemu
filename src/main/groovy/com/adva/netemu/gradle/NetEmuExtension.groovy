package com.adva.netemu.gradle

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
