package com.adva.netemu

import java.util.concurrent.ExecutionException

import javax.annotation.Nonnull

import net.javacrumbs.futureconverter.java8guava.FutureConverter

import spock.lang.Specification

import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource

import org.opendaylight.netconf.api.NamespaceURN

import com.adva.netemu.testemu.NetEmuDefined


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

    def ".getSource(#moduleName@#revisionString) asynchronously yields according YANG module content"() {
        given:
        @Nonnull final identifier = new SourceIdentifier(moduleName, (Revision.of revisionString))

        when:
        @Nonnull final futureSource = FutureConverter.toCompletableFuture(this.testYangPool.getSource identifier)

        and:
        @Nonnull final content = futureSource.thenApplyAsync YangTextSchemaSource::read get()

        then:
        content =~ /(?ms)^\s*module\s+${moduleName}\s+\{[\s\n]+.+\s+revision\s+${revisionString}\s+\{/

        and: /// Just ensure that the schema source can be read repeatedly ...
        content == (futureSource.get() read())

        and: /// ...& that the result is the same when content is read after future completion
        content == (this.testYangPool.getSource identifier get() read())

        where:
        moduleName        | revisionString
        'iana-if-type'    | '2017-01-19'
        'ietf-interfaces' | '2018-02-20'
        'ietf-yang-types' | '2013-07-15'
    }

    def ".getSource() asynchronously throws when given YANG module is not found"() {
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
        @Nonnull final data = this.testYangPool.readConfigurationData() get() get()

        then:
        @Nonnull final qName = data.identifier.nodeType
        qName.namespace as String == NamespaceURN.BASE
        qName.localName == 'data'

        and:
        @Nonnull final children = data.body() as Collection
        children.empty
    }

    def ".readOperationalData() from empty YANG datastore results in empty <data> node"() {
        when:
        @Nonnull final data = this.testYangPool.readOperationalData() get() get()

        then:
        @Nonnull final qName = data.identifier.nodeType
        qName.namespace as String == NamespaceURN.BASE
        qName.localName == 'data'

        and:
        @Nonnull final children = data.body() as Collection
        children.empty
    }
}
