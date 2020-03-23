package com.adva.netemu;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;


public final class Yang {

    private Yang() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static SchemaPath absolutePathFrom(@Nonnull final SchemaContext yangContext, @Nonnull final String... path) {
        @Nonnull final var namespaceMap = YangNamespaceMap.from(yangContext);
        @Nullable URI namespace = null;

        @Nonnull final var qnames = new ArrayList<QName>();
        for (@Nonnull final var segment : StreamEx.of(path)
                .flatMap(segment -> StreamEx.of(segment.split("/")).filter(StringUtils::isNotEmpty))) {

            if (segment.contains(":")) {
                @Nonnull final var splitSegment = segment.split(":", 2); // 2 -> No more than one split
                namespace = URI.create(namespaceMap.getNamespaceURI(splitSegment[0]));
                qnames.add(QName.create(namespace, splitSegment[1]));

            } else {
                if (namespace == null) {
                    throw new IllegalArgumentException(String.format("Missing namespace for YANG Path segment: %s", segment));
                }

                qnames.add(QName.create(namespace, segment));
            }
        }

        return SchemaPath.create(/* absolute = */ true, qnames.toArray(new QName[0]));
    }

    @Nonnull
    public static SchemaPath absolutePathFrom(@Nonnull final SchemaContext yangContext, @Nonnull final QName... path) {
        @Nonnull final var namespaceMap = YangNamespaceMap.from(yangContext);
        for (@Nonnull final var qname : path) {
            if (!namespaceMap.containsValue(qname.getNamespace())) {
                throw new IllegalArgumentException(String.format("Unknown YANG Module namespace in: %s", qname));
            }
        }

        return SchemaPath.create(/* absolute = */ true, path);
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    YangData<Y> operationalDataFrom(@Nonnull final T object) {
        try {
            return object.provideOperationalData().get();

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    StreamEx<Y> streamOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects.stream());
    }

    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf<?>>
    StreamEx<Y> streamOperationalDataFrom(@Nonnull final Stream<T> objects) {
        try {
            return StreamEx.of(Futures.allAsList(StreamEx.of(objects).map(YangBinding::provideOperationalData)).get())
                    .filter(YangData::isPresent).map(YangData::get);

        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /*
    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf>
    List<Y> listOperationalDataFrom(@Nonnull final Collection<T> objects) {
        return streamOperationalDataFrom(objects).collect(Collectors.toList());
    }
    */

    /*
    @Nonnull
    public static <T extends YangBinding<Y, ? extends Builder<Y>>, Y extends ChildOf>
    List<Y> listOperationalDataFrom(@Nonnull final Stream<T> objects) {
        return streamOperationalDataFrom(objects).collect(Collectors.toList());
    }
    */
}
