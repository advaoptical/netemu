package com.adva.netemu

import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull

import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier

import org.opendaylight.netconf.api.xml.XmlNetconfConstants

import spock.lang.Specification

import com.adva.netemu.testemu.NetEmuDefined


class YangPoolSpec extends Specification {

    @Nonnull
    def testYangPool = new YangPool("netemu-test", NetEmuDefined.YANG_MODULE_INFOS)

    def "Test .id() string"() {
        expect:
            this.testYangPool.id() == "netemu-test"
    }

    def "Test elements of YANG .modules() set"() {
        expect:
            this.testYangPool.getModules() == NetEmuDefined.YANG_MODULE_INFOS
    }

    def "Test YANG module content from .getSource(#moduleName@#revisionString)"() {
        given:
            @Nonnull final identifier = RevisionSourceIdentifier.create(moduleName, Revision.of(revisionString))
            @Nonnull final charset = StandardCharsets.US_ASCII

        when:
            @Nonnull final content = this.testYangPool.getSource(identifier).get().asCharSource(charset).read()

        then:
            content =~ /(?ms)^\s*module\s+${moduleName}\s+\{[\s\n]+.+\s+revision\s+${revisionString}\s+\{/

        where:
            moduleName        | revisionString
            "iana-if-type"    | "2017-01-19"
            "ietf-interfaces" | "2018-02-20"
            "ietf-yang-types" | "2013-07-15"
    }

    def "Test YANG modules from .getEffectiveModelContext()"() {
        given:
            @Nonnull final qNameModules = NetEmuDefined.YANG_MODULE_INFOS.collect { it.getName().getModule() }.toSet()

        when:
            @Nonnull final modules = this.testYangPool.getEffectiveModelContext().getModules()

        then:
            modules.collect { it.getQNameModule() }.toSet() == qNameModules
    }

    def "Test .readConfigurationData() from empty datastore"() {
        when:
            @Nonnull final data = this.testYangPool.readConfigurationData().get().get()

        then:
            @Nonnull final qName = data.getNodeType()
            qName.getNamespace() as String == XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
            qName.getLocalName() == "data"

            @Nonnull final children = data.getValue() as Collection
            children.size() == 0
    }

    def "Test .readOperationalData() from empty datastore"() {
        when:
            @Nonnull final data = this.testYangPool.readOperationalData().get().get()

        then:
            @Nonnull final qName = data.getNodeType()
            qName.getNamespace() as String == XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0
            qName.getLocalName() == "data"

            @Nonnull final children = data.getValue() as Collection
            children.size() == 0
    }
}
