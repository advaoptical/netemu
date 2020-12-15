package com.adva.netemu.northbound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;

import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.MissingNameSpaceException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;

import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.ConfigurationBuilder;
import org.opendaylight.netconf.test.tool.rpchandler.RpcHandler;

import com.adva.netemu.YangPool;
import com.adva.netemu.service.EmuService;


public class NetconfService extends EmuService implements RpcHandler {

    public static class Settings extends ConfigurationBuilder implements EmuService.Settings<NetconfService> {}

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(NetconfService.class);

    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    @Nonnull
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();

    @Nonnull
    private static final TransformerFactory XML_TRANSFORMER_FACTORY = TransformerFactory.newDefaultInstance();

    @Nonnull
    private static final DocumentBuilder RESPONSE_BUILDER;
    static {
        try {
            @Nonnull final var factory = DocumentBuilderFactory.newInstance();

            /*  Without namespace awareness enabled, the builder would not be able to parse any XML using namespace syntax.
                Learned from https://docs.oracle.com/javase/tutorial/jaxp/dom/readingXML.html
            */

            factory.setNamespaceAware(true);
            RESPONSE_BUILDER = factory.newDocumentBuilder();

        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Failed creating XML Document builder for NETCONF responses", e);
        }
    }

    @Nonnull
    private final NetconfState netconfState;

    @Nonnull
    private final AtomicReference<NetconfDeviceSimulator> netconf = new AtomicReference<>(null);

    @Nonnull
    public NetconfDeviceSimulator netconf() {
        return this.netconf.get();
    }

    public NetconfService(@Nonnull final YangPool pool, @Nonnull final Settings settings) {
        super(pool, settings);
        this.netconfState = NetconfState.from(pool.getEffectiveModelContext());
    }

    @Override
    public synchronized void run() {
        if (this.netconf.get() != null) {
            throw new IllegalStateException(String.format("%s is already running", this));
        }

        @Nonnull final var netconf = new NetconfDeviceSimulator(((ConfigurationBuilder) super.settings())
                .setCapabilities(Set.of("urn:ietf:params:netconf:base:1.0", "urn:ietf:params:netconf:base:1.1"))
                .setModels(Set.copyOf(super.yangPool().getModules()))
                .setRpcMapping(this)
                .setDeviceCount(1)
                .build());

        this.netconf.set(netconf);
        netconf.start();
    }

    @Nonnull @Override
    public Optional<Document> getResponse(@Nullable final XmlElement request) {
        if (request == null) {
            LOG.debug("Received null request!");
            return Optional.empty();
        }

        LOG.debug("Received request: {}", request);

        @Nullable final var id = request.getDomElement().getOwnerDocument().getDocumentElement().getAttribute(
                XmlNetconfConstants.MESSAGE_ID);

        if (id == null) {
            LOG.error("Received <{}> request has no ID!", request.getName());
            return Optional.empty();
        }

        LOG.info("Received <{}> request with {}: {}", request.getName(), XmlNetconfConstants.MESSAGE_ID, id);

        @Nonnull final var response = RESPONSE_BUILDER.newDocument();
        @Nonnull final Element root;
        try {
            root = response.createElementNS(request.getNamespace(), "rpc-reply");
            response.appendChild(root);
            root.setAttribute(XmlNetconfConstants.MESSAGE_ID, id);

        } catch (final MissingNameSpaceException e) {
            LOG.error("Request has no namespace!", e);
            return Optional.empty();
        }

        @Nonnull final CompletableFuture<Optional<Element>> futureData;
        switch (request.getName()) {
            case "get":
                futureData = this.applyGetRequest(Objects.requireNonNull(request));
                break;

            case "edit-config":
                futureData = this.applyEditConfigRequest(request);
                break;

            default:
                return Optional.empty();
        }

        try {
            futureData.get().ifPresent(element -> root.appendChild(response.importNode(element, true)));

        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @Nonnull
    CompletableFuture<Optional<Element>> applyGetRequest(@Nonnull @SuppressWarnings({"unused"}) final XmlElement request) {
        return FutureConverter.toCompletableFuture(super.yangPool().readOperationalData()).thenApplyAsync(data ->
                data.map(yangNode -> {
                    @Nonnull final var xmlByteStream = new ByteArrayOutputStream();

                    @Nonnull final XMLStreamWriter xmlWriter;
                    try {
                        xmlWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(xmlByteStream, "UTF-8");

                    } catch (final XMLStreamException e) {
                        throw new RuntimeException(e);
                    }

                    @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var yangNodeWriter = NormalizedNodeWriter
                            .forStreamWriter(XMLStreamNormalizedNodeStreamWriter.createSchemaless(
                                    new IndentingXMLStreamWriter(xmlWriter)));
                                    // this._pool.getYangContext()));

                    try {
                        yangNodeWriter.write(yangNode).flush(); // TODO: @SuppressWarnings({"UnstableApiUsage"})

                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                    /*  Adding <data> element to response adapted from
                        https://stackoverflow.com/questions/729621/convert-string-xml-fragment-to-document-node-in-java
                    */

                    @Nonnull final Document xmlData;
                    try {
                        xmlData = RESPONSE_BUILDER.parse(new InputSource(new StringReader(xmlByteStream.toString(
                                StandardCharsets.UTF_8))));

                    } catch (final SAXException | IOException e) {
                        throw new RuntimeException(e);
                    }

                    @Nonnull final var xmlDataElement = xmlData.getDocumentElement();
                    xmlDataElement.appendChild(xmlData.importNode(this.netconfState.toXmlElement(), true));
                    return xmlDataElement;
                }));
    }

    @Nonnull
    CompletableFuture<Optional<Element>> applyEditConfigRequest(@Nonnull final XmlElement request) {
        return CompletableFuture.supplyAsync(() -> {
            // @Nonnull final XmlElement target;
            try {
                request.getOnlyChildElement("target").getOnlyChildElement();

            } catch (final DocumentedException e) {
                throw new RuntimeException(e);
            }

            @Nonnull final XmlElement data;
            try {
                data = request.getOnlyChildElement("config");

            } catch (final DocumentedException e) {
                throw new RuntimeException(e);
            }

            @Nonnull final Transformer xmlTransformer;
            try {
                xmlTransformer = XML_TRANSFORMER_FACTORY.newTransformer();

            } catch (final TransformerConfigurationException e) {
                throw new RuntimeException(e);
            }

            @Nonnull final StringWriter dataWriter = new StringWriter();
            try {
                xmlTransformer.transform(new DOMSource(data.getDomElement()), new StreamResult(dataWriter));

            } catch (final TransformerException e) {
                throw new RuntimeException(e);
            }

            @Nonnull final XMLStreamReader xmlReader;
            try {
                xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(dataWriter.toString()));

            } catch (final XMLStreamException e) {
                throw new RuntimeException(e);
            }

            return xmlReader;

        }).thenCompose(xmlReader -> FutureConverter.toCompletableFuture(super.yangPool().writeOperationalDataFrom(xmlReader))
                .thenApply(ignoredCommitInfos -> Optional.empty()));
    }
}
