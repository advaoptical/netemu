package com.adva.netemu.gradle

import javax.annotation.Nonnull

import org.gradle.api.Action
import org.gradle.util.ConfigureUtil


class NetEmuExtension {

    static class YangToSources {
        // String yangDir = "META-INF/yang"
        private String _outputDir = null

        public String getOutputDir() {
            return this._outputDir
        }

        public @Nonnull String generateIn(@Nonnull final String outputDir) {
            this._outputDir = outputDir
            return outputDir
        }
    }

    YangToSources yangToSources = new YangToSources()

    YangToSources yangToSources(final Closure closure) {
        ConfigureUtil.configure(closure, this.yangToSources)
        return this.yangToSources
    }

    YangToSources yangToSources(
            final Action<? extends YangToSources> action) {

        action.execute(this.yangToSources)
        return this.yangToSources
    }
}
