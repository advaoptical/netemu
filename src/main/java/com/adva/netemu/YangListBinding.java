package com.adva.netemu;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.reflect.TypeToken;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public abstract class YangListBinding
        <Y extends ChildOf & Identifiable<K>, K extends Identifier<Y>, B extends Builder<Y>> // TODO: ChildOf<?>
        extends YangBinding<Y, B> implements YangListBindable {

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this);
    }

    @Nonnull
    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public Class<K> getKeyClass() {
        return (Class<K>) (new TypeToken<K>(this.getClass()) {}).getRawType();
    }

    @Nonnull @Override
    @SuppressWarnings("unchecked")
    public InstanceIdentifier.InstanceIdentifierBuilder<Y> getIidBuilder() {
        if (this.owner == null) {
            return InstanceIdentifier.builder(this.getDataClass(), this.getKey());
        }

        return this.getParentAugmentationClass().map(augmentationClass ->
                this.owner.getIidBuilder().augmentation(augmentationClass).child(this.getDataClass(), this.getKey())

        ).orElseGet(() -> this.owner.getIidBuilder().child(this.getDataClass(), this.getKey()));
    }

    @Nonnull
    public abstract K getKey();
}
