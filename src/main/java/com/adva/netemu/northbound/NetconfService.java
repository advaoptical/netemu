package com.adva.netemu.northbound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

import com.google.common.collect.ImmutableSet;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
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

    @Nonnull
    private static Logger LOG = LoggerFactory.getLogger(NetconfService.class);

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
    private final AtomicReference<NetconfDeviceSimulator> netconf = new AtomicReference<>(null);

    @Nonnull
    public NetconfDeviceSimulator netconf() {
        return this.netconf.get();
    }

    public NetconfService(@Nonnull final YangPool pool) {
        super(pool);
    }

    @Override
    public synchronized void run() {
        if (this.netconf.get() != null) {
            return;
        }

        @Nonnull final var netconf = new NetconfDeviceSimulator(new ConfigurationBuilder()
                .setCapabilities(ImmutableSet.of("urn:ietf:params:netconf:base:1.0", "urn:ietf:params:netconf:base:1.1"))
                .setModels(Set.copyOf(super.yangPool().getModules()))
                .setRpcMapping(this)
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

        @Nonnull final Optional<Element> data;
        switch (request.getName()) {
            case "get":
                data = this.applyGetRequest(Objects.requireNonNull(request));
                break;

            case "edit-config":
                data = this.applyEditConfigRequest(request);
                break;

            default:
                return Optional.empty();
        }

        data.ifPresent(element -> root.appendChild(response.importNode(element, true)));
        return Optional.of(response);
    }

    @Nonnull
    Optional<Element> applyGetRequest(@SuppressWarnings({"unused"}) final XmlElement request) {
        @Nonnull final var futureData = super.yangPool().readOperationalData();
        @Nonnull final Optional<NormalizedNode<?, ?>> node;
        try {
                node = futureData.get();

        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        if (node.isEmpty()) {
            return Optional.empty();
        }

        @Nonnull final var stream = new ByteArrayOutputStream();
        @Nonnull final XMLStreamWriter xmlWriter;
        try {
            xmlWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(stream, "UTF-8");

        } catch (XMLStreamException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var nodeWriter = NormalizedNodeWriter.forStreamWriter(
                XMLStreamNormalizedNodeStreamWriter.createSchemaless(new IndentingXMLStreamWriter(xmlWriter)));
                // this._pool.getYangContext()));

        try {
            nodeWriter.write(node.get()).flush(); // TODO: @SuppressWarnings({"UnstableApiUsage"})

        } catch (final IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        /*  Adding <data> element to response adapted from
            https://stackoverflow.com/questions/729621/convert-string-xml-fragment-to-document-node-in-java
        */

        @Nonnull final Document data;
        try {
            data = RESPONSE_BUILDER.parse(new InputSource(new StringReader(stream.toString(StandardCharsets.UTF_8))));

        } catch (final SAXException | IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        return Optional.of(data.getDocumentElement());
    }

    @Nonnull
    Optional<Element> applyEditConfigRequest(@Nonnull final XmlElement request) {
        // @Nonnull final XmlElement target;
        try {
            request.getOnlyChildElement("target").getOnlyChildElement();

        } catch (final DocumentedException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        @Nonnull final XmlElement data;
        try {
            data = request.getOnlyChildElement("config");

        } catch (final DocumentedException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        @Nonnull final Transformer xmlTransformer;
        try {
            xmlTransformer = XML_TRANSFORMER_FACTORY.newTransformer();

        } catch (final TransformerConfigurationException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        @Nonnull final StringWriter dataWriter = new StringWriter();
        try {
            xmlTransformer.transform(new DOMSource(data.getDomElement()), new StreamResult(dataWriter));

        } catch (final TransformerException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        @Nonnull final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(new StringReader(dataWriter.toString()));

        } catch (final XMLStreamException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        super.yangPool().writeOperationalDataFrom(xmlReader);
        return Optional.empty();
    }
}
