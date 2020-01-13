package com.adva.netemu;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema
        .ImmutableNormalizedNodeStreamWriter;

import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter
        .BindingToNormalizedNodeCodec;

import org.opendaylight.mdsal.binding.dom.codec.impl
        .BindingNormalizedNodeCodecRegistry;

import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import static org.opendaylight.mdsal.dom.store.inmemory
        .InMemoryDOMDataStoreConfigProperties
        .DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE;

import com.adva.netemu.datastore.DaggerYangDatastore;
import com.adva.netemu.datastore.YangDatastore;


public class YangPool {

    private static final Logger LOG = LoggerFactory.getLogger(YangPool.class);

    @Nonnull
    private final ScheduledThreadPoolExecutor _executor =
            new ScheduledThreadPoolExecutor(0);

    @Nonnull
    private final ImmutableSet<YangModuleInfo> _modules;

    @Nonnull
    public ImmutableSet<YangModuleInfo> getModules() {
        return this._modules;
    }

    @Nonnull
    private final ModuleInfoBackedContext _context;

    @Nonnull
    public SchemaContext getYangContext() {
        return this._context.getSchemaContext();
    }

    @Nonnull
    private final YangDatastore _datastore = DaggerYangDatastore.create();

    @Nonnull
    private final BindingDOMDataBrokerAdapter _broker;

    @Nonnull
    public DataBroker getDataBroker() {
        return this._broker;
    }

    @Nonnull
    private final SerializedDOMDataBroker _domBroker;

    @Nonnull
    public DOMDataBroker getDomDataBroker() {
        return this._domBroker;
    }

    @Nonnull
    private final InMemoryDOMDataStore _operStore;

    @Nonnull
    public DOMStore getOperationalDataStore() {
        return this._operStore;
    }

    @Nonnull
    private final InMemoryDOMDataStore _configStore;

    @Nonnull
    public DOMStore getConfigurationDataStore() {
        return this._configStore;
    }

    @Nonnull
    private final List<YangModeled<?, ?>> _yangModeledRegistry =
            Collections.synchronizedList(new ArrayList<>());

    public YangPool(
            @Nonnull final String id, final YangModuleInfo... modules) {

        this._modules = ImmutableSet.copyOf(modules);

        this._context = ModuleInfoBackedContext.create();
        this._context.addModuleInfos(this._modules);

        this._configStore = new InMemoryDOMDataStore(
                "netemu-" + id + "-config",
                LogicalDatastoreType.CONFIGURATION,
                this._executor,
                DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE,
                false);

        this._configStore.onGlobalContextUpdated(this.getYangContext());

        this._operStore = new InMemoryDOMDataStore(
                "netemu-" + id + "-oper",
                LogicalDatastoreType.OPERATIONAL,
                this._executor,
                DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE,
                false);

        this._operStore.onGlobalContextUpdated(this.getYangContext());

        final var stores = Map.of(
                LogicalDatastoreType.CONFIGURATION,
                (DOMStore) this._configStore,

                LogicalDatastoreType.OPERATIONAL,
                (DOMStore) this._operStore);

        this._domBroker = new SerializedDOMDataBroker(
                stores, MoreExecutors.listeningDecorator(this._executor));
                        // Executors.newScheduledThreadPool(0)));

        this._broker = new BindingDOMDataBrokerAdapter(
                this._domBroker, new BindingToNormalizedNodeCodec(
                        this._context, new BindingNormalizedNodeCodecRegistry(
                                BindingRuntimeContext.create(
                                        this._context,
                                        this.getYangContext()))));
    }

    @Nonnull
    public <T extends YangModeled<Y, B>,
            Y extends ChildOf,
            B extends Builder<Y>>

    T registerYangModeled(@Nonnull final T object) {
        final YangModeled<Y, B>.ConfigurationBinding binding =
                object.createConfigurationBinding();

        this._broker.registerDataTreeChangeListener(
                binding.getDataTreeId(), binding);

        this._yangModeledRegistry.add(object);
        return object;
    }

    @Nonnull
    public
    FluentFuture<Optional<NormalizedNode<?, ?>>> readConfigurationData() {
        return this.readData(LogicalDatastoreType.CONFIGURATION);
    }

    @Nonnull
    public
    FluentFuture<Optional<NormalizedNode<?, ?>>> readOperationalData() {
        return this.readData(LogicalDatastoreType.OPERATIONAL);
    }

    @Nonnull
    public FluentFuture<Optional<NormalizedNode<?, ?>>> readData(
            @Nonnull final LogicalDatastoreType storeType) {

        if (storeType == LogicalDatastoreType.OPERATIONAL) {
            synchronized (this._yangModeledRegistry) {
                for (final var object : this._yangModeledRegistry) {
                    this.writeOperationalDataFrom(object);
                }
            }
        }

        final var txn = this._domBroker.newReadOnlyTransaction();
        final var future = txn.read(
                storeType, YangInstanceIdentifier.empty());

        LOG.info("Reading from " + storeType + " Datastore");
        future.addCallback(
                this._datastore.injectReading().of(storeType).futureCallback,
                this._executor);

        return future;
    }

    public void writeConfigurationDataFrom(
            @Nonnull final XMLStreamReader xmlReader) {

        final var input = YangXmlDataInput.using(
                xmlReader, this.getYangContext());

        final var dataNodes = new ArrayList<NormalizedNode<?, ?>>();
        try {
            while (input.hasNext()) {
                input.nextYangTree();

                final var nodeResult = new NormalizedNodeResult();
                final var nodeWriter =
                        ImmutableNormalizedNodeStreamWriter.from(nodeResult);

                final var parser = XmlParserStream.create(
                        nodeWriter, this.getYangContext(),
                        input.getYangTreeNode(), true);

                parser.parse(input);
                dataNodes.add(nodeResult.getResult());
            }

        } catch (final
                IllegalArgumentException |
                IOException |
                SAXException |
                URISyntaxException |
                XMLStreamException e) {

            LOG.error("Cannot use XML Data input: " + xmlReader);
            e.printStackTrace();
            LOG.error("Failed parsing XML Data from: " + xmlReader);

        } catch (YangXmlDataInput.EndOfDocument ignored) {}

        for (final var node : dataNodes) {
            this.writeConfigurationData(node);
        }
    }

    public void writeOperationalData(
            @Nonnull final NormalizedNode<?, ?> node) {

        this.writeData(node, LogicalDatastoreType.OPERATIONAL);
    }

    public void writeConfigurationData(
            @Nonnull final NormalizedNode<?, ?> node) {

        this.writeData(node, LogicalDatastoreType.CONFIGURATION);
    }

    public void writeData(
            @Nonnull final NormalizedNode<?, ?> node,
            @Nonnull final LogicalDatastoreType storeType) {

        final var path = YangInstanceIdentifier.create(node.getIdentifier());
        final var txn = this._domBroker.newWriteOnlyTransaction();
        txn.put(storeType, path, node);

        LOG.info("Writing to " + storeType + " Datastore: " + path);
        Futures.addCallback(
                txn.commit(), new FutureCallback<CommitInfo>() {

                    @Override
                    public void onSuccess(@Nullable CommitInfo result) {
                        LOG.info(result.toString());
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failure.printStackTrace();
                        LOG.error("Failed writing to "
                                + storeType + " Datastore: " + path);
                    }

                }, this._executor);
    }

    public void writeOperationalDataFrom(@Nonnull final YangModeled object) {
        this.writeData(object, LogicalDatastoreType.OPERATIONAL);
    }

    public void writeConfigurationDataFrom(
            @Nonnull final YangModeled object) {

        this.writeData(object, LogicalDatastoreType.CONFIGURATION);
    }

    public void writeData(
            @Nonnull final YangModeled object,
            @Nonnull final LogicalDatastoreType storeType) {

        final var iid = object.getIidBuilder().build();
        final var txn = this._broker.newWriteOnlyTransaction();
        txn.put(storeType, iid, object.toYangData());

        LOG.info("Writing to " + storeType + " Datastore: " + iid);
        Futures.addCallback(
                txn.commit(), new FutureCallback<CommitInfo>() {

                    @Override
                    public void onSuccess(@Nullable CommitInfo result) {
                        LOG.info(result.toString());
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failure.printStackTrace();
                        LOG.error("Failed writing to "
                                + storeType + " Datastore: " + iid);
                    }

                }, this._executor);
    }

    public void deleteOperationalDataOf(@Nonnull final YangModeled object) {
        this.deleteData(object, LogicalDatastoreType.OPERATIONAL);
    }

    public void deleteConfigurationDataOf(@Nonnull final YangModeled object) {
        this.deleteData(object, LogicalDatastoreType.CONFIGURATION);
    }

    public void deleteData(
            @Nonnull final YangModeled object,
            @Nonnull final LogicalDatastoreType storeType) {

        final var iid = object.getIidBuilder().build();
        final var txn = this._broker.newWriteOnlyTransaction();
        txn.delete(storeType, iid);

        LOG.info("Deleting from " + storeType + " Datastore: " + iid);
        Futures.addCallback(
                txn.commit(), new FutureCallback<CommitInfo>() {

                    @Override
                    public void onSuccess(@Nullable CommitInfo result) {
                        LOG.info(result.toString());
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failure.printStackTrace();
                        LOG.error("Failed deleting from "
                                + storeType + " Datastore: " + iid);
                    }

                }, this._executor);
    }
}
