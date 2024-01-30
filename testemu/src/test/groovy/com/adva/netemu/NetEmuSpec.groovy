package com.adva.netemu

import java.util.concurrent.atomic.AtomicInteger

import javax.annotation.Nonnull

import spock.lang.Specification

import com.adva.netemu.southbound.NetconfDriver
import com.adva.netemu.testemu.NetEmuDefined


class NetEmuSpec extends Specification {

    @Nonnull
    def testEmu = NetEmu.withId 'netemu-test' fromYangModuleInfos NetEmuDefined.YANG_MODULE_INFOS

    @Nonnull
    static testEmuInstanceCounter = new AtomicInteger(1)

    @Nonnull
    static NetEmu newTestEmu() {
        NetEmu.withId "netemu-test-${testEmuInstanceCounter++}" fromYangModuleInfos NetEmuDefined.YANG_MODULE_INFOS
    }

    def ".id() string equals construction parameter"() {
        expect:
        this.testEmu.id() == 'netemu-test'
    }

    def ".identifier equals id string from construction"() {
        expect:
        this.testEmu.identifier == 'netemu-test'
    }

    def ".yangPool().id() string equals construction parameter"() {
        expect:
        this.testEmu.yangPool().id() == 'netemu-test'
    }

    def ".yangPool().modules() set equals construction parameter"() {
        expect:
        this.testEmu.yangPool().modules == NetEmuDefined.YANG_MODULE_INFOS
    }

    def ".activeDriverSession is empty if no driver registered"() {
        expect:
        this.testEmu.activeDriverSession.empty
    }

    def ".requireActiveDriverSession() throws if no driver registered"() {
        when:
        this.testEmu.requireActiveDriverSession()

        then:
        thrown NoSuchElementException
    }

    def ".activeDriverSession is empty if driver registered, but no session activated"() {
        given:
        final testEmu = newTestEmu()

        when:
        testEmu.registerDriver NetconfDriver

        then:
        testEmu.activeDriverSession.empty
    }

    def ".requireActiveDriverSession() throws if driver registered, but no session activated"() {
        given:
        final testEmu = newTestEmu()

        when:
        testEmu.registerDriver NetconfDriver
        testEmu.requireActiveDriverSession()

        then:
        thrown NoSuchElementException
    }
}
