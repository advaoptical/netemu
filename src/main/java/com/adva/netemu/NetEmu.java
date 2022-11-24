package com.adva.netemu;

// import org.graalvm.compiler.hotspot.stubs.StubUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.io.PatternFilenameFilter;
import io.github.classgraph.ClassGraph;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import net.jodah.typetools.TypeResolver;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.ArrayUtils;

/*
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

import org.opendaylight.mdsal.common.api.CommitInfo;

import com.adva.netemu.driver.EmuDriver;
import com.adva.netemu.service.EmuService;


public class NetEmu {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(NetEmu.class);

    @Nonnull
    private static final ClassGraph CLASS_GRAPH = new ClassGraph();

    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    @Nonnull
    private final YangPool pool;

    @Nonnull
    public YangPool getYangPool() {
        return this.pool;
    }

    @Nonnull
    public String id() {
        return this.getYangPool().id();
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

        @Nonnull
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

    public static final class RegisteredService<S extends EmuService<T>, T extends EmuService.Settings<S>> {

        @Nonnull
        private final String name;

        @Nonnull
        public String name() {
            return this.name;
        }

        @Nonnull
        private final NetEmu emu;

        @Nonnull
        private final Class<S> serviceClass;

        @Nonnull
        public Class<S> getServiceClass() {
            return this.serviceClass;
        }

        @Nonnull @SuppressWarnings({"unchecked"})
        public Class<T> getSettingsClass() {
            return (Class<T>) TypeResolver.resolveRawArguments(EmuService.class, this.serviceClass)[0];
        }

        @Nonnull
        private final T settings;

        @Nonnull
        private final BiFunction<YangPool, T, S> instanceCreator;

        @Nonnull
        private final AtomicBoolean enabled = new AtomicBoolean(true);

        public boolean isEnabled() {
            return this.enabled.get();
        }

        @Nonnull
        private final AtomicReference<Thread> starterThread = new AtomicReference<>();

        @Nonnull
        public Optional<Thread> getStarterThread() {
            return Optional.ofNullable(this.starterThread.get());
        }

        @Nonnull
        private final AtomicReference<S> instance = new AtomicReference<>();

        @Nonnull
        public Optional<S> getInstance() {
            return Optional.of(this.instance.get());
        }

        public boolean isStarting() {
            return this.getStarterThread().map(Thread::isAlive).orElse(false);
        }

        public boolean isRunning() {
            return this.getStarterThread().map(thread -> !thread.isAlive()).orElse(false);
        }

        private RegisteredService(@Nonnull final String name, @Nonnull final NetEmu emu, @Nonnull final Class<S> serviceClass,
                @Nullable final T settings,
                @Nullable final BiFunction<YangPool, T, S> instanceCreator) {

            this.name = Objects.requireNonNull(name, "Missing name for EMU-Service registration");
            this.emu = Objects.requireNonNull(emu, "Missing EMU-Instance reference for EMU-Service registration");
            this.serviceClass = Objects.requireNonNull(serviceClass, "Missing Class<> for EMU-Service registration");

            @Nonnull final var settingsClass = this.getSettingsClass();
            this.settings = Objects.requireNonNullElseGet(settings, () -> {
                try {
                    return settingsClass.getDeclaredConstructor().newInstance();

                } catch (final
                        InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {

                    throw new RuntimeException("Failed construction of default settings for EMU-Service registration", e);
                }
            });

            this.instanceCreator = Objects.requireNonNullElse(instanceCreator, (yangPool, instanceSettings) -> {
                try {
                    return this.serviceClass.getDeclaredConstructor(YangPool.class, settingsClass)
                            .newInstance(yangPool, instanceSettings);

                } catch (final
                        InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {

                    throw new RuntimeException("Failed EMU-Service instantiation", e);
                }
            });
        }

        private RegisteredService(@Nonnull final String name, @Nonnull final NetEmu emu, @Nonnull final Class<S> serviceClass,
                @Nonnull final BiFunction<YangPool, T, S> instanceCreator) {

            this(name, emu, serviceClass, null, instanceCreator);
        }

        private RegisteredService(@Nonnull final String name, @Nonnull final NetEmu emu, @Nonnull final Class<S> serviceClass) {
            this(name, emu, serviceClass, null, null);
        }

        public void enable() {
            LOG.info("Enabling {}", this);
            this.enabled.set(true);
        }

        public void disable() {
            LOG.info("Disabling {}", this);
            this.enabled.set(false);
        }

        @Nonnull
        public Thread start() {
            synchronized (this.enabled) {
                if (!this.isEnabled()) {
                    throw new RuntimeException("Service is disabled");
                }

                synchronized (this.starterThread) {
                    if (this.isStarting()) {
                        throw new RuntimeException("Service is already starting");
                    }

                    synchronized (this.instance) {
                        if (this.isRunning()) {
                            throw new RuntimeException("Service is already running");
                        }

                        @Nonnull final var service = this.instanceCreator.apply(this.emu.pool, this.settings);
                        this.instance.set(service);

                        @Nonnull final var starterThread = new Thread(service);
                        starterThread.setDaemon(false);
                        starterThread.start();

                        this.starterThread.set(starterThread);
                        return starterThread;
                    }
                }
            }
        }
    }

    @Nonnull
    private final List<Class<? extends EmuDriver>> driverRegistry = Collections.synchronizedList(new ArrayList<>());

    @Nonnull
    private final List<RegisteredService<?, ?>> serviceRegistry = Collections.synchronizedList(new ArrayList<>());

    /*
    @Nonnull
    private final Map<Class<? extends EmuService>, Thread> serviceThreads = new HashMap<>();
    */

    @Nonnull
    public List<RegisteredService<?, ?>> registeredServices() {
        synchronized (this.serviceRegistry) {
            return List.copyOf(this.serviceRegistry);
        }
    }

    protected NetEmu(@Nonnull final YangPool pool) {
        this.pool = pool;
    }

    @Nonnull
    public static NetEmu forYangPool(@Nonnull final YangPool yangPool) {
        return new NetEmu(yangPool);
    }

    @Nonnull
    public static Factory withId(@Nonnull final String id) {
        return new Factory(id);
    }

    public static class Factory {

        @Nonnull
        private final String id;

        private Factory(@Nonnull final String id) {
            this.id = id;
        }

        @Nonnull
        public NetEmu fromYangModuleInfos(@Nonnull final Collection<YangModuleInfo> yangModuleInfos) {
            return NetEmu.forYangPool(new YangPool(this.id, yangModuleInfos));
        }

        @Nonnull
        public NetEmu fromYangModuleInfos(@Nonnull final YangModuleInfo ...yangModuleInfos) {
            return NetEmu.forYangPool(new YangPool(this.id, yangModuleInfos));
        }
    }

    public <D extends EmuDriver> RegisteredDriver<D> registerDriver(@Nonnull final Class<D> driverClass) {
        this.driverRegistry.add(driverClass);
        return new RegisteredDriver<>(this, driverClass);
    }

    /*
    @SafeVarargs
    public final <S extends EmuService<T>, T extends EmuService.Settings<S>> List<RegisteredService<S, T>> registerService(
            @Nonnull final Class<S> serviceClass, @Nonnull final T... constructionSettings) {

        return StreamEx.of(constructionSettings).map(settingsInstance -> this.registerService(serviceClass.getName(), (yangPool, settings) -> {
            try {
                return serviceClass.getDeclaredConstructor(YangPool.class, settings.getClass())
                        .newInstance(yangPool, settings.getClass().cast(settings));

            } catch (final
                    NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {

                throw new RuntimeException(e);
            }

        })).toImmutableList();
    }
    */

    @Nonnull
    public <S extends EmuService<T>, T extends EmuService.Settings<S>> T // RegisteredService<S, T>
    registerService(@Nonnull final String name, @Nonnull final Class<S> serviceClass, @Nonnull final BiFunction<YangPool, T, S> instanceCreator) {
        @Nonnull final var registeredService = new RegisteredService<>(name, this, serviceClass, instanceCreator);
        this.serviceRegistry.add(registeredService);
        return registeredService.settings;
    }

    @Nonnull
    public <S extends EmuService<T>, T extends EmuService.Settings<S>> T // RegisteredService<S, T>
    registerService(@Nonnull final String name, @Nonnull final Class<S> serviceClass) {
        @Nonnull final var registeredService = new RegisteredService<>(name, this, serviceClass);
        this.serviceRegistry.add(registeredService);
        return registeredService.settings;
    }

    @Nonnull
    public <S extends EmuService<T>, T extends EmuService.Settings<S>> T // RegisteredService<S, T>
    registerService(@Nonnull final Class<S> serviceClass) {
        return this.registerService(serviceClass.getName(), serviceClass);
    }

    public synchronized void start() {

        for (@Nonnull final var registeredService : this.serviceRegistry) {
            /*
            @Nonnull final var service = serviceCreator.apply(this.pool);
            @Nonnull final var serviceThread = new Thread(service);
            serviceThread.setDaemon(false);
            serviceThread.start();
            */

            // this.serviceThreads.put(service.getClass(), serviceThread);
            registeredService.start();
        }
    }

    /*
    public synchronized void stop() {
        for (@Nonnull final var serviceThread : this.serviceThreads.values()) {
        }
    }
    */

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfiguration() {
        return this.loadConfigurationFromResources().thenCompose(resourceCommitInfos ->
                this.loadConfigurationFromCurrentDirectory().thenApply(fileCommitInfos ->
                        StreamEx.of(resourceCommitInfos).append(fileCommitInfos).toImmutableList()));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromResources() {
        /*
        @Nonnull final var reflections = new Reflections(String.format("EMU-INF/config/%s", this.pool.id()),
                new ResourcesScanner());

        @Nonnull final var configResources = reflections.getResources(Pattern.compile(".*\\.xml$"));
        if (configResources.isEmpty()) {
            LOG.info("Found no config resources for YangPool {}", this.pool.id());
            return CompletableFuture.completedFuture(List.of());
        }
        */

        @Nonnull final List<CompletableFuture<List<CommitInfo>>> futures = new ArrayList<>();
        try (@Nonnull final var scanResult = CLASS_GRAPH.acceptPaths(String.format("EMU-INF/config/%s", this.pool.id()))
                .scan()) {

            @Nonnull final var configResources = scanResult.getResourcesWithExtension("xml");
            LOG.info("Found config resources for YangPool {}: {}", this.pool.id(), configResources);

            try {
                configResources.forEachInputStreamThrowingIOException(((resource, inputStream) -> {
                    futures.add(this.pool.loadConfigurationFromXml(inputStream));
                }));

            } catch (final IOException e) {
                throw new IllegalStateException(String.format("Cannot access resource found by %s", scanResult), e);
            }
        }

        /*
        @Nonnull final var futures = StreamEx.of(configResources)
                .map(configResource -> this.pool.loadConfigurationFromXml(Optional
                        .ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream(configResource))
                        .orElseThrow(() -> new IllegalStateException(String.format(
                                "Cannot access resource %s although found by %s", configResource, reflections)))))

                .toImmutableList();
        */

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignoredVoid -> StreamEx.of(futures)
                .flatMap(futureCommitInfos -> StreamEx.of(futureCommitInfos.join()))
                .toImmutableList());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromCurrentDirectory() {
        @Nullable final var configFiles = Paths.get("EMU-CONF", this.pool.id()).toAbsolutePath().toFile()
                .listFiles(new PatternFilenameFilter(".*\\.xml$"));

        if (ArrayUtils.isEmpty(configFiles)) {
            LOG.info("Found no config files for YangPool {} in current directory", this.pool.id());
            return CompletableFuture.completedFuture(List.of());
        }

        LOG.info("Found config files for YangPool {} in current directory: {}", this.pool.id(), configFiles);

        @Nonnull final var futures = StreamEx.of(configFiles).map(this.pool::loadConfigurationFromXml).toImmutableList();
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
            @Nonnull final File file,
            @Nonnull final Charset encoding) {

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
