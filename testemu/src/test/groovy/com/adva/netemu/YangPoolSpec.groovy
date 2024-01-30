package com.adva.netemu

import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull

import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier

import org.opendaylight.netconf.api.xml.XmlNetconfConstants

import spock.lang.Specification

import com.adva.netemu.testemu.NetEmuDefined

import java.util.concurrent.ExecutionException


class YangPoolSpec extends Specification {

    @Nonnull
    def testYangPool = new YangPool('netemu-test', NetEmuDefined.YANG_MODULE_INFOS)

    def ".id() string equals construction parameter"() {
        expect:
        this.testYangPool.id() == 'netemu-test'
    }

    def "YANG .modules() set equals construction parameter"() {
        expect:
        this.testYangPool.modules == NetEmuDefined.YANG_MODULE_INFOS
    }

    def "YANG .modules() are immutable; #module can't be added"() {
        when:
        this.testYangPool.modules.add module

        then:
        thrown UnsupportedOperationException

        where:
        module << [NetEmuDefined.YANG_MODULE_INFOS.first(), null]
    }

    def ".getSource(#moduleName@#revisionString) yields according YANG module content"() {
        given:
        @Nonnull final identifier = new SourceIdentifier(moduleName, Revision.of(revisionString))
        @Nonnull final charset = StandardCharsets.US_ASCII

        when:
        @Nonnull final content = this.testYangPool.getSource identifier get() asCharSource charset read()

        then:
        content =~ /(?ms)^\s*module\s+${moduleName}\s+\{[\s\n]+.+\s+revision\s+${revisionString}\s+\{/

        where:
        moduleName        | revisionString
        'iana-if-type'    | '2017-01-19'
        'ietf-interfaces' | '2018-02-20'
        'ietf-yang-types' | '2013-07-15'
    }

    def ".getSource() throws when given YANG module not found"() {
        given:
        @Nonnull final identifier = new SourceIdentifier('unknown', Revision.of('2010-01-02'))

        when:
        this.testYangPool.getSource identifier get()

        then:
        final e = thrown ExecutionException
        e.cause instanceof NoSuchElementException
    }

    def ".getSource() throws when YANG module revision is missing"() {
        given:
        @Nonnull final identifier = new SourceIdentifier('ietf-interfaces')

        when:
        this.testYangPool.getSource identifier get()

        then:
        final e = thrown ExecutionException
        e.cause instanceof NoSuchElementException
    }

    def "YANG .modules from .effectiveModelContext match module infos from construction"() {
        given:
        @Nonnull final qNameModules = NetEmuDefined.YANG_MODULE_INFOS.collect { it.name.module } as Set

        when:
        @Nonnull final modules = this.testYangPool.effectiveModelContext.modules

        then:
        modules.collect { it.QNameModule } as Set == qNameModules
    }

    def ".readConfigurationData() from empty YANG datastore results in empty <data> node"() {
        when:
        @Nonnull final data = this.testYangPool.readConfigurationData().get().get()

        then:
        @Nonnull final qName = data.identifier.nodeType
        qName.namespace as String == XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
        qName.localName == 'data'

        and:
        @Nonnull final children = data.body() as Collection
        children.empty
    }

    def ".readOperationalData() from empty YANG datastore results in empty <data> node"() {
        when:
        @Nonnull final data = this.testYangPool.readOperationalData().get().get()

        then:
        @Nonnull final qName = data.identifier.nodeType
        qName.namespace as String == XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
        qName.localName == 'data'

        and:
        @Nonnull final children = data.body() as Collection
        children.empty
    }
}
