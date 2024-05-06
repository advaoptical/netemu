package com.adva.netemu.southbound;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.InetSocketAddress;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MountPointContext;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.YangInstanceIdentifierWriter;

import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableChoiceNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;

import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.spi.AbstractEffectiveModelContextProvider;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.CommitInfo;

import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.xml.XmlUtil;

import org.opendaylight.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;

import org.opendaylight.netconf.client.mdsal.impl.DefaultBaseNetconfSchemas;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformer;
import org.opendaylight.netconf.client.mdsal.impl.NetconfMessageTransformUtil;
import org.opendaylight.netconf.common.mdsal.NormalizedDataUtil;

// import org.opendaylight.netconf.nettyutil.TimedReconnectStrategyFactory;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.LoginPasswordHandler;

import com.adva.netemu.YangBindable;
import com.adva.netemu.YangData;
import com.adva.netemu.YangPool;
import com.adva.netemu.driver.EmuDriver;


@Slf4j @SuppressWarnings({"UnstableApiUsage"})
public class NetconfDriver extends EmuDriver {

    public static class Settings implements EmuDriver.Settings<NetconfDriver> {

        @Nonnull
        private String host = "localhost";

        @Nonnull
        public String getHost() {
            return this.host;
        }

        @Nonnull
        public Settings setHost(@Nonnull final String value) {
            this.host = value;
            return this;
        }

        @Nonnegative
        private int port = 830;

        @Nonnull
        public Integer getPort() {
            return this.port;
        }

        @Nonnull
        public Settings setPort(final int value) {
            this.port = value;
            return this;
        }

        @Nonnull
        private String user = "user";

        @Nonnull
        public String getUser() {
            return this.user;
        }

        @Nonnull
        public Settings setUser(@Nonnull final String value) {
            this.user = value;
            return this;
        }

        @Nonnull
        private String password = "password";

        @Nonnull
        public String getPassword() {
            return this.password;
        }

        @Nonnull
        public Settings setPassword(@Nonnull final String value) {
            this.password = value;
            return this;
        }
    }

    @Nonnull
    private static final GlobalEventExecutor EVENT_EXECUTOR = GlobalEventExecutor.INSTANCE;

    @Nonnull
    private static final NioEventLoopGroup EVENT_LOOP = new NioEventLoopGroup();

    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
    }

    @Nonnull
    private static final YangParserFactory YANG_PARSER_FACTORY = new DefaultYangParserFactory();

    @Nonnull
    private static final NetconfMessage EMPTY_NETCONF_MESSAGE;
    static {
        try {
            EMPTY_NETCONF_MESSAGE = new NetconfMessage(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()
                    .newDocument());

        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Failed creating empty NETCONF message for dry-run purposes", e);
        }
    }

    @Nullable
    private final InetSocketAddress address;

    @Nullable
    private final AuthenticationHandler authentication;

    @Nonnull
    private final NetconfMessageTransformer transformer;

    @Nullable
    private final NetconfClientSession session;

    @Nonnull
    private final SimpleNetconfClientSessionListener listener = new SimpleNetconfClientSessionListener();

    public NetconfDriver(
            @Nonnull final YangPool pool,
            @Nonnull final EmuDriver.Settings<NetconfDriver> settings,
            @Nonnull final Boolean dryRun) {

        super(pool, settings, dryRun);

        try {
            this.transformer = new NetconfMessageTransformer(
                    MountPointContext.of(pool.getEffectiveModelContext()), true, // true -> strict message parsing
                    new DefaultBaseNetconfSchemas(YANG_PARSER_FACTORY).getBaseSchemaWithNotifications());

        } catch (final YangParserException e) {
            throw new RuntimeException(String.format("Failed creating instance of %s", NetconfMessageTransformer.class), e);
        }

        if (dryRun) {
            this.address = null;
            this.authentication = null;
            this.session = null;
            return;
        }

        @Nonnull final var netconfSettings = (Settings) settings;
        this.address = new InetSocketAddress(netconfSettings.getHost(), netconfSettings.getPort());
        this.authentication = new LoginPasswordHandler(netconfSettings.getUser(), netconfSettings.getPassword());

        @Nonnull final var futureSession = new NetconfClientDispatcherImpl(EVENT_LOOP, EVENT_LOOP, new HashedWheelTimer())
                .createClient(NetconfClientConfigurationBuilder.create()
                        .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                        .withAddress(this.address)
                        .withAuthHandler(this.authentication)
                        // .withReconnectStrategy(new TimedReconnectStrategyFactory(EVENT_EXECUTOR, 5L, 0, BigDecimal.ONE)
                        //         .createReconnectStrategy())

                        // .withReconnectStrategy(new ReconnectImmediatelyStrategy(EVENT_EXECUTOR, 0))
                        .withSessionListener(this.listener)
                        .build());

        try {
            this.session = futureSession.get();

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed creating NETCONF client session", e);
        }
    }

    @Nonnull @Override
    public FluentFuture<List<CommitInfo>> fetchConfigurationData(@Nonnull final YangInstanceIdentifier iid) {
        return this.requestGetConfig();
    }

    @Nonnull
    public FluentFuture<List<CommitInfo>> fetchConfigurationDataFor(@Nonnull final YangBindable object) {
        return this.requestGetConfig();
    }

    @Nonnull
    public FluentFuture<List<CommitInfo>> requestGetConfig() {
        @Nonnull final var message = this.transformer.toRpcRequest(NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(NetconfMessageTransformUtil.NETCONF_CONFIG_NODEID)
                        .withChild(ImmutableContainerNodeBuilder.create()

                                .withNodeIdentifier(NetconfMessageTransformUtil.NETCONF_SOURCE_NODEID)
                                .withChild(ImmutableChoiceNodeBuilder.create()
                                        .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(QName
                                                .create(NormalizedDataUtil.NETCONF_QNAME, "config-source")))

                                        .withChild(ImmutableLeafNodeBuilder.createNode(
                                                YangInstanceIdentifier.NodeIdentifier
                                                        .create(NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME),

                                                Empty.value())).build()).build()).build());

        @Nonnull final var response = this.request(message);

        if (super.dryRun) {
            return FluentFuture.from(Futures.immediateFuture(List.of()));
        }

        LOG.info("Applying NETCONF response from {}@{}", this.authentication.getUsername(), this.address);
        try {
            return this.yangPool().writeConfigurationDataFrom(XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(
                    response.toString())));

            //this.transformer.toRpcResult(response, NetconfMessageTransformUtil.NETCONF_GET_PATH).getResult());

        } catch (final XMLStreamException e) {
            return FluentFuture.from(Futures.immediateFailedFuture(e));
        }
    }

    @Nonnull @Override
    public FluentFuture<List<CommitInfo>> fetchOperationalData(@Nonnull final YangInstanceIdentifier iid) {
        return this.requestGet();
    }

    @Nonnull
    public FluentFuture<List<CommitInfo>> fetchOperationalDataFor(@Nonnull final YangBindable object) {
        return this.requestGet();
    }

    @Nonnull
    public FluentFuture<List<CommitInfo>> requestGet() {
        @Nonnull final var message = this.transformer.toRpcRequest(
                NetconfMessageTransformUtil.NETCONF_GET_QNAME,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(NetconfMessageTransformUtil.NETCONF_DATA_NODEID)
                        .build());

        @Nonnull final var response = this.request(message);

        if (super.dryRun) {
            return FluentFuture.from(Futures.immediateFuture(List.of()));
        }

        LOG.info("Applying NETCONF response from {}@{}", this.authentication.getUsername(), this.address);
        try {
            return this.yangPool().writeOperationalDataFrom(XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(
                    response.toString())));

            //this.transformer.toRpcResult(response, NetconfMessageTransformUtil.NETCONF_GET_PATH).getResult());

        } catch (final XMLStreamException e) {
            return FluentFuture.from(Futures.immediateFailedFuture(e));
        }
    }

    /*
    @Nonnull
    public FluentFuture<List<CommitInfo>> pushConfigurationDataFrom(@Nonnull final YangBindable object) {
        return object.requireYangBinding().provideConfigurationData().transformAsync(data -> {
            @Nonnull final var futureCommitInfos = this.yangPool().writeConfigurationDataFrom(object.requireYangBinding());
            this.requestEditConfig(object.requireYangBinding().getIid(), data);

            return futureCommitInfos;
        }, this.executor);
    }
    */

    @Nonnull @Override
    public <Y extends DataObject> CompletableFuture<RpcError> pushConfigurationData(
            @Nonnull final InstanceIdentifier<Y> iid,
            @Nonnull final YangData<Y> data) {

        return CompletableFuture.supplyAsync(() -> data.map(dataObject -> {

            @Nonnull final var netconfMessage = this.transformer.toRpcRequest(
                    NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME,
                    ImmutableContainerNodeBuilder.create()
                            .withNodeIdentifier(NetconfMessageTransformUtil.NETCONF_CONFIG_NODEID)
                            .build());

            @Nonnull final var rpcDocument = netconfMessage.getDocument();
            @Nonnull final var editConfigElement = rpcDocument.getDocumentElement().getFirstChild();

            @Nonnull final var targetElement = XmlUtil.createElement(rpcDocument,
                    NetconfMessageTransformUtil.NETCONF_TARGET_QNAME.getLocalName(),
                    Optional.of(NetconfMessageTransformUtil.NETCONF_TARGET_QNAME.getNamespace().toString()));

            targetElement.appendChild(XmlUtil.createElement(rpcDocument,
                    NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME.getLocalName(),
                    Optional.of(NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME.getNamespace().toString())));

            @Nonnull final var configElement = XmlUtil.createElement(rpcDocument,
                    NetconfMessageTransformUtil.NETCONF_CONFIG_QNAME.getLocalName(),
                    Optional.of(NetconfMessageTransformUtil.NETCONF_CONFIG_QNAME.getNamespace().toString()));

            @Nonnull final var yangContext = super.yangPool().getEffectiveModelContext();
            @Nonnull final var yangSerializer = super.yangPool().serializer();
            @Nonnull final var yangNodeResult = (BindingNormalizedNodeSerializer.NodeResult)
                    yangSerializer.toNormalizedNode(iid, dataObject);

            try { /** Adapted from {@link NetconfMessageTransformUtil#createEditConfigAnyxml}
                */
                @Nonnull final var yangTreeResultHolder = new NormalizationResultHolder();

                try (@Nonnull final var yangStreamWriter = ImmutableNormalizedNodeStreamWriter.from(yangTreeResultHolder)) {
                    try (
                            @Nonnull final var yangPathWriter = YangInstanceIdentifierWriter.open(yangStreamWriter, yangContext,
                                    yangSerializer.toYangInstanceIdentifier(iid).coerceParent());

                            @Nonnull final var yangNodeWriter = NormalizedNodeWriter.forStreamWriter(yangStreamWriter)) {

                        yangNodeWriter.write(yangNodeResult.node());
                    }
                }

                NormalizedDataUtil.writeNormalizedNode(yangTreeResultHolder.getResult().data(), new DOMResult(configElement),
                        yangContext, null);

            } catch (final IOException | XMLStreamException e) {
                throw new RuntimeException(String.format("Failed serializing %s to NETCONF <config> tree",
                        yangNodeResult.node()), e);
            }

            editConfigElement.appendChild(targetElement);
            editConfigElement.appendChild(configElement);
            @Nonnull final var response = this.request(netconfMessage);

            if (!super.dryRun) {
                LOG.info("NETCONF response from {}@{}: {}", this.authentication.getUsername(), this.address, response);
            }

            return (RpcError) null;

        }).orElse(null)).exceptionallyAsync(e -> {
            if (super.dryRun) {
                LOG.error("Failed dry-run pushing CONFIGURATION Data: {}", iid, e);

            } else {
                LOG.error("Failed pushing CONFIGURATION Data to {}: {}", this.address, iid, e);
            }

            return (RpcError) null;
        });
    }

    @Nonnull
    public FluentFuture<List<CommitInfo>> requestEditConfig(@Nonnull final ContainerNode data) {
        this.request(this.transformer.toRpcRequest(NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME, data));
        return FluentFuture.from(Futures.immediateFuture(List.of()));
    }

    @Nonnull
    private NetconfMessage request(@Nonnull final NetconfMessage message) {
        if (super.dryRun) {
            LOG.info("Dry-run NETCONF request:\n{}", message);
            return EMPTY_NETCONF_MESSAGE;
        }

        LOG.info("Sending NETCONF request to {}@{}:\n{}", this.authentication.getUsername(), this.address, message);

        @Nonnull final var futureResponse = this.listener.sendRequest(message);
        @Nonnull final NetconfMessage response;
        try {
            response = Objects.requireNonNull(futureResponse.get(), () -> String.format("Received null response from %s",
                    this.listener));

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed sending NETCONF request or receiving response", e);
        }

        LOG.debug("Received NETCONF response from {}@{}:\n{}", this.authentication.getUsername(), this.address, response);
        return response;
    }
}
