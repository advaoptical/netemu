package com.adva.netemu;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

// import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.xml.sax.SAXException;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;

// import org.opendaylight.yangtools.concepts.Builder;
// import org.opendaylight.yangtools.rfc8528.data.util.EmptyMountPointContext;
import org.opendaylight.yangtools.yang.binding.ChildOf;
// import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

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
@Slf4j @SuppressWarnings({"UnstableApiUsage"})
public class YangPool extends SplitLayout implements EffectiveModelContextProvider,
        SchemaSourceProvider<YangTextSchemaSource> {

    /** Provider of tools for reading XML data.
      */
    @NonNull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    /** Default executor for asynchronous operations, including OpenDaylight datastore transactions.
     */
    @NonNull
    private final Executor executor;

    @NonNull
    public Executor executor() {
        return this.executor;
    }

    /** Default executor for asynchronous OpenDaylight datastore transactions.
      */
    @NonNull
    private final ExecutorService transactionExecutor;

    /** Default executor for logging results of asynchronous OpenDaylight datastore transactions.
     */
    // @NonNull
    // private final Executor loggingCallbackExecutor = MoreExecutors.directExecutor();

    /** Unique ID of this YANG pool.
      */
    @NonNull
    private final String id;

    /** Returns the unique, non-changeable ID of this YANG pool.

      * @return
            The ID string
      */
    @NonNull
    public String id() {
        return this.id;
    }

    /** Set of schemas defining this YANG pool's datastore structures.
      */
    @NonNull
    private final Set<YangModuleInfo> modules;

    /** Returns the schemas of this YANG pool.

      * @return
            An immutable set
      */
    @NonNull
    public Set<YangModuleInfo> getModules() {
        return this.modules;
    }

    /** Returns the source of YANG module specified by given identifier.

      * This is an asynchronous operation!

      * <p> NETEMU prefers {@link CompletableFuture} over the {@link ListenableFuture} returned by this overridden OpenDaylight
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
            A unique YANG module identification by name and optional revision. Default implementations is
            {@link org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier}

      * @return
            The readable YANG module source. This is an asynchronous result!
      */
    @NonNull @Override
    public ListenableFuture<? extends YangTextSchemaSource> getSource(@NonNull final SourceIdentifier identifier) {

        return Futures.submit(() -> StreamEx.of(this.modules).findFirst(module -> {
            @NonNull final var qName = module.getName();
            return qName.getLocalName().equals(identifier.name().getLocalName())
                    && qName.getRevision().equals(Optional.ofNullable(identifier.revision()));

        }).map(module -> YangTextSchemaSource.delegateForByteSource(identifier, module.getYangTextByteSource())).orElseThrow(() ->
                new NoSuchElementException(identifier.toString())), this.executor);
    }

    /** Model context of this YANG pool.
      */
    @NonNull
    private final EffectiveModelContext context;

    @Deprecated @NonNull
    public EffectiveModelContext getYangContext() {
        return this.context; // .getSchemaContext();
    }

    /** Returns the model context of this YANG pool.

      * @return The immutable YANG model context
      */
    @NonNull @Override
    public EffectiveModelContext getEffectiveModelContext() {
        return this.context;

        /*
        return this.context.tryToCreateModelContext().orElseThrow(() -> new RuntimeException(String.format(
                "Failed to create instance of %s from %s", EffectiveModelContext.class, this.context)));
        */
    }

    @NonNull
    private final XmlCodecFactory xmlCodecFactory;

    /** Datastore of this YANG pool.
      */
    @NonNull
    private final YangDatastore datastore = DaggerYangDatastore.create();

    @NonNull
    private final InMemoryDOMDataStore configurationStore;

    @NonNull
    private final InMemoryDOMDataStore operationalStore;

    @NonNull
    private final CurrentAdapterSerializer serializer;

    @NonNull
    public CurrentAdapterSerializer serializer() {
        return this.serializer;
    }

    @NonNull
    private final BindingDOMDataBrokerAdapter broker;

    @NonNull
    public BindingDOMDataBrokerAdapter getDataObjectBroker() {
        return this.broker;
    }

    public static class NormalizedNodeBroker extends SerializedDOMDataBroker {

        @NonNull
        private final YangPool yangPool;

        private NormalizedNodeBroker(@NonNull final YangPool yangPool) {
            super(Map.of(
                    LogicalDatastoreType.CONFIGURATION, yangPool.configurationStore,
                    LogicalDatastoreType.OPERATIONAL, yangPool.operationalStore

            ), MoreExecutors.listeningDecorator(yangPool.transactionExecutor));

            this.yangPool = yangPool;
        }

        @NonNull @Override
        public DOMTransactionChain createTransactionChain(@NonNull final DOMTransactionChainListener listener) {
            LOG.info("Updating {} Datastore from {} Datastore", LogicalDatastoreType.OPERATIONAL,
                    LogicalDatastoreType.CONFIGURATION);

            @NonNull final var updatingFuture = this.yangPool.readConfigurationData().transformAsync(config -> Futures
                    .allAsList(config.map(data -> StreamEx.of(((ContainerNode) data).body())).orElseGet(StreamEx::of)
                            .map(childNode -> (ListenableFuture<? extends CommitInfo>) this.yangPool
                                    .writeOperationalData(childNode))), this.yangPool.executor);

            @NonNull final var yangBindingRegistry = this.yangPool.yangBindingRegistry;
            try {
                synchronized (yangBindingRegistry) {
                    LOG.info("Updating {} Datastore from {} registered bindings", LogicalDatastoreType.OPERATIONAL,
                            yangBindingRegistry.size());

                    updatingFuture.transformAsync(ignoredCommitInfos -> this.yangPool
                            .writeOperationalDataFrom(yangBindingRegistry), this.yangPool.executor).get();
                }

            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Failed updating {} Datastore from registered bindings", LogicalDatastoreType.OPERATIONAL,
                        e.getCause());
            }

            return super.createTransactionChain(listener);
        }
    }

    @NonNull
    private final NormalizedNodeBroker normalizedNodeBroker;

    @NonNull
    public NormalizedNodeBroker getNormalizedNodeBroker() {
        return this.normalizedNodeBroker;
    }

    @NonNull
    private final List<YangBinding<?, ?>> yangBindingRegistry = Collections.synchronizedList(new ArrayList<>());

    @NonNull
    private final List<YangBindable> yangBindableRegistry = Collections.synchronizedList(new ArrayList<>());

    @NonNull
    private final List<YangListBindable> yangListBindableRegistry = Collections.synchronizedList(new ArrayList<>());

    @NonNull
    private final AtomicReference<NetEmu> netEmu = new AtomicReference<>();

    @NonNull
    public Optional<NetEmu> getNetEmu() {
        return Optional.ofNullable(this.netEmu.get());
    }

    @NonNull
    public NetEmu requireNetEmu() {
        return this.getNetEmu().orElseThrow(() -> new NoSuchElementException(String.format("%s has no %s instance attached",
                this, NetEmu.class)));
    }

    public void setNetEmu(@NonNull final NetEmu netEmu) {
        this.netEmu.set(netEmu);
    }

    public YangPool(@NonNull final String id, @NonNull final YangModuleInfo... modules) {
        this(id, List.of(modules));
    }

    public YangPool(@NonNull final String id, @NonNull final Collection<YangModuleInfo> modules) {
        this.id = Objects.requireNonNull(id);
        this.modules = Set.copyOf(Objects.requireNonNull(modules));
        this.context = BindingRuntimeHelpers.createEffectiveModel(this.modules);
        // this.context.addModuleInfos(this.modules);

        // Custom thread naming via factory was adapted from https://stackoverflow.com/a/9748697
        this.executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(id + "-thread-%d").build());

        this.transactionExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder()
                .setNameFormat(id + "-transaction-thread-%d")
                .build());

        this.xmlCodecFactory = XmlCodecFactory.create(this.context);

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

        @NonNull final var runtimeContext = BindingRuntimeHelpers.createRuntimeContext(
                StreamEx.of(this.modules).map(YangModuleInfo::getClass).toArray(Class[]::new));

        @NonNull final var adapterSerializer = this.serializer = new CurrentAdapterSerializer(new BindingCodecContext(
                runtimeContext));

        @NonNull final var adapterContext = new AdapterContext() {

            @NonNull @Override
            public CurrentAdapterSerializer currentSerializer() {
                return adapterSerializer;
            }
        };

        this.normalizedNodeBroker = new NormalizedNodeBroker(this);
        this.broker = new BindingDOMDataBrokerAdapter(adapterContext, this.normalizedNodeBroker);

        /*
                new BindingToNormalizedNodeCodec(this.context, new BindingNormalizedNodeCodecRegistry(
                        BindingRuntimeContext.create(this.context, this.getYangContext()))));
        */

        // @NonNull final var rpcService = new BindingDOMRpcProviderServiceAdapter(adapterContext, )

        super.setOrientation(Orientation.HORIZONTAL);
        super.setSizeFull();

        @NonNull final var configurationDataUiTreeGrid = new TreeGrid<NormalizedNode>();
        configurationDataUiTreeGrid.setSizeFull();

        @NonNull final var operationalDataUiTreeGrid = new TreeGrid<NormalizedNode>();
        operationalDataUiTreeGrid.setSizeFull();

        @NonNull final var uiRefreshButton = new Button("Refresh");

        @NonNull final var uiToolBar = new HorizontalLayout();

        super.addToPrimary(configurationDataUiTreeGrid);
    }

    @NonNull
    private final Map<LogicalDatastoreType, Boolean> datastoreBindingsEnabled = Collections.synchronizedMap(new HashMap<>(Map
            .of(LogicalDatastoreType.CONFIGURATION, true, LogicalDatastoreType.OPERATIONAL, true)));

    public boolean datastoreBindingsEnabled(@NonNull final LogicalDatastoreType storeType) {
        return this.datastoreBindingsEnabled.get(storeType);
    }

    public boolean datastoreBindingsDisabled(@NonNull final LogicalDatastoreType storeType) {
        return !this.datastoreBindingsEnabled(storeType);
    }

    @NonNull
    private final AtomicReference<CompletableFuture<? extends YangBinding<?, ?>>> yangBindingRegisteringFuture =
            new AtomicReference<>(CompletableFuture.completedFuture(null));

    @NonNull
    public CompletableFuture<? extends YangBinding<?, ?>> awaitYangBindingRegistering() {
        return this.yangBindingRegisteringFuture.get();
    }

    @NonNull
    public <T extends YangBinding<Y, B>, Y extends ChildOf<?>, B extends YangBuilder<Y>>
    CompletableFuture<T> registerYangBinding(@NonNull final T binding) {
        binding.setYangPool(this);

        synchronized (this.yangBindingRegisteringFuture) {
            @NonNull final var futureBinding = this.yangBindingRegisteringFuture.get()
                    .thenApplyAsync(ignoredRegisteredBinding -> {
                        LOG.info("Registering {}", binding);

                        for (@NonNull final YangBinding<Y, B>.DatastoreBinding storeBinding : List.of(
                                binding.createConfigurationDatastoreBinding(this),
                                binding.createOperationalDatastoreBinding(this))) {

                            LOG.debug("Registering {} Change listener for {}", storeBinding.storeType(), binding);
                            this.broker.registerDataTreeChangeListener(storeBinding.getDataTreeId(), storeBinding);
                        }

                        this.yangBindingRegistry.add(binding);
                        return binding;

                    }, this.executor);

            this.yangBindingRegisteringFuture.set(futureBinding);
            return futureBinding;
        }
    }

    @NonNull
    public <T extends YangBindable> T registerYangBindable(@NonNull final T object) {
        object.getYangBinding().ifPresent(this::registerYangBinding);
        this.yangBindableRegistry.add(object);
        return object;
    }

    @NonNull
    public <T extends YangListBindable> T registerYangBindable(@NonNull final T object) {
        object.getYangListBinding().ifPresent(this::registerYangBinding);
        this.yangListBindableRegistry.add(object);
        return object;
    }

    @NonNull
    public <T> Optional<T> findRegisteredInstanceOf(@NonNull final Class<T> registreeClass) {
        return StreamEx.of(this.yangBindableRegistry).findFirst(registreeClass::isInstance)
                .map(registreeClass::cast)
                .or(() -> StreamEx.of(this.yangListBindableRegistry).findFirst(registreeClass::isInstance)
                        .map(registreeClass::cast));
    }

    @NonNull
    public Optional<Object> findRegisteredInstanceOf(@NonNull final String registreeClassName) {
        return StreamEx.of(this.yangBindableRegistry)
                .findFirst(object -> object.getClass().getCanonicalName().equals(registreeClassName))
                .map(Object.class::cast)
                .or(() -> StreamEx.of(this.yangListBindableRegistry)
                        .findFirst(object -> object.getClass().getCanonicalName().equals(registreeClassName))
                        .map(Object.class::cast));
    }

    @NonNull
    private final
    Map<SchemaNodeIdentifier, BiFunction<SchemaNodeIdentifier, javax.xml.namespace.QName, javax.xml.namespace.QName>>
    xmlDataInputElementTagProcessorRegistry =

            Collections.synchronizedMap(new HashMap<>());

    public void registerXmlDataInputElementTagProcessor(
            @NonNull final String yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTagProcessor(
            @NonNull final String[] yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTagProcessor(
            @NonNull final QName[] yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTagProcessor(
            @NonNull final SchemaNodeIdentifier yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, javax.xml.namespace.QName, javax.xml.namespace.QName> processor) {

        this.xmlDataInputElementTagProcessorRegistry.put(yangPath, processor);
    }

    @NonNull
    private final
    Map<SchemaNodeIdentifier, BiFunction<SchemaNodeIdentifier, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>>
    xmlDataInputElementNamespaceProcessorRegistry =

            Collections.synchronizedMap(new HashMap<>());

    public void registerXmlDataInputElementNamespaceProcessor(
            @NonNull final String yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>
                    processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementNamespaceProcessor(
            @NonNull final String[] yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>
                    processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementNamespaceProcessor(
            @NonNull final QName[] yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>
                    processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementNamespaceProcessor(
            @NonNull final SchemaNodeIdentifier yangPath,
            @NonNull final BiFunction<SchemaNodeIdentifier, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>
                    processor) {

        this.xmlDataInputElementNamespaceProcessorRegistry.put(yangPath, processor);
    }

    @NonNull
    private final Map<SchemaNodeIdentifier, BiFunction<SchemaNodeIdentifier, String, String>>
            xmlDataInputElementTextProcessorRegistry = Collections.synchronizedMap(new HashMap<>());

    public void registerXmlDataInputElementTextProcessor(
            @NonNull final String yangPath, @NonNull final BiFunction<SchemaNodeIdentifier, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTextProcessor(
            @NonNull final String[] yangPath, @NonNull final BiFunction<SchemaNodeIdentifier, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTextProcessor(
            @NonNull final QName[] yangPath, @NonNull final BiFunction<SchemaNodeIdentifier, String, String> processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(Yang.absolutePathFrom(this.getYangContext(), yangPath), processor);
    }

    public void registerXmlDataInputElementTextProcessor(
            @NonNull final SchemaNodeIdentifier yangPath, @NonNull final BiFunction<SchemaNodeIdentifier, String, String>
            processor) {

        this.xmlDataInputElementTextProcessorRegistry.put(yangPath, processor);
    }

    private YangXmlDataInput createYangXmlDataInputUsing(@NonNull final XMLStreamReader reader) {
        return YangXmlDataInput.using(
                reader, this.getYangContext(),
                Map.copyOf(this.xmlDataInputElementTagProcessorRegistry),
                Map.copyOf(this.xmlDataInputElementNamespaceProcessorRegistry),
                Map.copyOf(this.xmlDataInputElementTextProcessorRegistry));
    }

    @NonNull
    public CompletableFuture</*? extends List<*/? extends CommitInfo> updateConfigurationData() {
        synchronized (this.yangBindingRegistry) {
            LOG.info("Updating {} Datastore from {} registered bindings", LogicalDatastoreType.CONFIGURATION,
                    this.yangBindingRegistry.size());

            return FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(this.yangBindingRegistry));
            /* Futures.allAsList(StreamEx.of(this.yangBindingRegistry)
                    .map(this::writeConfigurationDataFrom))); */
        }
    }

    @NonNull
    public CompletableFuture</*? extends List<*/? extends CommitInfo> updateOperationalData() {
        synchronized (this.yangBindingRegistry) {
            LOG.info("Updating {} Datastore from {} registered bindings", LogicalDatastoreType.OPERATIONAL,
                    this.yangBindingRegistry.size());

            return FutureConverter.toCompletableFuture(this.writeOperationalDataFrom(this.yangBindingRegistry));
            /* Futures.allAsList(StreamEx.of(this.yangBindingRegistry)
                    .map(this::writeOperationalDataFrom))); */
        }
    }

    @NonNull
    public FluentFuture<Optional<NormalizedNode>> readConfigurationData() {
        return this.readData(LogicalDatastoreType.CONFIGURATION);
    }

    @NonNull
    public FluentFuture<Optional<NormalizedNode>> readOperationalData() {
        return this.readData(LogicalDatastoreType.OPERATIONAL);
    }

    @NonNull
    public FluentFuture<Optional<NormalizedNode>> readData(@NonNull final LogicalDatastoreType storeType) {
        @NonNull final ListenableFuture<? extends CommitInfo> updatingFuture;

        if (storeType == LogicalDatastoreType.OPERATIONAL) {
            updatingFuture = this.readConfigurationData().transformAsync(config -> {
                LOG.info("Updating {} Datastore from {} Datastore", storeType, LogicalDatastoreType.CONFIGURATION);

                return FluentFuture.from(config
                        .map(data -> Futures.allAsList(StreamEx.of(((ContainerNode) data).body())
                                .map(childNode -> (ListenableFuture<? extends CommitInfo>) this.writeOperationalData(childNode))))

                        .orElseGet(() -> Futures.immediateFuture(List.of())));

            }, this.executor).transformAsync(ignoredCommitInfo -> {
                synchronized (this.yangBindingRegistry) {
                    LOG.info("Updating {} Datastore from {} registered bindings", storeType, this.yangBindingRegistry.size());

                    return this.writeOperationalDataFrom(this.yangBindingRegistry);
                }

            }, this.executor);

        } else {
            updatingFuture = Futures.transformAsync(FutureConverter.toListenableFuture(this.awaitYangBindingRegistering()),
                    ignoredRegisteredBinding -> {
                        synchronized (this.yangBindingRegistry) {
                            LOG.info("Updating {} Datastore from {} registered bindings", LogicalDatastoreType.CONFIGURATION,
                                    this.yangBindingRegistry.size());

                            return this.writeConfigurationDataFrom(this.yangBindingRegistry);
                        }

                    }, this.executor);
        }

        return FluentFuture.from(updatingFuture).transformAsync(ignoredCommitInfos -> {
            @NonNull final var txn = this.getNormalizedNodeBroker().newReadOnlyTransaction();
            @NonNull final var readingFuture = txn.read(storeType, YangInstanceIdentifier.empty());

            LOG.info("Reading from {} Datastore", storeType);
            readingFuture.addCallback(this.datastore.injectReading().of(storeType).futureCallback, this.executor);
            return readingFuture;

        }, this.executor);
    }

    @NonNull
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(
            @NonNull final File file, @NonNull final Charset encoding) {

        @NonNull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new FileReader(file, encoding));

        } catch (final IOException | XMLStreamException e) {
            LOG.error("While opening file for loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", file);

            return CompletableFuture.completedFuture(List.of());
        }

        return FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(xmlReader));

        /*
        return CompletableFuture.runAsync(() -> {
            this.datastoreBindingsEnabled.put(LogicalDatastoreType.CONFIGURATION, false);

        }).thenComposeAsync(ignoredVoid -> FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(xmlReader)))
                .handleAsync((commitInfos, e) -> {
                    this.datastoreBindingsEnabled.put(LogicalDatastoreType.CONFIGURATION, true);
                    if (e == null) {
                        return commitInfos;
                    }

                    throw new RuntimeException(e);
                });
        */
    }

    @NonNull
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(@NonNull final File file) {
        return this.loadConfigurationFromXml(file, StandardCharsets.UTF_8);
    }

    @NonNull
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(
            @NonNull final InputStream stream,
            @NonNull final Charset encoding) {

        @NonNull final XMLStreamReader xmlReader;
        try {
            /*  W/o InputStreamReader wrapper, the following can happen (especially w/resource streams) - e.g. due to BOMs:
                com.sun.org.apache.xerces.internal.xni.XNIException: Content is not allowed in prolog.
            */
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new InputStreamReader(stream, encoding));

        } catch (final XMLStreamException e) {
            LOG.error("While using stream for loading XML Configuration: ", e);
            LOG.error("Failed reading XML Configuration from: {}", stream);

            return CompletableFuture.completedFuture(List.of());
        }

        return FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(xmlReader));

        /*
        return CompletableFuture.runAsync(() -> {
            this.datastoreBindingsEnabled.put(LogicalDatastoreType.CONFIGURATION, false);

        }).thenComposeAsync(ignoredVoid -> FutureConverter.toCompletableFuture(this.writeConfigurationDataFrom(xmlReader)))
                .handleAsync((commitInfos, e) -> {
                    this.datastoreBindingsEnabled.put(LogicalDatastoreType.CONFIGURATION, true);
                    if (e == null) {
                        return commitInfos;
                    }

                    throw new RuntimeException(e);
                });
        */
    }

    @NonNull
    public CompletableFuture<List<CommitInfo>> loadConfigurationFromXml(@NonNull final InputStream stream) {
        return this.loadConfigurationFromXml(stream, StandardCharsets.UTF_8);
    }

    @NonNull
    public FluentFuture<List<CommitInfo>> writeConfigurationDataFrom(@NonNull final XMLStreamReader xmlReader) {
        return this.writeData(LogicalDatastoreType.CONFIGURATION, xmlReader);
    }

    @NonNull
    public FluentFuture<List<CommitInfo>> writeOperationalDataFrom(@NonNull final XMLStreamReader xmlReader) {
        return this.writeData(LogicalDatastoreType.OPERATIONAL, xmlReader);
    }

    @NonNull
    public FluentFuture<List<CommitInfo>> writeData(
            @NonNull final LogicalDatastoreType storeType, @NonNull final XMLStreamReader xmlReader) {

        @NonNull final var input = this.createYangXmlDataInputUsing(xmlReader);
        @NonNull final var dataNodes = new ArrayList<NormalizedNode>();
        try {
            while (input.nextYangTree()) {
                @NonNull final var nodeResult = new NormalizedNodeResult();
                @NonNull final var nodeWriter = ImmutableNormalizedNodeStreamWriter.from(nodeResult);

                @NonNull final var parser = XmlParserStream.create(nodeWriter, this.xmlCodecFactory,
                        SchemaInferenceStack.Inference.ofDataTreePath(this.context, input.getYangTreeNode().getQName()),
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

    @NonNull
    public FluentFuture<? extends CommitInfo> writeOperationalData(@NonNull final NormalizedNode ...nodes) {
        return this.writeData(LogicalDatastoreType.OPERATIONAL, nodes);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeOperationalData(
            @NonNull final Collection<? extends NormalizedNode> nodes) {

        return this.writeData(LogicalDatastoreType.OPERATIONAL, nodes);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeConfigurationData(@NonNull final NormalizedNode ...nodes) {
        return this.writeData(LogicalDatastoreType.CONFIGURATION, nodes);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeConfigurationData(
            @NonNull final Collection<? extends NormalizedNode> nodes) {

        return this.writeData(LogicalDatastoreType.CONFIGURATION, nodes);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeData(
            @NonNull final LogicalDatastoreType storeType,
            @NonNull final NormalizedNode ...nodes) {

        return this.writeData(storeType, List.of(nodes));
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeData(
            @NonNull final LogicalDatastoreType storeType,
            @NonNull final Collection<? extends NormalizedNode> nodes) {

        return FluentFuture.from(FutureConverter.toListenableFuture(this.awaitYangBindingRegistering()))
                .transformAsync(ignoredRegisteredBinding -> {
                    @NonNull final var txn = this.getNormalizedNodeBroker().newWriteOnlyTransaction();

                    @NonNull final var yangPaths = StreamEx.of(nodes)
                            .mapToEntry(node -> YangInstanceIdentifier.create(node.getIdentifier()), Function.identity())
                            .mapKeyValue((yangPath, node) -> {
                                txn.merge(storeType, yangPath, node);
                                return yangPath;

                            }).toImmutableList();

                    LOG.info("Writing {} {} instances to {} Datastore", yangPaths.size(), NormalizedNode.class.getName(),
                            storeType);

                    LOG.debug("Writing to {} Datastore: {}", storeType, yangPaths);

                    @NonNull final var committingFuture = txn.commit();
                    committingFuture.addCallback(this.datastore.injectWriting().of(storeType, yangPaths).futureCallback,
                            this.executor);

                    return committingFuture;

                }, this.executor);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeOperationalDataFrom(@NonNull final YangBinding<?, ?> ...bindings) {
        return this.writeBindingData(LogicalDatastoreType.OPERATIONAL, List.of(bindings));
    }

    @NonNull
    public // <Y extends ChildOf<?>, B extends Builder<Y>>
    FluentFuture<? extends CommitInfo> writeOperationalDataFrom(
            @NonNull final Collection<? extends YangBinding<?, ?>> bindings) {

        return this.writeBindingData(LogicalDatastoreType.OPERATIONAL, bindings);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeConfigurationDataFrom(@NonNull final YangBinding<?, ?> ...bindings) {
        return this.writeBindingData(LogicalDatastoreType.CONFIGURATION, List.of(bindings));
    }

    @NonNull
    public // <Y extends ChildOf<?>, B extends Builder<Y>>
    FluentFuture<? extends CommitInfo> writeConfigurationDataFrom(
            @NonNull final Collection<? extends YangBinding<?, ?>> bindings) {

        return this.writeBindingData(LogicalDatastoreType.CONFIGURATION, bindings);
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeBindingData(
            @NonNull final LogicalDatastoreType storeType,
            @NonNull final YangBinding<?, ?> ...bindings) {

        return this.writeBindingData(storeType, List.of(bindings));
    }

    @NonNull
    public FluentFuture<? extends CommitInfo> writeBindingData(
            @NonNull final LogicalDatastoreType storeType,
            @NonNull final Collection<? extends YangBinding<?, ?>> bindings) {

        @NonNull final var txn = this.broker.newWriteOnlyTransaction();
        @NonNull final var yangModeledPaths = StreamEx.of(bindings)
                .mapToEntry(YangBinding::getIid, binding -> this.mergeBindingDataIntoWriteTransaction(txn, storeType, binding))
                .filter(data -> data.getValue().isPresent())
                .keys().toImmutableList();

        @NonNull final var writing = this.datastore.injectModeledWriting().of(storeType, yangModeledPaths);
        LOG.info("Writing {} {} instances to {} Datastore", yangModeledPaths.size(), DataObject.class.getName(), storeType);
        LOG.debug("Writing to {} Datastore: {}", storeType, yangModeledPaths);

        @NonNull final var future = txn.commit();
        future.addCallback(writing.futureCallback, this.executor);
        return future;
    }

    @NonNull
    private <Y extends ChildOf<?>, B extends YangBuilder<Y>>
    YangData<Y> mergeBindingDataIntoWriteTransaction(@NonNull final WriteTransaction txn, @NonNull final LogicalDatastoreType storeType, @NonNull final YangBinding<Y, B> binding) {
        @NonNull final var futureData = (storeType == LogicalDatastoreType.CONFIGURATION) ?
                binding.provideConfigurationData() : binding.provideOperationalData();

        try {
            @NonNull final YangData<Y> data = futureData.get();
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
    @NonNull
    public // <Y extends ChildOf<?>, B extends Builder<Y>>
    FluentFuture<? extends CommitInfo> writeData(
            @NonNull final LogicalDatastoreType storeType,
            @NonNull final Map<? extends InstanceIdentifier<?>, ? extends ListenableFuture<YangData<?>>> modeledNodes) {

        @NonNull final var txn = this.broker.newWriteOnlyTransaction();

        @NonNull final var yangModeledPaths = EntryStream.of(modeledNodes)
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

        @NonNull final var writing = this.datastore.injectModeledWriting().of(storeType, StreamEx.of(bindings)
                .map(binding -> binding.getIidBuilder().build())
                .toImmutableList());

        // @NonNull final var future = writing.transactor.apply(this.broker, storeType, bindings);

        future.addCallback(writing.futureCallback, this.loggingCallbackExecutor);
        return future;
    }
    */

    public <Y extends ChildOf<?>, B extends YangBuilder<Y>>
    void deleteOperationalDataOf(@NonNull final YangBinding<Y, B> object) {
        this.deleteData(LogicalDatastoreType.OPERATIONAL, object);
    }

    public <Y extends ChildOf<?>, B extends YangBuilder<Y>>
    void deleteConfigurationDataOf(@NonNull final YangBinding<Y, B> object) {
        this.deleteData(LogicalDatastoreType.CONFIGURATION, object);
    }

    public <Y extends ChildOf<?>, B extends YangBuilder<Y>>
    void deleteData(@NonNull final LogicalDatastoreType storeType, @NonNull final YangBinding<Y, B> object) {
        @NonNull final var iid = object.getIidBuilder().build();
        @NonNull final var txn = this.broker.newWriteOnlyTransaction();
        txn.delete(storeType, iid);

        LOG.info("Deleting from {} Datastore: {}", storeType, iid);
        Futures.addCallback(txn.commit(), new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(@Nullable final CommitInfo result) {
                LOG.info("TODO: {}", result);
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                LOG.error("While deleting from {} Datastore: ", storeType, t);
                LOG.error("Failed deleting from {} Datastore: {}", storeType, iid);
            }

        }, this.executor);
    }
}
