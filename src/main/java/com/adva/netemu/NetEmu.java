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
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import one.util.streamex.StreamEx;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.mdsal.common.api.CommitInfo;

import com.adva.netemu.driver.EmuDriver;
import com.adva.netemu.service.EmuService;


public class NetEmu {

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
    private final List<Function<YangPool, ? extends EmuService>> serviceRegistry =
            Collections.synchronizedList(new ArrayList<>());

    @Nonnull
    private final Map<Class<? extends EmuService>, Thread> serviceThreads = new HashMap<>();

    public NetEmu(@Nonnull final YangPool pool) {
        this.pool = pool;
    }

    public <D extends EmuDriver> RegisteredDriver<D> registerDriver(@Nonnull final Class<D> driverClass) {
        this.driverRegistry.add(driverClass);
        return new RegisteredDriver<>(this, driverClass);
    }

    @SafeVarargs
    public final <S extends EmuService> void registerService(
            @Nonnull final Class<S> serviceClass, @Nonnull final EmuService.Settings<S>... constructionSettings) {

        for (@Nonnull final var settings : constructionSettings) {
            this.registerService(yangPool -> {
                try {
                    return serviceClass.getDeclaredConstructor(YangPool.class, settings.getClass())
                            .newInstance(yangPool, settings.getClass().cast(settings));

                } catch (final
                        NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {

                    throw new RuntimeException(e);
                }
            });
        }

        // this.serviceRegistry.add(serviceClass);
    }

    public void registerService(@Nonnull final Function<YangPool, ? extends EmuService> supplier) {
        this.serviceRegistry.add(supplier);
    }

    public synchronized void start() {

        for (@Nonnull final var serviceCreator : this.serviceRegistry) {
            @Nonnull final var service = serviceCreator.apply(this.pool);
            @Nonnull final var serviceThread = new Thread(service);
            serviceThread.setDaemon(false);
            serviceThread.start();

            this.serviceThreads.put(service.getClass(), serviceThread);
        }
    }

    /*
    public synchronized void stop() {
        for (@Nonnull final var serviceThread : this.serviceThreads.values()) {
        }
    }
    */

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromResources() {
        @Nonnull final var reflections = new Reflections(
                String.format("EMU-INF/config/%s", this.pool.id()),
                new ResourcesScanner());

        @Nonnull final var configResources = reflections.getResources(Pattern.compile(".*\\.xml$"));
        if (configResources.isEmpty()) {
            LOG.info("Found no EMU-INF/config resources for YangPool {}", this.pool.id());

        } else {
            LOG.info("Found EMU-INF/config resources for YangPool {}: {}", this.pool.id(), configResources);
        }

        @Nonnull final var futures = StreamEx.of(configResources)
                .map(configResource -> this.pool.loadConfigurationFromXml(Optional
                        .ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream(configResource))
                        .orElseThrow(() -> new IllegalStateException(String.format(
                                "Cannot access resource %s although found by %s", configResource, reflections)))))

                .toImmutableList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignoredVoid -> StreamEx.of(futures)
                .flatMap(futureCommitInfos -> StreamEx.of(futureCommitInfos.join()))
                .toImmutableList());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(@Nonnull final Reader reader) {
        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(reader);

        } catch (final XMLStreamException e) {
            LOG.error("While loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", reader);
            return CompletableFuture.completedFuture(List.of());
        }

        return FutureConverter.toCompletableFuture(this.pool.writeConfigurationDataFrom(xmlReader));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(
            @Nonnull final File file, @Nonnull final Charset encoding) {

        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new FileReader(file, encoding));

        } catch (final IOException | XMLStreamException e) {
            LOG.error("While opening file for loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", file);
            return CompletableFuture.completedFuture(List.of());
        }

        return FutureConverter.toCompletableFuture(this.pool.writeConfigurationDataFrom(xmlReader));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(@Nonnull final File file) {
        return this.loadConfigurationFromXml(file, StandardCharsets.UTF_8);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml() {
        return this.loadConfigurationFromXml(new File("configuration.xml").getAbsoluteFile());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> applyOperationalDataFromXml(
            @Nonnull final File file, @Nonnull final Charset encoding) {

        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new FileReader(file, encoding));

        } catch (final IOException | XMLStreamException e) {
            LOG.error("While opening file for loading Operational XML Data: ", e);
            LOG.error("Failed reading Operational XML Data from: {}", file);
            return CompletableFuture.completedFuture(List.of());
        }

        return FutureConverter.toCompletableFuture(this.pool.writeOperationalDataFrom(xmlReader));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> applyOperationalDataFromXml(@Nonnull final File file) {
        return this.applyOperationalDataFromXml(file, StandardCharsets.UTF_8);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> applyOperationalDataFromXml() {
        return this.applyOperationalDataFromXml(new File("operational.xml").getAbsoluteFile());
    }
}
