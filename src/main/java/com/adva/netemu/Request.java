package com.adva.netemu;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema
    .ImmutableNormalizedNodeStreamWriter;

import org.opendaylight.yangtools.yang.data.impl.schema
    .NormalizedNodeResult;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import org.opendaylight.netconf.api.xml.MissingNameSpaceException;
import org.opendaylight.netconf.api.xml.XmlElement;
import org.opendaylight.netconf.api.xml.XmlNetconfConstants;

import org.opendaylight.netconf.test.tool.rpchandler.RpcHandler;


public class Request implements RpcHandler {

    private static Logger LOG = LoggerFactory.getLogger(Request.class);

    private static XMLInputFactory XML_INPUT_FACTORY =
        XMLInputFactory.newInstance();

    private static DocumentBuilder RESPONSE_BUILDER;
    static {
        try {
            RESPONSE_BUILDER = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();

        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private SchemaContext _yangContext;

    public Request(final SchemaContext yangContext) {
        this._yangContext = yangContext;
    }

    @Override
    public Optional<Document> getResponse(final XmlElement rpc) {
        if (rpc == null) {
            LOG.debug("Received null message!");
            return Optional.empty();
        }

        LOG.debug("Received message:\n{}", rpc.toString());

        final var id = rpc.getDomElement().getOwnerDocument()
            .getDocumentElement().getAttribute(
                XmlNetconfConstants.MESSAGE_ID);

        if (id == null) {
            LOG.error("Received message has no ID!");
            return Optional.empty();
        }
        LOG.info("Received message ID: {}", id);

        final var nodeResult = new NormalizedNodeResult();
        final var nodeWriter =
            ImmutableNormalizedNodeStreamWriter.from(nodeResult);

        final var yangModule = this._yangContext.findModule("TODO!");
        if (yangModule.isEmpty()) {
            LOG.info("YANG module not supported!");
            return Optional.empty();
        }

        final var parser = XmlParserStream.create(
            nodeWriter, this._yangContext, (SchemaNode) yangModule.get());

        try {
            parser.parse(XML_INPUT_FACTORY.createXMLStreamReader(
                new StringReader(rpc.toString())));

        } catch (
            final
                IOException |
                SAXException |
                URISyntaxException |
                XMLStreamException e) {

            e.printStackTrace();
            LOG.error("Cannot parse received message! ID: {}", id);
            return Optional.empty();
        }

        final var node = nodeResult.getResult();
        LOG.debug(
            "Parsed received message to YANG data node:\n{}",
            node.toString());

        final var response = RESPONSE_BUILDER.newDocument();
        try {
            final var root = response.createElementNS(
                rpc.getNamespace(), "rpc-reply");

            response.appendChild(root);
            root.setAttribute(XmlNetconfConstants.MESSAGE_ID, id);

        } catch (final MissingNameSpaceException e) {
            e.printStackTrace();
            LOG.error("Received message has no namespace!");
            return Optional.empty();
        }

        return Optional.of(response);
    }
}
