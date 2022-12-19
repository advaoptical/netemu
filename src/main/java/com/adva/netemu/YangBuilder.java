package com.adva.netemu;

import javax.annotation.Nonnull;

import org.opendaylight.yangtools.yang.binding.DataObject;


public interface YangBuilder<Y extends DataObject> {

    @Nonnull
    Y build();
}
