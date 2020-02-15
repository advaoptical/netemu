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

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(YangXmlDataInput.class);

    public static final class EndOfDocument extends Exception {}

    public static class NamespaceMap implements NamespaceContext {

        @Nonnull
        private final NamespaceContext context;

        private NamespaceMap(@Nonnull final NamespaceContext context) {
            this.context = context;
        }

        @Nonnull
        public static NamespaceMap using(@Nonnull final NamespaceContext context) {
            return new NamespaceMap(context);
        }

        @Nonnull @Override
        public String getNamespaceURI(String prefix) {
            @Nonnull final var uri = this.context.getNamespaceURI(prefix);
            if (uri == null) {
                throw new IllegalArgumentException(String.format("Undefined namespace prefix: %s", prefix));
            }

            return uri;
        }

        @Nullable @Override
        public String getPrefix(String namespaceURI) {
            return this.context.getPrefix(namespaceURI);
        }

        @Nonnull @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return this.context.getPrefixes(namespaceURI);
        }
    }

    @Nonnull
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    @Nonnull
    private final SchemaContext yangContext;

    private int nextTagType = -1;

    @Nullable
    private DataSchemaNode yangTreeNode = null;

    @Nullable
    private javax.xml.namespace.QName treeTag = null;

    private boolean finishedTree = false;

    @Nullable
    public DataSchemaNode getYangTreeNode() {
        return this.yangTreeNode;
    }

    private YangXmlDataInput(@Nonnull final XMLStreamReader xmlReader, @Nonnull final SchemaContext yangContext) {
        super(xmlReader);
        this.yangContext = yangContext;
    }

    @Nonnull
    public static YangXmlDataInput using(@Nonnull final XMLStreamReader xmlReader, @Nonnull final SchemaContext yangContext) {
        return new YangXmlDataInput(xmlReader, yangContext);
    }

    @Nonnull
    public static YangXmlDataInput using(@Nonnull final Reader reader, @Nonnull final SchemaContext yangContext)
            throws XMLStreamException {

        return using(XML_INPUT_FACTORY.createXMLStreamReader(reader), yangContext);
    }

    @Nonnull @Override
    public NamespaceContext getNamespaceContext() {
        return NamespaceMap.using(super.getNamespaceContext());
    }

    public boolean nextYangTree() throws XMLStreamException, EndOfDocument {
        this.nextTagType = -1;
        this.treeTag = null;
        this.finishedTree = false;

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

            @Nullable final var namespace = this.getNamespaceURI();
            if (namespace == null) {
                continue;
            }

            @Nonnull final URI uri;
            try {
                uri = new URI(namespace);

            } catch (final URISyntaxException e) {
                continue;
            }

            /*  First need to explicitly find matching YANG module,
                because the module revision is needed for finding the
                tree node in the YANG context afterwards
            */

            final var modules = this.yangContext.findModules(uri).iterator();
            if (!modules.hasNext()) {
                continue;
            }

            @Nonnull @SuppressWarnings({"UnstableApiUsage"}) final var node = this.yangContext.findDataTreeChild(QName.create(
                    modules.next().getQNameModule(), this.getLocalName()));

            if (node.isEmpty()) {
                continue;
            }

            LOG.info("Found YANG Data tree: {}", node.get().getQName());
            this.yangTreeNode = node.get();
            this.treeTag = super.getName();
            this.nextTagType = tagType;

            return true;
        }

        return false; // throw new XMLStreamException("TODO!");
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int tagType = this.nextTagType;
        if (tagType >= 0) {
            this.nextTagType = -1;

        } else {
            tagType = super.nextTag();
            if (tagType == XMLStreamConstants.END_ELEMENT) {
                if (super.getName().equals(this.treeTag)) {
                    this.finishedTree = true;

                    LOG.info("Reached end of YANG Data tree: {}", this.yangTreeNode);
                }
            }
        }

        return tagType;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        if (this.nextTagType >= 0) {
            return true;
        }

        if (this.finishedTree) {
            return false;
        }

        return super.hasNext();
    }
}
