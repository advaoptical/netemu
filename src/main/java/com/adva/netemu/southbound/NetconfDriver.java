package com.adva.netemu.southbound;

import java.io.StringReader;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.rcf8528.data.util.EmptyMountPointContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.impl.YangParserFactoryImpl;

import org.opendaylight.mdsal.common.api.CommitInfo;

import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.netconf.client.NetconfClientSession;
import org.opendaylight.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.netconf.client.conf.NetconfClientConfigurationBuilder;

import org.opendaylight.netconf.nettyutil.ReconnectImmediatelyStrategy;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.netconf.nettyutil.handler.ssh.authentication.LoginPasswordHandler;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.DefaultBaseNetconfSchemas;
import org.opendaylight.netconf.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.netconf.sal.connect.netconf.util.NetconfMessageTransformUtil;

import com.adva.netemu.YangBindable;
import com.adva.netemu.YangPool;
import com.adva.netemu.driver.EmuDriver;


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
    private static final Logger LOG = LoggerFactory.getLogger(NetconfDriver.class);

    @Nonnull
    private static final GlobalEventExecutor EVENT_EXECUTOR = GlobalEventExecutor.INSTANCE;

    @Nonnull
    private static final NioEventLoopGroup EVENT_LOOP = new NioEventLoopGroup();

    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    @Nonnull
    private static final YangParserFactory YANG_PARSER_FACTORY = new YangParserFactoryImpl();

    @Nonnull
    private final InetSocketAddress address;

    @Nonnull
    private final AuthenticationHandler authentication;

    @Nonnull
    private final NetconfMessageTransformer transformer;

    @Nonnull
    private final NetconfClientSession session;

    @Nonnull
    private final SimpleNetconfClientSessionListener listener = new SimpleNetconfClientSessionListener();

    public NetconfDriver(@Nonnull final YangPool pool, @Nonnull final EmuDriver.Settings<NetconfDriver> settings) {
        super(pool, settings);

        @Nonnull final var netconfSettings = (Settings) settings;
        this.address = new InetSocketAddress(netconfSettings.getHost(), netconfSettings.getPort());
        this.authentication = new LoginPasswordHandler(netconfSettings.getUser(), netconfSettings.getPassword());

        @Nonnull final var futureSession = new NetconfClientDispatcherImpl(EVENT_LOOP, EVENT_LOOP, new HashedWheelTimer())
                .createClient(NetconfClientConfigurationBuilder.create()
                        .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                        .withAddress(this.address)
                        .withAuthHandler(this.authentication)
                        .withReconnectStrategy(new ReconnectImmediatelyStrategy(EVENT_EXECUTOR, 0))
                        .withSessionListener(this.listener)
                        .build());

        try {
            this.session = futureSession.get();

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        this.transformer = new NetconfMessageTransformer(
                new EmptyMountPointContext(pool.getEffectiveModelContext()), true, // true -> strict message parsing
                new DefaultBaseNetconfSchemas(YANG_PARSER_FACTORY).getBaseSchemaWithNotifications());
    }

    @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchConfigurationData(@Nonnull final YangInstanceIdentifier iid) {
        return this.requestGetConfig();
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchConfigurationDataFor(@Nonnull final YangBindable object) {
        return this.requestGetConfig();
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> requestGetConfig() {
        @Nonnull final var message = this.transformer.toRpcRequest(
                NetconfMessageTransformUtil.NETCONF_GET_CONFIG_PATH,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(NetconfMessageTransformUtil.NETCONF_CONFIG_NODEID)
                        .build());

        @Nonnull final var response = this.request(message);

        LOG.info("Applying NETCONF response from {}@{}", this.authentication.getUsername(), this.address);
        try {
            return this.yangPool().writeConfigurationDataFrom(XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(
                    response.toString())));

            //this.transformer.toRpcResult(response, NetconfMessageTransformUtil.NETCONF_GET_PATH).getResult());

        } catch (final XMLStreamException e) {
            return FluentFuture.from(Futures.immediateFailedFuture(e));
        }
    }

    @Nonnull @Override @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchOperationalData(@Nonnull final YangInstanceIdentifier iid) {
        return this.requestGet();
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> fetchOperationalDataFor(@Nonnull final YangBindable object) {
        return this.requestGet();
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> requestGet() {
        @Nonnull final var message = this.transformer.toRpcRequest(
                NetconfMessageTransformUtil.NETCONF_GET_PATH,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(NetconfMessageTransformUtil.NETCONF_DATA_NODEID)
                        .build());

        @Nonnull final var response = this.request(message);

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
    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> pushConfigurationDataFrom(@Nonnull final YangBindable object) {
        return object.requireYangBinding().provideConfigurationData().transformAsync(data -> {
            @Nonnull final var futureCommitInfos = this.yangPool().writeConfigurationDataFrom(object.requireYangBinding());
            this.requestEditConfig(object.requireYangBinding().getIid(), data);

            return futureCommitInfos;
        }, this.executor);
    }
    */

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public FluentFuture<List<CommitInfo>> requestEditConfig(@Nonnull final NormalizedNode<?, ?> data) {
        this.request(this.transformer.toRpcRequest(NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_PATH, data));
        return FluentFuture.from(Futures.immediateFuture(List.of()));
    }

    @Nonnull
    private NetconfMessage request(@Nonnull final NetconfMessage message) {
        LOG.info("Sending NETCONF request to {}@{}:\n{}", this.authentication.getUsername(), this.address, message);

        @Nonnull final var futureResponse = this.listener.sendRequest(message);
        @Nonnull final NetconfMessage response;
        try {
            response = futureResponse.get();

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        LOG.debug("Received NETCONF response from {}@{}:\n{}", this.authentication.getUsername(), this.address, response);
        return response;
    }
}
