package com.adva.netemu;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

// import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.rfc8528.data.util.EmptyMountPointContext;
import org.opendaylight.yangtools.yang.binding.ChildOf;
// import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
// import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.CurrentAdapterSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;

/*
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
*/

// import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.spi.BindingRuntimeHelpers;

import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;

import com.adva.netemu.datastore.DaggerYangDatastore;
import com.adva.netemu.datastore.YangDatastore;


/** Manages datastores, transactions, and data-bindings for a set of YANG modules.
  */
public class YangPool implements EffectiveModelContextProvider, SchemaSourceProvider<YangTextSchemaSource> {

    /** Default logger for this class.
      */
    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(YangPool.class);

    /** Provider of tools for reading XML data.
      */
    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    /** Default executor for asynchronous OpenDaylight operations.
     */
    @Nonnull
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(0); // 0 -> no idle threads

    /** Default executor for asynchronous OpenDaylight datastore transactions.
      */
    @Nonnull
    private final ScheduledExecutorService transactionExecutor = new ScheduledThreadPoolExecutor(0); // 0 -> no idle threads

    /** Default executor for logging results of asynchronous OpenDaylight datastore transactions.
     */
    @Nonnull
    private final Executor loggingCallbackExecutor = MoreExecutors.directExecutor();

    /** Unique ID of this YANG pool.
      */
    @Nonnull
    private final String id;

    /** Returns the unique, non-changeable ID of this YANG pool.

      * @return
            The ID string
      */
    @Nonnull
    public String id() {
        return this.id;
    }

    /** Set of schemas defining this YANG pool's datastore structures.
      */
    @Nonnull
    private final Set<YangModuleInfo> modules;

    /** Returns the schemas of this YANG pool.

      * @return
            An immutable set
      */
    @Nonnull
    public Set<YangModuleInfo> getModules() {
        return this.modules;
    }

    /** Returns the source of YANG module specified by given identifier.

      * This is an asynchronous operation!

      * <p> NETEMU prefers {@link CompletableFuture} over the {@link ListenableFuture} returned by this overriden OpenDaylight
        method from {@link SchemaSourceProvider}. For a consistent coding style,
        {@link FutureConverter#toCompletableFuture(ListenableFuture)} should be applied.

      * <p> For example, to get the content of YANG module {@code ietf-interfaces@2018-02-20} as a string:

      * <pre>{@code
      *     var identifier = RevisionSourceIdentifier
      *             .create("ietf-interfaces", Revision.of("2018-02-20"));
      *
      *     FutureConverter.toCompletableFuture(yangPool.getSource(identifier))
      *             .thenAccept(source -> {
      *                 var content = source.asCharSource(StandardCharsets.US_ASCII).read();
      *                 ...
      *             });
      * }</pre>

      * @param identifier
            A unique YANG module identification by name and optional revision. Default implementations are
            {@link org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier} and
            {@link org.opendaylight.yangtools.yang.model.repo.api.SemVerSourceIdentifier}

      * @return
            The readable YANG module source. This is an asynchronous result!
      */
    @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
    public ListenableFuture<? extends YangTextSchemaSource> getSource(@Nonnull final SourceIdentifier identifier) {

        return Futures.submit(() -> StreamEx.of(this.modules).findFirst(module -> {
            @Nonnull final var qName = module.getName();
            return qName.getLocalName().equals(identifier.getName()) && qName.getRevision().equals(identifier.getRevision());

        }).map(module -> YangTextSchemaSource.delegateForByteSource(identifier, module.getYangTextByteSource())).orElseThrow(() ->
                new NoSuchElementException(identifier.toString())), this.executor);
    }

    /** Model context of this YANG pool.
      */
    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    private final EffectiveModelContext context;

    @Deprecated @Nonnull
    public SchemaContext getYangContext() {
        return this.context; // .getSchemaContext();
    }

    /** Returns the model context of this YANG pool.

      * @return The immutable YANG model context
      */
    @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
    public EffectiveModelContext getEffectiveModelContext() {
        return this.context;

        /*
        return this.context.tryToCreateModelContext().orElseThrow(() -> new RuntimeException(String.format(
                "Failed to create instance of %s from %s", EffectiveModelContext.class, this.context)));
        */
    }

    /** Datastore of this YANG pool.
      */
    @Nonnull
    private final YangDatastore datastore = DaggerYangDatastore.create();

    @Nonnull
    private final InMemoryDOMDataStore configurationStore;

    @Nonnull
    private final InMemoryDOMDataStore operationalStore;

    @Nonnull
    private final BindingDOMDataBrokerAdapter broker;

    @Nonnull
    public DataBroker getDataObjectBroker() {
        return this.broker;
    }

    public static class NormalizedNodeBroker extends SerializedDOMDataBroker {

        @Nonnull
        private final YangPool yangPool;

        private NormalizedNodeBroker(@Nonnull final YangPool yangPool) {
            super(Map.of(
                    LogicalDatastoreType.CONFIGURATION, yangPool.configurationStore,
                    LogicalDatastoreType.OPERATIONAL, yangPool.operationalStore

            ), MoreExecutors.listeningDecorator(yangPool.transactionExecutor));

            this.yangPool = yangPool;
        }

        @Nonnull @Override
        public DOMTransactionChain createTransactionChain(@Nonnull final DOMTransactionChainListener listener) {
            LOG.info("Updating {} Datastore from {} Datastore", LogicalDatastoreType.OPERATIONAL,
                    LogicalDatastoreType.CONFIGURATION);

            @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final ListenableFuture<List<CommitInfo>> updatingFuture =
                    this.yangPool.readConfigurationData().transformAsync(config -> config
                            .map(data -> Futures.allAsList(StreamEx.of(((ContainerNode) data).body())
                                    .map(childNode -> (ListenableFuture<CommitInfo>)
                                            this.yangPool.writeOperationalData(childNode))))

                            .orElseGet(() -> Futures.immediateFuture(List.of())), this.yangPool.executor);

            try {
                synchronized (this.yangPool.yangBindingRegistry) {
                    LOG.info("Updating {} Datastore from all registered bindings", LogicalDatastoreType.OPERATIONAL);

                    Futures.transformAsync(updatingFuture, /* ignoredCommitInfos -> Futures.allAsList(
                            StreamEx.of(this.yangPool.yangBindingRegistry).map(this.yangPool::writeOperationalDataFrom) */
                            ignoredCommitInfos -> this.yangPool.writeOperationalDataFrom(this.yangPool.yangBindingRegistry),
                            this.yangPool.executor

                    ).get();
                }

            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Failed updating {} Datastore from registered bindings", LogicalDatastoreType.OPERATIONAL,
                        e.getCause());
            }

            return super.createTransactionChain(listener);
        }
    }

    @Nonnull
    private final NormalizedNodeBroker normalizedNodeBroker;

    @Nonnull
    public NormalizedNodeBroker getNormalizedNodeBroker() {
        return this.normalizedNodeBroker;
    }

    @Nonnull
    private final List<YangBinding<?, ?>> yangBindingRegistry = Collections.synchronizedList(new ArrayList<>());

    @Nonnull
    private final List<YangBindable> yangBindableRegistry = Collections.synchronizedList(new ArrayList<>());

    @Nonnull
    private final List<YangListBindable> yangListBindableRegistry = Collections.synchronizedList(new ArrayList<>());

    public YangPool(@Nonnull final String id, @Nonnull final YangModuleInfo... modules) {
        this(id, List.of(modules));
    }

    public YangPool(@Nonnull final String id, @Nonnull final Collection<YangModuleInfo> modules) {
        this.id = Objects.requireNonNull(id);
        this.modules = Set.copyOf(Objects.requireNonNull(modules));
        this.context = BindingRuntimeHelpers.createEffectiveModel(this.modules);
        // this.context.addModuleInfos(this.modules);

        this.configurationStore = new InMemoryDOMDataStore(
                String.format("netemu-%s-configuration", id),
                LogicalDatastoreType.CONFIGURATION,
                MoreExecutors.newDirectExecutorService(), // -> writing data immediately executes change listeners

                /* TODO: (?)
                    Use this.transactionExecutor instead, and enhance YangBinding.DatastoreBinding to make its change listener
                    awaitable and back-traceable to its originating write event
                */

                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE,
                false);

        this.configurationStore.onModelContextUpdated(this.context);

        this.operationalStore = new InMemoryDOMDataStore(
                String.format("netemu-%s-operational", id),
                LogicalDatastoreType.OPERATIONAL,
                MoreExecutors.newDirectExecutorService(), // -> writing data immediately executes change listeners

                /* TODO: (?)
                    Use this.transactionExecutor instead, and enhance YangBinding.DatastoreBinding to make its change listener
                    awaitable and back-traceable to its originating write event
                */

                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE,
                false);

        this.operationalStore.onModelContextUpdated(this.context);

        @Nonnull final var runtimeContext = BindingRuntimeHelpers.createRuntimeContext(
                StreamEx.of(this.modules).map(YangModuleInfo::getClass).toArray(Class[]::new));

        @Nonnull final var adapterContext = new AdapterContext() {

            @Nonnull @Override
            public CurrentAdapterSerializer currentSerializer() {
                return new CurrentAdapterSerializer(new BindingCodecContext(runtimeContext));
            }
        };

        this.normalizedNodeBroker = new NormalizedNodeBroker(this);
        this.broker = new BindingDOMDataBrokerAdapter(adapterContext, this.normalizedNodeBroker);

        /*
                new BindingToNormalizedNodeCodec(this.context, new BindingNormalizedNodeCodecRegistry(
                        BindingRuntimeContext.create(this.context, this.getYangContext()))));
        */

        // @Nonnull final var rpcService = new BindingDOMRpcProviderServiceAdapter(adapterContext, )
    }

    @Nonnull
    private final AtomicReference<CompletableFuture<? extends YangBinding<?, ?>>> yangBindingRegisteringFuture =
            new AtomicReference<>(CompletableFuture.completedFuture(null));

    @Nonnull
    public CompletableFuture<? extends YangBinding<?, ?>> awaitYangBindingRegistering() {
        return this.yangBindingRegisteringFuture.get();
    }

    @Nonnull @SuppressWarnings({"UnusedReturnValue"})
    public <T extends YangBinding<Y, B>, Y extends ChildOf<?>, B extends Builder<Y>>
    CompletableFuture<T> registerYangBinding(@Nonnull final T binding) {
        binding.setYangPool(this);

        synchronized (this.yangBindingRegisteringFuture) {
            @Nonnull final var futureBinding = this.yangBindingRegisteringFuture.get()
                    .thenApplyAsync(ignoredRegisteredBinding -> {
                        LOG.info("Registering {}", binding);

                        for (@Nonnull final YangBinding<Y, B>.DatastoreBinding storeBinding : List.of(
                                binding.createConfigurationDatastoreBinding(),
                                binding.createOperationalDatastoreBinding())) {

                            LOG.debug("Registering {} Change listener for {}", storeBinding.storeType(), binding);
                            this.broker.registerDataTreeChangeListener(storeBinding.getDataTreeId(), storeBinding);
                        }

                        this.yangBindingRegistry.add(binding);
                        return binding;
                    });

            this.yangBindingRegisteringFuture.set(futureBinding);
            return futureBinding;
        }
    }

    @Nonnull
    public <T extends YangBindable> T registerYangBindable(@Nonnull final T object) {
        object.getYangBinding().ifPresent(this::registerYangBinding);
        this.yangBindableRegistry.add(object);
        return object;
    }

    @Nonnull
    public <T extends YangListBindable> T registerYangBindable(@Nonnull final T object) {
        object.getYangListBinding().ifPresent(this::registerYangBinding);
        this.yangListBindableRegistry.add(object);
        return object;
    }

    @Nonnull
    public <T> Optional<T> findRegisteredInstanceOf(@Nonnull final Class<T> registreeClass) {
        return StreamEx.of(this.yangBindableRegistry).findFirst(registreeClass::isInstance)
                .map(registreeClass::cast)
                .or(() -> StreamEx.of(this.yangListBindableRegistry).findFirst(registreeClass::isInstance)
                        .map(registreeClass::cast));
    }

    @Nonnull
    public Optional<Object> findRegisteredInstanceOf(@Nonnull final String registreeClassName) {
        return StreamEx.of(this.yangBindableRegistry)
                .findFirst(object -> object.getClass().getCanonicalName().equals(registreeClassName))
                .map(Object.class::cast)
                .or(() -> StreamEx.of(this.yangListBindableRegistry)
                        .findFirst(object -> object.getClass().getCanonicalName().equals(registreeClassName))
                        .map(Object.class::cast));
    }

    @Nonnull
    private final
    Map<SchemaPath, BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName>>
    xmlDataInputElementTagProcessorRegistry =

            Collections.synchronizedMap(new HashMap<>());

    public void registerXmlDataInputElementTagProcessor(
            @Nonnull final String yangPath,
            @Nonnull final BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTagProcessor(
            @Nonnull final String[] yangPath,
            @Nonnull final BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTagProcessor(
            @Nonnull final QName[] yangPath,
            @Nonnull final BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTagProcessor(
            @Nonnull final SchemaPath yangPath,
            @Nonnull final BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(yangPath, processor);
    }

    @Nonnull
    private final
    Map<SchemaPath, BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>>
    xmlDataInputElementNamespaceProcessorRegistry =

            Collections.synchronizedMap(new HashMap<>());

    public void registerXmlDataInputElementNamespaceProcessor(
            @Nonnull final String yangPath,
            @Nonnull final BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>> processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementNamespaceProcessor(
            @Nonnull final String[] yangPath,
            @Nonnull final BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>> processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementNamespaceProcessor(
            @Nonnull final QName[] yangPath,
            @Nonnull final BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>> processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementNamespaceProcessor(
            @Nonnull final SchemaPath yangPath,
            @Nonnull final BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>> processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(yangPath, processor);
    }

    @Nonnull
    private final Map<SchemaPath, BiFunction<SchemaPath, String, String>> xmlDataInputElementTextProcessorRegistry =
            Collections.synchronizedMap(new HashMap<>());

    public void registerXmlDataInputElementTextProcessor(
            @Nonnull final String yangPath, @Nonnull final BiFunction<SchemaPath, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTextProcessor(
            @Nonnull final String[] yangPath, @Nonnull final BiFunction<SchemaPath, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTextProcessor(
            @Nonnull final QName[] yangPath, @Nonnull final BiFunction<SchemaPath, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTextProcessor(
            @Nonnull final SchemaPath yangPath, @Nonnull final BiFunction<SchemaPath, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(yangPath, processor);
    }

    private YangXmlDataInput createYangXmlDataInputUsing(@Nonnull final XMLStreamReader reader) {
        return YangXmlDataInput.using(
                reader, this.getYangContext(),
                Map.copyOf(this.xmlDataInputElementTagProcessorRegistry),
                Map.copyOf(this.xmlDataInputElementNamespaceProcessorRegistry),
                Map.copyOf(this.xmlDataInputElementTextProcessorRegistry));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture</*? extends List<*/? extends CommitInfo> updateConfigurationData() {
        LOG.info("Updating {} Datastore from all registered bindings", LogicalDatastoreType.CONFIGURATION);

        synchronized (this.yangBindingRegistry) {
            return FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(this.yangBindingRegistry));
            /* Futures.allAsList(StreamEx.of(this.yangBindingRegistry)
                    .map(this::writeConfigurationDataFrom))); */
        }
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture</*? extends List<*/? extends CommitInfo> updateOperationalData() {
        LOG.info("Updating {} Datastore from all registered bindings", LogicalDatastoreType.OPERATIONAL);

        synchronized (this.yangBindingRegistry) {
            return FutureConverter.toCompletableFuture(this.writeOperationalDataFrom(this.yangBindingRegistry));
            /* Futures.allAsList(StreamEx.of(this.yangBindingRegistry)
                    .map(this::writeOperationalDataFrom))); */
        }
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<Optional<NormalizedNode>> readConfigurationData() {
        return this.readData(LogicalDatastoreType.CONFIGURATION);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<Optional<NormalizedNode>> readOperationalData() {
        return this.readData(LogicalDatastoreType.OPERATIONAL);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<Optional<NormalizedNode>> readData(@Nonnull final LogicalDatastoreType storeType) {
        @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final ListenableFuture<? extends CommitInfo> updatingFuture;

        if (storeType == LogicalDatastoreType.OPERATIONAL) {
            updatingFuture = this.readConfigurationData().transformAsync(config -> {
                LOG.info("Updating {} Datastore from {} Datastore", storeType, LogicalDatastoreType.CONFIGURATION);

                return FluentFuture.from(config
                        .map(data -> Futures.allAsList(StreamEx.of(((ContainerNode) data).body())
                                .map(childNode -> (ListenableFuture<? extends CommitInfo>) this.writeOperationalData(childNode))))

                        .orElseGet(() -> Futures.immediateFuture(List.of())));

            }, this.executor).transformAsync(ignoredCommitInfo -> {
                synchronized (this.yangBindingRegistry) {
                    // return Futures.allAsList(StreamEx.of(this.yangBindingRegistry).map(this::writeOperationalDataFrom));
                    LOG.info("Updating {} Datastore from all registered bindings", storeType);

                    return this.writeOperationalDataFrom(this.yangBindingRegistry);
                }

            }, this.executor);

        } else {
            updatingFuture = Futures.transformAsync(FutureConverter.toListenableFuture(this.awaitYangBindingRegistering()),
                    ignoredRegisteredBinding -> {
                        synchronized (this.yangBindingRegistry) {
                            // return Futures.allAsList(StreamEx.of(this.yangBindingRegistry).map(this::writeConfigurationDataFrom));
                            LOG.info("Updating {} Datastore from all registered bindings", LogicalDatastoreType.CONFIGURATION);

                            return this.writeConfigurationDataFrom(this.yangBindingRegistry);
                        }

                    }, this.executor);
        }

        return FluentFuture.from(updatingFuture).transformAsync(ignoredCommitInfos -> {
            @Nonnull final var txn = this.getNormalizedNodeBroker().newReadOnlyTransaction();
            @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var readingFuture =
                    txn.read(storeType, YangInstanceIdentifier.empty());

            LOG.info("Reading from {} Datastore", storeType);
            readingFuture.addCallback(this.datastore.injectReading().of(storeType).futureCallback, this.loggingCallbackExecutor);
            return readingFuture;

        }, this.executor);
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

        return FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(xmlReader));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(@Nonnull final File file) {
        return this.loadConfigurationFromXml(file, StandardCharsets.UTF_8);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(
            @Nonnull final InputStream stream, @Nonnull final Charset encoding) {

        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(stream, encoding.name());

        } catch (final XMLStreamException e) {
            LOG.error("While using stream for loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", stream);

            return CompletableFuture.completedFuture(List.of());
        }

        return FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(xmlReader));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(@Nonnull final InputStream stream) {
        return this.loadConfigurationFromXml(stream, StandardCharsets.UTF_8);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage", "UnusedReturnValue"})
    public FluentFuture<List<CommitInfo>> writeConfigurationDataFrom(@Nonnull final XMLStreamReader xmlReader) {
        return this.writeData(LogicalDatastoreType.CONFIGURATION, xmlReader);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage", "UnusedReturnValue"})
    public FluentFuture<List<CommitInfo>> writeOperationalDataFrom(@Nonnull final XMLStreamReader xmlReader) {
        return this.writeData(LogicalDatastoreType.OPERATIONAL, xmlReader);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> writeData(
            @Nonnull final LogicalDatastoreType storeType, @Nonnull final XMLStreamReader xmlReader) {

        @Nonnull final var input = this.createYangXmlDataInputUsing(xmlReader);
        @Nonnull final var dataNodes = new ArrayList<NormalizedNode>();
        try {
            while (input.nextYangTree()) {
                @Nonnull final var nodeResult = new NormalizedNodeResult();
                @Nonnull final var nodeWriter = ImmutableNormalizedNodeStreamWriter.from(nodeResult);

                @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var parser = XmlParserStream.create(
                        nodeWriter, new EmptyMountPointContext(this.context), input.getYangTreeNode().getPath().asAbsolute(),
                        false); // TODO: Add strictParsing to method params

                parser.parse(input);
                dataNodes.add(nodeResult.getResult());
            }

        } catch (final IllegalArgumentException | IOException | SAXException | URISyntaxException | XMLStreamException e) {
            LOG.error("While parsing from XML Data input: ", e);
            LOG.error("Failed parsing XML Data from: " + xmlReader);

        } catch (YangXmlDataInput.EndOfDocument ignored) {}

        return FluentFuture.from(Futures.allAsList(StreamEx.of(dataNodes).map(node -> this.writeData(storeType, node))));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeOperationalData(@Nonnull final NormalizedNode ...nodes) {
        return this.writeData(LogicalDatastoreType.OPERATIONAL, nodes);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeOperationalData(
            @Nonnull final Collection<? extends NormalizedNode> nodes) {

        return this.writeData(LogicalDatastoreType.OPERATIONAL, nodes);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeConfigurationData(@Nonnull final NormalizedNode ...nodes) {
        return this.writeData(LogicalDatastoreType.CONFIGURATION, nodes);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeConfigurationData(
            @Nonnull final Collection<? extends NormalizedNode> nodes) {

        return this.writeData(LogicalDatastoreType.CONFIGURATION, nodes);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeData(
            @Nonnull final LogicalDatastoreType storeType,
            @Nonnull final NormalizedNode ...nodes) {

        return this.writeData(storeType, List.of(nodes));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeData(
            @Nonnull final LogicalDatastoreType storeType,
            @Nonnull final Collection<? extends NormalizedNode> nodes) {

        return FluentFuture.from(FutureConverter.toListenableFuture(this.awaitYangBindingRegistering()))
                .transformAsync(ignoredRegisteredBinding -> {
                    @Nonnull final var txn = this.getNormalizedNodeBroker().newWriteOnlyTransaction();

                    @Nonnull final var yangPaths = StreamEx.of(nodes)
                            .mapToEntry(node -> YangInstanceIdentifier.create(node.getIdentifier()), Function.identity())
                            .mapKeyValue((yangPath, node) -> {
                                txn.merge(storeType, yangPath, node);
                                return yangPath;

                            }).toImmutableList();

                    LOG.info("Writing to {} Datastore: {}", storeType, yangPaths);

                    @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var committingFuture = txn.commit();
                    committingFuture.addCallback(this.datastore.injectWriting().of(storeType, yangPaths).futureCallback,
                            this.loggingCallbackExecutor);

                    return committingFuture;

                }, this.executor);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeOperationalDataFrom(@Nonnull final YangBinding<?, ?> ...bindings) {
        return this.writeBindingData(LogicalDatastoreType.OPERATIONAL, List.of(bindings));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public // <Y extends ChildOf<?>, B extends Builder<Y>>
    FluentFuture<? extends CommitInfo> writeOperationalDataFrom(
            @Nonnull final Collection<? extends YangBinding<?, ?>> bindings) {

        return this.writeBindingData(LogicalDatastoreType.OPERATIONAL, bindings);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeConfigurationDataFrom(@Nonnull final YangBinding<?, ?> ...bindings) {
        return this.writeBindingData(LogicalDatastoreType.CONFIGURATION, List.of(bindings));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public // <Y extends ChildOf<?>, B extends Builder<Y>>
    FluentFuture<? extends CommitInfo> writeConfigurationDataFrom(
            @Nonnull final Collection<? extends YangBinding<?, ?>> bindings) {

        return this.writeBindingData(LogicalDatastoreType.CONFIGURATION, bindings);
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeBindingData(
            @Nonnull final LogicalDatastoreType storeType,
            @Nonnull final YangBinding<?, ?> ...bindings) {

        return this.writeBindingData(storeType, List.of(bindings));
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<? extends CommitInfo> writeBindingData(
            @Nonnull final LogicalDatastoreType storeType,
            @Nonnull final Collection<? extends YangBinding<?, ?>> bindings) {

        @Nonnull final var txn = this.broker.newWriteOnlyTransaction();
        @Nonnull final var yangModeledPaths = StreamEx.of(bindings)
                .mapToEntry(YangBinding::getIid, binding -> this.mergeBindingDataIntoWriteTransaction(txn, storeType, binding))
                .filter(data -> data.getValue().isPresent())
                .keys().toImmutableList();

        @Nonnull final var writing = this.datastore.injectModeledWriting().of(storeType, yangModeledPaths);
        LOG.info("Writing to {} Datastore: {}", storeType, yangModeledPaths);

        @Nonnull final var future = txn.commit();
        future.addCallback(writing.futureCallback, this.loggingCallbackExecutor);
        return future;
    }

    @Nonnull
    private <Y extends ChildOf<?>, B extends Builder<Y>>
    YangData<Y> mergeBindingDataIntoWriteTransaction(@Nonnull final WriteTransaction txn, @Nonnull final LogicalDatastoreType storeType, @Nonnull final YangBinding<Y, B> binding) {
        @Nonnull final var futureData = (storeType == LogicalDatastoreType.CONFIGURATION) ?
                binding.provideConfigurationData() : binding.provideOperationalData();

        try {
            @Nonnull final YangData<Y> data = futureData.get();
            if (data.isPresent()) {
                txn.merge(storeType, binding.getIid(), data.get());
            }

            return data;

        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Failed {} Data provisioning from {}", storeType, binding, e.getCause());

            return YangData.empty();
        }
    }

    /*
    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public // <Y extends ChildOf<?>, B extends Builder<Y>>
    FluentFuture<? extends CommitInfo> writeData(
            @Nonnull final LogicalDatastoreType storeType,
            @Nonnull final Map<? extends InstanceIdentifier<?>, ? extends ListenableFuture<YangData<?>>> modeledNodes) {

        @Nonnull final var txn = this.broker.newWriteOnlyTransaction();

        @Nonnull final var yangModeledPaths = EntryStream.of(modeledNodes)
        // .mapToEntry(binding -> binding.getIidBuilder().build(), Function.identity())
                .mapKeyValue((yangModeledPath, futureData) -> {
                    @Nullable final YangData<?> data;
                    try {
                        this.addDataToTransaction(txn, storeType, yangModeledPath, (YangData<?>) futureData.get().get());
                        data = futureData.get();

                    } catch (final InterruptedException | ExecutionException e) {
                        // data = YangData.empty();

                        LOG.error("Failed reading {} Data from {}", storeType, binding);
                    }

                    if (data.isPresent()) {
                        this.addDataToTransaction(txn, storeType, yangModeledPath, data.get());
                    }

                    return yangModeledPath;

                }).toImmutableList();

        @Nonnull final var writing = this.datastore.injectModeledWriting().of(storeType, StreamEx.of(bindings)
                .map(binding -> binding.getIidBuilder().build())
                .toImmutableList());

        // @Nonnull final var future = writing.transactor.apply(this.broker, storeType, bindings);

        future.addCallback(writing.futureCallback, this.loggingCallbackExecutor);
        return future;
    }
    */

    public <Y extends ChildOf<?>, B extends Builder<Y>>
    void deleteOperationalDataOf(@Nonnull final YangBinding<Y, B> object) {
        this.deleteData(LogicalDatastoreType.OPERATIONAL, object);
    }

    public <Y extends ChildOf<?>, B extends Builder<Y>>
    void deleteConfigurationDataOf(@Nonnull final YangBinding<Y, B> object) {
        this.deleteData(LogicalDatastoreType.CONFIGURATION, object);
    }

    @SuppressWarnings({"UnstableApiUsage"})
    public <Y extends ChildOf<?>, B extends Builder<Y>>
    void deleteData(@Nonnull final LogicalDatastoreType storeType, @Nonnull final YangBinding<Y, B> object) {
        @Nonnull final var iid = object.getIidBuilder().build();
        @Nonnull final var txn = this.broker.newWriteOnlyTransaction();
        txn.delete(storeType, iid);

        LOG.info("Deleting from {} Datastore: {}", storeType, iid);
        Futures.addCallback(txn.commit(), new FutureCallback<CommitInfo>() {

            @Override @SuppressWarnings({"UnstableApiUsage"})
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info("TODO: {}", result);
            }

            @Override
            public void onFailure(@Nonnull final Throwable t) {
                LOG.error("While deleting from {} Datastore: ", storeType, t);
                LOG.error("Failed deleting from {} Datastore: {}", storeType, iid);
            }

        }, this.loggingCallbackExecutor);
    }
}
