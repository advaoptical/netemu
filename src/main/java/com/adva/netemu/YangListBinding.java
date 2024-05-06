package com.adva.netemu;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.reflect.TypeToken;

import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Key;
import org.opendaylight.yangtools.yang.binding.KeyAware;


public abstract class YangListBinding
        <Y extends ChildOf & KeyAware<K>, K extends Key<Y>, B extends YangBuilder<Y>> // TODO: ChildOf<?>
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
    public InstanceIdentifier.Builder<Y> getIidBuilder() {
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
