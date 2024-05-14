package com.adva.netemu.gradle

import javax.annotation.Nonnull

import org.gradle.api.UnknownDomainObjectException
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification


/** Spock test specification for {@link NetEmuPlugin}.
*/
class NetEmuPluginSpec extends Specification {

    @Nonnull
    def plugin = new NetEmuPlugin()

    @Nonnull
    def emptyProject = ProjectBuilder.builder().build()

    @Nonnull
    def javaProject = ProjectBuilder.builder().build()

    def setup() {
        this.javaProject.plugins.apply 'java'
    }

    def "Throws when applied to null project"() {
        when:
        this.plugin.apply null

        then:
        @Nonnull final e = thrown IllegalArgumentException
        e.message == "project cannot be null"
    }

    def "Throws when applied to project w/o pre-applied 'java' plugin"() {
        when:
        this.plugin.apply this.emptyProject

        then:
        @Nonnull final e = thrown UnknownDomainObjectException
        e.message.startsWith "Extension with name 'java' does not exist."
    }

    def "Applies to project w/pre-applied 'java' plugin"() {
        given:
        this.javaProject.hasProperty 'netEmu' is false

        when:
        this.plugin.apply this.javaProject

        then:
        this.javaProject.hasProperty 'netEmu'
    }

    def "Adds 'netEmu' extension to project"() {
        given:
        this.javaProject.extensions.findByName 'netEmu' is null

        when:
        this.plugin.apply this.javaProject

        then:
        (this.javaProject.extensions.findByName 'netEmu').class.superclass.is NetEmuExtension
    }

    def "Adds 'yangToSources' sub-extension to 'netEmu' extension"() {
        given:
        this.javaProject.extensions.findByName 'netEmu' is null

        when:
        this.plugin.apply this.javaProject

        and:
        @Nonnull final netEmuExtension = (this.javaProject.extensions.findByName 'netEmu') as NetEmuExtension

        then:
        netEmuExtension.yangToSources instanceof NetEmuExtension.YangToSources
    }

    def "Adds 'yangToSources' task to project"() {
        given: //noinspection ConfigurationAvoidance (which would prefer .named over .findByName -- leading to an exception)
        this.javaProject.tasks.findByName 'yangToSources' is null

        when:
        this.plugin.apply this.javaProject

        then:
        (this.javaProject.tasks.named 'yangToSources' get()).actions.size() == 1
    }
}
