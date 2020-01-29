package com.adva.netemu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import static java.nio.charset.StandardCharsets.UTF_8;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream
        .NormalizedNodeWriter;

import org.opendaylight.yangtools.yang.data.codec.xml
        .XMLStreamNormalizedNodeStreamWriter;

import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.MissingNameSpaceException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.netconf.test.tool.rpchandler.RpcHandler;


public class NetconfRequest implements RpcHandler {

    private static Logger LOG = LoggerFactory.getLogger(NetconfRequest.class);

    private static final XMLInputFactory XML_INPUT_FACTORY =
            XMLInputFactory.newInstance();

    private static final XMLOutputFactory XML_OUTPUT_FACTORY =
            XMLOutputFactory.newFactory();

    private static final TransformerFactory XML_TRANSFORMER_FACTORY =
            TransformerFactory.newDefaultInstance();

    private static final DocumentBuilder RESPONSE_BUILDER;
    static {
        try {
            final var factory = DocumentBuilderFactory.newInstance();

            /*  Without namespace awareness enabled, the builder would not be
                able to parse any XML using namespace syntax. Learned from
                https://docs.oracle.com/javase/tutorial/jaxp/dom
                        /readingXML.html
             */

            factory.setNamespaceAware(true);
            RESPONSE_BUILDER = factory.newDocumentBuilder();

        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException("TODO!");
        }
    }

    @Nonnull
    private YangPool _pool;

    public NetconfRequest(@Nonnull final YangPool pool) {
        this._pool = pool;
    }

    @Nonnull @Override
    public Optional<Document> getResponse(final XmlElement request) {
        if (request == null) {
            LOG.debug("Received null request!");
            return Optional.empty();
        }

        LOG.debug("Received request: {}", request.toString());

        final var id = request.getDomElement().getOwnerDocument()
                .getDocumentElement().getAttribute(
                        XmlNetconfConstants.MESSAGE_ID);

        if (id == null) {
            LOG.error("Received <{}> request has no ID!", request.getName());
            return Optional.empty();
        }

        LOG.info("Received <{}> request with {}: {}",
                request.getName(), XmlNetconfConstants.MESSAGE_ID, id);

        final var response = RESPONSE_BUILDER.newDocument();
        final Element root;
        try {
            root = response.createElementNS(
                    request.getNamespace(), "rpc-reply");

            response.appendChild(root);
            root.setAttribute(XmlNetconfConstants.MESSAGE_ID, id);

        } catch (final MissingNameSpaceException e) {
            e.printStackTrace();
            LOG.error("Request has no namespace!");
            return Optional.empty();
        }

        final Element data;
        switch (request.getName()) {
            case "get":
                data = this.applyGetRequest(request);
                break;

            case "edit-config":
                data = this.applyEditConfigRequest(request);
                break;

            default:
                return Optional.empty();
        }

        if (data != null) {
            root.appendChild(response.importNode(data, true));
        }

        return Optional.of(response);
    }

    @Nullable
    Element applyGetRequest(@Nonnull final XmlElement request) {

        final var future = this._pool.readOperationalData();
        final Optional<NormalizedNode<?, ?>> node;
        try {
                node = future.get();

        } catch (final
                InterruptedException |
                ExecutionException e) {

            e.printStackTrace();
            return null;
        }

        if (node.isEmpty()) {
            return null;
        }

        final var stream = new ByteArrayOutputStream();
        final XMLStreamWriter xmlWriter;
        try {
            xmlWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(
                    stream, "UTF-8");

        } catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        }

        final var nodeWriter = NormalizedNodeWriter.forStreamWriter(
                XMLStreamNormalizedNodeStreamWriter.createSchemaless(
                        new IndentingXMLStreamWriter(xmlWriter)));
                        // this._pool.getYangContext()));

        try {
            nodeWriter.write(node.get());
            nodeWriter.flush();

        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }

        /*  Adding <data> element to response adapted from
            https://stackoverflow.com/questions/729621
                    /convert-string-xml-fragment-to-document-node-in-java
        */

        final Document data;
        try {
            data = RESPONSE_BUILDER.parse(new InputSource(new StringReader(
                    stream.toString(UTF_8))));

        } catch (final
                SAXException |
                IOException e) {

            e.printStackTrace();
            return null;
        }

        return data.getDocumentElement();
    }

    @Nullable
    Element applyEditConfigRequest(@Nonnull final XmlElement request) {
        final XmlElement target;
        try {
            target = request.getOnlyChildElement("target")
                    .getOnlyChildElement();

        } catch (DocumentedException e) {
            e.printStackTrace();
            return null;
        }

        final XmlElement data;
        try {
            data = request.getOnlyChildElement("config");

        } catch (DocumentedException e) {
            e.printStackTrace();
            return null;
        }

        final Transformer xmlTransformer;
        try {
            xmlTransformer = XML_TRANSFORMER_FACTORY.newTransformer();

        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return null;
        }

        final StringWriter dataWriter = new StringWriter();
        try {
            xmlTransformer.transform(
                    new DOMSource(data.getDomElement()),
                    new StreamResult(dataWriter));

        } catch (TransformerException e) {
            e.printStackTrace();
            return null;
        }

        final XMLStreamReader xmlReader;
        try {
            xmlReader = XML_INPUT_FACTORY.createXMLStreamReader(
                    new StringReader(dataWriter.toString()));

        } catch (final XMLStreamException e) {
            e.printStackTrace();
            return null;
        }

        this._pool.writeOperationalDataFrom(xmlReader);
        return null;
    }
}
