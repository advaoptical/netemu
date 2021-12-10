package com.adva.netemu;

import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;


public class YangXmlDataInput extends StreamReaderDelegate {

    @Nonnull
    private static final Logger LOG = LoggerFactory.getLogger(YangXmlDataInput.class);

    public static final class EndOfDocument extends Exception {}

    private static class XmlNamespaceContext implements NamespaceContext {

        @Nonnull
        private final NamespaceContext context;

        @Nonnull
        private final Map<String, String> extraEntries;

        private XmlNamespaceContext(@Nonnull final NamespaceContext context, @Nonnull final Map<String, String> extraEntries) {
            this.context = context;
            this.extraEntries = extraEntries;
        }

        @Nonnull
        public static XmlNamespaceContext using(
                @Nonnull final NamespaceContext context,
                @Nonnull final Map<String, String> extraEntries) {

            return new XmlNamespaceContext(context, extraEntries);
        }

        @Nonnull @Override
        public String getNamespaceURI(@Nonnull final String prefix) {
            return Optional.ofNullable(this.context.getNamespaceURI(prefix))
                    .or(() -> Optional.ofNullable(this.extraEntries.get(prefix)))
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Undefined namespace prefix: %s", prefix)));
        }

        @Nullable @Override
        public String getPrefix(@Nonnull final String namespace) {
            return Optional.ofNullable(this.context.getPrefix(namespace))
                    .or(() -> EntryStream.of(this.extraEntries).findFirst(entry -> entry.getValue().equals(namespace))
                            .map(Map.Entry::getKey))

                    .orElse(null);
        }

        @Nonnull @Override
        public Iterator<String> getPrefixes(@Nonnull final String namespace) {
            return StreamEx.of(this.context.getPrefixes(namespace)).append(this.extraEntries.values()).distinct().iterator();
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

    @Nonnull
    private final Stack<javax.xml.namespace.QName> tagStack = new Stack<>();

    @Nullable
    private List<Map.Entry<String, String>> namespaceEntries = null;

    @Nonnull
    private final
    Map<SchemaPath, BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName>> elementTagProcessors;

    @Nonnull
    private final
    Map<SchemaPath, BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>>
    elementNamespaceProcessors;

    @Nonnull
    private final Map<SchemaPath, BiFunction<SchemaPath, String, String>> elementTextProcessors;

    private YangXmlDataInput(
            @Nonnull final XMLStreamReader xmlReader,
            @Nonnull final SchemaContext yangContext,
            @Nonnull final
            Map<SchemaPath, BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName>> elementTagProcessors,

            @Nonnull final
            Map<SchemaPath, BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>>
            elementNamespaceProcessors,

            @Nonnull final Map<SchemaPath, BiFunction<SchemaPath, String, String>> elementTextProcessors) {

        super(xmlReader);
        this.yangContext = yangContext;
        this.elementTagProcessors = elementTagProcessors;
        this.elementNamespaceProcessors = elementNamespaceProcessors;
        this.elementTextProcessors = elementTextProcessors;
    }

    @Nonnull
    public static YangXmlDataInput using(
            @Nonnull final XMLStreamReader xmlReader,
            @Nonnull final SchemaContext yangContext,
            @Nonnull final
            Map<SchemaPath, BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName>> elementTagProcessors,

            @Nonnull final
            Map<SchemaPath, BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>>
            elementNamespaceProcessors,

            @Nonnull final Map<SchemaPath, BiFunction<SchemaPath, String, String>> elementTextProcessors) {

        return new YangXmlDataInput(
                xmlReader, yangContext, elementTagProcessors, elementNamespaceProcessors, elementTextProcessors);
    }

    @Nonnull
    public static YangXmlDataInput using(@Nonnull final XMLStreamReader xmlReader, @Nonnull final SchemaContext yangContext) {
        return using(xmlReader, yangContext, Map.of(), Map.of(), Map.of());
    }

    @Nonnull
    public static YangXmlDataInput using(
            @Nonnull final Reader reader,
            @Nonnull final SchemaContext yangContext,
            @Nonnull final
            Map<SchemaPath, BiFunction<SchemaPath, javax.xml.namespace.QName, javax.xml.namespace.QName>> elementTagProcessors,

            @Nonnull final
            Map<SchemaPath, BiFunction<SchemaPath, List<Map.Entry<String, String>>, List<Map.Entry<String, String>>>>
            elementNamespaceProcessors,

            @Nonnull final Map<SchemaPath, BiFunction<SchemaPath, String, String>> elementTextProcessors)

            throws XMLStreamException {

        return using(
                XML_INPUT_FACTORY.createXMLStreamReader(reader), yangContext,
                elementTagProcessors, elementNamespaceProcessors, elementTextProcessors);
    }

    @Nonnull
    public static YangXmlDataInput using(@Nonnull final Reader reader, @Nonnull final SchemaContext yangContext)
            throws XMLStreamException {

        return using(reader, yangContext, Map.of(), Map.of(), Map.of());
    }

    @Nonnull @Override
    public NamespaceContext getNamespaceContext() {
        return XmlNamespaceContext.using(super.getNamespaceContext(), Optional.ofNullable(this.namespaceEntries)
                .map(entries -> StreamEx.of(entries).toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (existingValue, value) -> value))

                .orElseGet(Map::of));
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
            this.tagStack.push(this.treeTag);
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
            switch (tagType) {
                case START_ELEMENT:
                    this.tagStack.push(this.getName());
                    break;

                case END_ELEMENT:
                    this.tagStack.pop();
                    if (super.getName().equals(this.treeTag)) {
                        this.finishedTree = true;

                        LOG.info("Reached end of YANG Data tree: {}", this.yangTreeNode);
                    }
            }
        }

        if (tagType == XMLStreamReader.START_ELEMENT) {
            this.updateNamespaceEntries();
        }

        return tagType;
    }

    @Nonnull @Override
    public javax.xml.namespace.QName getName() {
        @Nonnull final var superXmlQName = super.getName();
        if (this.tagStack.isEmpty() || !this.tagStack.lastElement().equals(superXmlQName)) {
            return superXmlQName;
        }

        @Nullable final var yangPath = SchemaPath.create(/* absolute = */ true, StreamEx.of(this.tagStack)
                .map(xmlQName -> QName.create(xmlQName.getNamespaceURI(), xmlQName.getLocalPart()))
                .toArray(new QName[0]));

        @Nullable final var elementTagProcessor = this.elementTagProcessors.get(yangPath);
        if (elementTagProcessor != null) {
            return elementTagProcessor.apply(yangPath, superXmlQName);
        }

        return superXmlQName;
    }

    @Nonnull @Override
    public String getLocalName() {
        return this.getName().getLocalPart();
    }

    @Nonnull @Override
    public String getNamespaceURI() {
        return this.getName().getNamespaceURI();
    }

    private void updateNamespaceEntries() {
        @Nullable final var yangPath = SchemaPath.create(/* absolute = */ true, StreamEx.of(this.tagStack)
                .map(xmlQName -> QName.create(xmlQName.getNamespaceURI(), xmlQName.getLocalPart()))
                .toArray(new QName[0]));

        @Nonnull final var namespaceEntries = IntStreamEx.range(super.getNamespaceCount())
                .mapToEntry(super::getNamespacePrefix, super::getNamespaceURI)
                .append(StreamEx.of(Optional.ofNullable(this.namespaceEntries).orElse(List.of())))
                .toImmutableList();

        this.namespaceEntries = Optional.ofNullable(this.elementNamespaceProcessors.get(yangPath))
                .map(processor -> processor.apply(yangPath, namespaceEntries))
                .orElse(namespaceEntries);
    }

    @Nonnegative @Override
    public int getNamespaceCount() {
        return Optional.ofNullable(this.namespaceEntries).map(List::size).orElseGet(super::getNamespaceCount);
                // .orElseThrow(() -> new IllegalStateException("No XML Namespace definitions available."));
}

    @Nullable @Override
    public String getNamespacePrefix(@Nonnegative int index) {
        if (this.namespaceEntries != null) {
            if (index < this.namespaceEntries.size()) {
                return namespaceEntries.get(index).getKey();
            }

            throw new IndexOutOfBoundsException(index);
        }

        return super.getNamespacePrefix(index);
                // .orElseThrow(() -> new IllegalStateException("No XML Namespace definitions available."));
    }

    @Nonnull @Override
    public String getNamespaceURI(@Nonnegative int index) {
        return Optional.ofNullable(this.namespaceEntries).map(entries -> {
            if (index < entries.size()) {
                return entries.get(index).getValue();
            }

            throw new IndexOutOfBoundsException(index);

        }).orElseGet(() -> super.getNamespaceURI(index));
                // .orElseThrow(() -> new IllegalStateException("No XML Namespace definitions available."));
    }

    @Nullable @Override
    public String getNamespaceURI(@Nonnull final String prefix) {
        if (this.namespaceEntries != null) {
            @Nonnull final var uri = StreamEx.ofReversed(this.namespaceEntries)
                    .findFirst(entry -> prefix.equals(entry.getKey()))
                    .map(Map.Entry::getValue);

            if (uri.isPresent()) {
                return uri.get();
            }
        }

        return super.getNamespaceURI(prefix);
    }

    @Nullable @Override
    public String getElementText() throws XMLStreamException {
        @Nullable final var yangPath = SchemaPath.create(/* absolute = */ true, StreamEx.of(this.tagStack)
                .map(xmlQName -> QName.create(xmlQName.getNamespaceURI(), xmlQName.getLocalPart()))
                .toArray(new QName[0]));

        this.tagStack.pop();
        @Nullable final var elementTextProcessor = this.elementTextProcessors.get(yangPath);
        if (elementTextProcessor != null) {
            return elementTextProcessor.apply(yangPath, super.getElementText());
        }

        return super.getElementText();
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
