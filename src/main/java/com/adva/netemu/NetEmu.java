package com.adva.netemu;

// import org.graalvm.compiler.hotspot.stubs.StubUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;

import javax.annotation.Nonnull;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.ConfigurationBuilder;


public final class NetEmu extends NetconfDeviceSimulator {

    private static final Logger LOG = LoggerFactory.getLogger(NetEmu.class);

    private static final XMLInputFactory XML_INPUT_FACTORY =
            XMLInputFactory.newInstance();

    @Nonnull
    private final YangPool _pool;

    @Nonnull
    public YangPool getYangPool() {
        return this._pool;
    }

    public NetEmu(@Nonnull final YangPool pool) {
        super(new ConfigurationBuilder()
                .setCapabilities(ImmutableSet.of(
                        "urn:ietf:params:netconf:base:1.0",
                        "urn:ietf:params:netconf:base:1.1"))

                .setModels(ImmutableSet.copyOf(pool.getModules()))
                .setRpcMapping(new NetconfRequest(pool))
                .build());

        this._pool = pool;
    }

    public void loadConfigurationFromXml(@Nonnull final Reader reader) {
        final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);

        } catch (XMLStreamException e) {
            LOG.error("Cannot use reader for loading XML Configuration: "
                    + reader);

            e.printStackTrace();
            LOG.error("Failed reading XML Configuration from: " + reader);
            return;
        }

        this._pool.writeConfigurationDataFrom(xmlReader);
    }

    public void loadConfigurationFromXml(
            @Nonnull final File file, @Nonnull final Charset encoding) {

        final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(
                    new FileReader(file, encoding));

        } catch (final
                IOException |
                XMLStreamException e) {

            LOG.error("Cannot open file for loading XML Configuration: "
                    + file);

            e.printStackTrace();
            LOG.error("Failed reading XML Configuration from: " + file);
            return;
        }

        this._pool.writeConfigurationDataFrom(xmlReader);
    }

    public void loadConfigurationFromXml(@Nonnull final File file) {
        this.loadConfigurationFromXml(file, UTF_8);
    }

    public void loadConfigurationFromXml() {
        this.loadConfigurationFromXml(
                new File("configuration.xml").getAbsoluteFile());
    }

    public void applyOperationalDataFromXml(
            @Nonnull final File file, @Nonnull final Charset encoding) {

        final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(
                    new FileReader(file, encoding));

        } catch (final
                IOException |
                XMLStreamException e) {

            LOG.error("Cannot open file for loading Operational XML Data: "
                    + file);

            e.printStackTrace();
            LOG.error("Failed reading Operational XML Data from: " + file);
            return;
        }

        this._pool.writeOperationalDataFrom(xmlReader);
    }

    public void applyOperationalDataFromXml(@Nonnull final File file) {
        this.applyOperationalDataFromXml(file, UTF_8);
    }

    public void applyOperationalDataFromXml() {
        this.applyOperationalDataFromXml(
                new File("configuration.xml").getAbsoluteFile());
    }
}
