package com.adva.netemu;

import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;


public class YangXmlDataInput extends StreamReaderDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(
            YangXmlDataInput.class);

    public static class EndOfDocument extends Exception {}

    public static class NamespaceMap implements NamespaceContext {

        @Nonnull
        private final NamespaceContext _context;

        private NamespaceMap(@Nonnull final NamespaceContext context) {
            this._context = context;
        }

        @Nonnull
        public static NamespaceMap using(
                @Nonnull final NamespaceContext context) {

            return new NamespaceMap(context);
        }

        @Nonnull @Override
        public String getNamespaceURI(String prefix) {
            final var uri = this._context.getNamespaceURI(prefix);
            if (uri == null) {
                throw new IllegalArgumentException(
                        "Undefined namespace prefix: " + prefix);
            }

            return uri;
        }

        @Nullable @Override
        public String getPrefix(String namespaceURI) {
            return this._context.getPrefix(namespaceURI);
        }

        @Nonnull @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return this._context.getPrefixes(namespaceURI);
        }
    }

    private static final XMLInputFactory XML_INPUT_FACTORY =
            XMLInputFactory.newInstance();

    @Nonnull
    private final SchemaContext _yangContext;

    private int _nextTagType = -1;

    private DataSchemaNode _yangTreeNode = null;
    private javax.xml.namespace.QName _treeTag = null;
    private boolean _finishedTree = false;

    public DataSchemaNode getYangTreeNode() {
        return this._yangTreeNode;
    }

    private YangXmlDataInput(
            @Nonnull final XMLStreamReader xmlReader,
            @Nonnull final SchemaContext yangContext) {

        super(xmlReader);
        this._yangContext = yangContext;
    }

    @Nonnull
    public static YangXmlDataInput using(
            @Nonnull final XMLStreamReader xmlReader,
            @Nonnull final SchemaContext yangContext) {

        return new YangXmlDataInput(xmlReader, yangContext);
    }

    @Nonnull
    public static YangXmlDataInput using(
            @Nonnull final Reader reader,
            @Nonnull final SchemaContext yangContext)

            throws XMLStreamException {

        return using(
                XML_INPUT_FACTORY.createXMLStreamReader(reader), yangContext);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return NamespaceMap.using(super.getNamespaceContext());
    }

    public boolean nextYangTree() throws XMLStreamException, EndOfDocument {
        this._nextTagType = -1;
        this._treeTag = null;
        this._finishedTree = false;

        while (super.hasNext()) {
            final int tagType;
            try {
                tagType = super.nextTag();

            } catch (final XMLStreamException e) {
                if (super.getEventType() == XMLStreamConstants.END_DOCUMENT) {
                    throw new EndOfDocument();
                }

                throw e;
            }

            if (tagType != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            final var namespace = this.getNamespaceURI();
            if (namespace == null) {
                continue;
            }

            final URI uri;
            try {
                uri = new URI(namespace);

            } catch (final URISyntaxException e) {
                continue;
            }

            /*  First need to explicitly find matching YANG module,
                because the module revision is needed for finding the
                tree node in the YANG context afterwards
            */

            final var i = this._yangContext.findModules(uri).iterator();
            if (!i.hasNext()) {
                continue;
            }

            final var node = this._yangContext.findDataTreeChild(QName.create(
                    i.next().getQNameModule(), this.getLocalName()));

            if (node.isEmpty()) {
                continue;
            }

            LOG.info("Found YANG Data tree: {}", node.get().getQName());
            this._yangTreeNode = node.get();
            this._treeTag = super.getName();
            this._nextTagType = tagType;

            return true;
        }

        return false; // throw new XMLStreamException("TODO!");
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int tagType = this._nextTagType;
        if (tagType >= 0) {
            this._nextTagType = -1;

        } else {
            tagType = super.nextTag();

            if (tagType == XMLStreamConstants.END_ELEMENT) {
                if (super.getName() == this._treeTag) {
                    this._finishedTree = true;
                }
            }
        }

        return tagType;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        if (this._nextTagType >= 0) {
            return true;
        }

        if (this._finishedTree) {
            return false;
        }

        return super.hasNext();
    }
}
