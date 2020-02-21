package com.adva.netemu;

// import org.graalvm.compiler.hotspot.stubs.StubUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.ConfigurationBuilder;

import com.adva.netemu.service.PythonService;


public final class NetEmu extends NetconfDeviceSimulator {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(NetEmu.class);

    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    @Nonnull
    private final YangPool pool;

    @Nonnull
    public YangPool getYangPool() {
        return this.pool;
    }

    public NetEmu(@Nonnull final YangPool pool) {
        super(new ConfigurationBuilder()
                .setCapabilities(ImmutableSet.of("urn:ietf:params:netconf:base:1.0", "urn:ietf:params:netconf:base:1.1"))
                .setModels(Set.copyOf(pool.getModules()))
                .setRpcMapping(new NetconfRequest(pool))
                .build());

        this.pool = pool;
    }

    @Override
    public List<Integer> start() {
        @Nonnull final var python = new PythonService(this.pool);
        return super.start();
    }

    public void loadConfigurationFromXml(@Nonnull final Reader reader) {
        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);

        } catch (final XMLStreamException e) {
            LOG.error("While loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", reader);
            return;
        }

        this.pool.writeConfigurationDataFrom(xmlReader);
    }

    public void loadConfigurationFromXml(@Nonnull final File file, @Nonnull final Charset encoding) {
        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new FileReader(file, encoding));

        } catch (final IOException | XMLStreamException e) {
            LOG.error("While opening file for loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", file);
            return;
        }

        this.pool.writeConfigurationDataFrom(xmlReader);
    }

    public void loadConfigurationFromXml(@Nonnull final File file) {
        this.loadConfigurationFromXml(file, StandardCharsets.UTF_8);
    }

    public void loadConfigurationFromXml() {
        this.loadConfigurationFromXml(new File("configuration.xml").getAbsoluteFile());
    }

    public void applyOperationalDataFromXml(@Nonnull final File file, @Nonnull final Charset encoding) {

        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new FileReader(file, encoding));

        } catch (final IOException | XMLStreamException e) {
            LOG.error("While opening file for loading Operational XML Data: ", e);
            LOG.error("Failed reading Operational XML Data from: {}", file);
            return;
        }

        this.pool.writeOperationalDataFrom(xmlReader);
    }

    public void applyOperationalDataFromXml(@Nonnull final File file) {
        this.applyOperationalDataFromXml(file, StandardCharsets.UTF_8);
    }

    public void applyOperationalDataFromXml() {
        this.applyOperationalDataFromXml(new File("operational.xml").getAbsoluteFile());
    }
}
