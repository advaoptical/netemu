package com.adva.netemu.northbound;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

// import jakarta.ws.rs.core.Application;
import com.adva.netemu.NetEmu;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;

import org.opendaylight.restconf.nb.rfc8040.RestconfApplication;

/*
import org.opendaylight.restconf.nb.rfc8040.handlers.ActionServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.DOMDataBrokerHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.DOMMountPointServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.NotificationServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.RpcServiceHandler;
*/

import org.opendaylight.restconf.nb.rfc8040.databind.DatabindProvider;
import org.opendaylight.restconf.nb.rfc8040.databind.mdsal.DOMDatabindProvider;
import org.opendaylight.restconf.nb.rfc8040.handlers.SchemaContextHandler;
// import org.opendaylight.restconf.nb.rfc8040.handlers.TransactionChainHandler;

// import org.opendaylight.restconf.nb.rfc8040.services.wrapper.ServicesWrapper;
import org.opendaylight.restconf.nb.rfc8040.streams.StreamsConfiguration;

import com.adva.netemu.YangPool;
import com.adva.netemu.service.EmuService;


public class RestconfService extends EmuService<RestconfService.Settings> {

    public static class Settings implements EmuService.Settings<RestconfService> {

        @Nonnegative
        private int port = 8080;

        @Nonnegative
        public int port() {
            return this.port;
        }

        @Nonnull
        public Settings setPort(@Nonnegative final int port) {
            this.port = port;
            return this;
        }

        @Nonnull @Override
        public Optional<NetEmu.RegisteredService<RestconfService, ?>> getRegisteredService() {
            return Optional.empty();
        }
    }

    @Nonnull
    private final AtomicReference<HttpServer> http = new AtomicReference<>();

    @Nonnull
    private final AtomicReference<RestconfApplication> restconf = new AtomicReference<>();

    @Nonnull
    public RestconfApplication restconf() {
        return this.restconf.get();
    }

    public RestconfService(@Nonnull final YangPool pool, @Nonnull final Settings settings) {
        super(pool, settings);
    }

    @Override
    public synchronized void run() {
        if (this.restconf.get() != null) {
            return;
        }

        @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var domSchemaService = FixedDOMSchemaService
                .of(super.yangPool(), super.yangPool());

        @Nonnull final var schemaContext = domSchemaService.getGlobalContext();

        @Nonnull final var domMountPointServiceHandler = /*new DOMMountPointServiceHandler(*/new DOMMountPointServiceImpl();
        @Nonnull final var transactionChainHandler = /*new TransactionChainHandler(*/super.yangPool().getNormalizedNodeBroker();
        @Nonnull final var schemaContextHandler = new SchemaContextHandler(transactionChainHandler, domSchemaService);
        schemaContextHandler.onModelContextUpdated(schemaContext);

        @Nonnull final var domRpcRouter = DOMRpcRouter.newInstance(domSchemaService);
        domRpcRouter.onModelContextUpdated(schemaContext);

        @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var restconf = new RestconfApplication(
                // schemaContextHandler, domMountPointServiceHandler, ServicesWrapper.newInstance(
                        // schemaContextHandler,
                        new DOMDatabindProvider(domSchemaService),
                        domMountPointServiceHandler,
                        transactionChainHandler,

                        // new DOMDataBrokerHandler(super.yangPool().getNormalizedNodeBroker()),
                        /*new RpcServiceHandler(*/domRpcRouter.getRpcService(),
                        /*new ActionServiceHandler(*/domRpcRouter.getActionService(),
                        /*new NotificationServiceHandler(*/DOMNotificationRouter.create(1024),
                        domSchemaService,
                        new StreamsConfiguration(0, 1, 0, true));

        @Nonnull final var settings = (Settings) this.settings();
        @Nonnull final var http = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(String.format("http://0.0.0.0:%d", settings.port())), ResourceConfig.forApplication(restconf));
                /*
                new Application() {

                    @Override
                    public Set<Class<?>> getClasses() {
                        return restconf.getClasses();
                    }

                    @Override
                    public Set<Object> getSingletons() {
                        return restconf.getSingletons();
                    }

                    @Override
                    public Map<String, Object> getProperties() {
                        return restconf.getProperties();
                    }
                }));
                */

        this.restconf.set(restconf);
        this.http.set(http);
        try {
            http.start();

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
