package com.adva.netemu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.opendaylight.yangtools.yang.data.api.schema.stream
        .NormalizedNodeWriter;

import org.opendaylight.yangtools.yang.data.codec.xml
        .XMLStreamNormalizedNodeStreamWriter;

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
    public Optional<Document> getResponse(final XmlElement message) {
        if (message == null) {
            LOG.debug("Received null message!");
            return Optional.empty();
        }

        LOG.debug("Received message:\n{}", message.toString());

        final var id = message.getDomElement().getOwnerDocument()
                .getDocumentElement().getAttribute(
                        XmlNetconfConstants.MESSAGE_ID);

        if (id == null) {
            LOG.error("Received message has no ID!");
            return Optional.empty();
        }

        LOG.info("Received message ID: {}", id);

        final var response = RESPONSE_BUILDER.newDocument();
        final Element root;
        try {
            root = response.createElementNS(
                    message.getNamespace(), "rpc-reply");

            response.appendChild(root);
            root.setAttribute(XmlNetconfConstants.MESSAGE_ID, id);

        } catch (final MissingNameSpaceException e) {
            e.printStackTrace();
            LOG.error("Received message has no namespace!");
            return Optional.empty();
        }

        final var future = this._pool.readOperationalData();
        final Optional<NormalizedNode<?, ?>> node;
        try {
                node = future.get();

        } catch (final
                InterruptedException |
                ExecutionException e) {

            e.printStackTrace();
            return Optional.empty();
        }

        if (node.isEmpty()) {
            return Optional.of(response);
        }

        final var stream = new ByteArrayOutputStream();
        final XMLStreamWriter xmlWriter;
        try {
            xmlWriter = XML_OUTPUT_FACTORY.createXMLStreamWriter(
                    stream, "UTF-8");

        } catch (XMLStreamException e) {
            e.printStackTrace();
            return Optional.of(response);
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
            return Optional.of(response);
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
            return Optional.of(response);
        }

        root.appendChild(response.importNode(
                data.getDocumentElement(), true));

        return Optional.of(response);
    }
}
