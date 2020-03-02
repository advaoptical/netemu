package com.adva.netemu;

// import org.graalvm.compiler.hotspot.stubs.StubUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adva.netemu.driver.EmuDriver;
import com.adva.netemu.service.EmuService;


public final class NetEmu {

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

    public static final class RegisteredDriver<D extends EmuDriver> {
        @Nonnull
        private final NetEmu netEmu;

        @Nonnull
        private final Class<D> driverClass;

        private RegisteredDriver(@Nonnull final NetEmu emu, @Nonnull final Class<D> driverClass) {
            this.netEmu = emu;
            this.driverClass = driverClass;
        }

        public D newSessionFrom(@Nonnull final EmuDriver.Settings<D> settings) {
            try {
                return driverClass.getDeclaredConstructor(YangPool.class, EmuDriver.Settings.class)
                        .newInstance(this.netEmu.getYangPool(), settings);

            } catch (final
                    NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {

                throw new RuntimeException(e);
            }
        }
    }

    @Nonnull
    private final List<Class<? extends EmuDriver>> driverRegistry = Collections.synchronizedList(new ArrayList<>());

    @Nonnull
    private final List<Class<? extends EmuService>> serviceRegistry = Collections.synchronizedList(new ArrayList<>());

    @Nonnull
    private final Map<Class<? extends EmuService>, Thread> serviceThreads = new HashMap<>();

    public NetEmu(@Nonnull final YangPool pool) {
        this.pool = pool;
    }

    public <D extends EmuDriver> RegisteredDriver<D> registerDriver(@Nonnull final Class<D> driverClass) {
        this.driverRegistry.add(driverClass);
        return new RegisteredDriver<>(this, driverClass);
    }

    public void registerService(@Nonnull final Class<? extends EmuService> serviceClass) {
        this.serviceRegistry.add(serviceClass);
    }

    public synchronized void start() {
        for (@Nonnull final var serviceClass : this.serviceRegistry) {
            @Nonnull final Thread serviceThread;
            try {
                serviceThread = new Thread(serviceClass.getDeclaredConstructor(YangPool.class).newInstance(this.pool));

            } catch (final
                    NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {

                throw new RuntimeException(e);
            }

            serviceThread.setDaemon(false);
            serviceThread.start();
            this.serviceThreads.put(serviceClass, serviceThread);
        }
    }

    /*
    public synchronized void stop() {
        for (@Nonnull final var serviceThread : this.serviceThreads.values()) {
        }
    }
    */

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
