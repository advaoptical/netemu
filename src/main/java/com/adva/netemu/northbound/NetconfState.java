package com.adva.netemu.northbound;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.w3c.dom.Element;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

import org.opendaylight.netconf.api.capability.Capability;
import org.opendaylight.netconf.api.capability.YangModuleCapability;
import org.opendaylight.netconf.test.tool.DummyMonitoringService;
import org.opendaylight.netconf.test.tool.monitoring.JaxBSerializer;


public class NetconfState extends DummyMonitoringService {

    @SuppressWarnings({"UnstableApiUsage"})
    private NetconfState(@Nonnull final EffectiveModelContext yangContext) {
        super(StreamEx.of(yangContext.getModules()).map(module -> (Capability) new YangModuleCapability(module, /* TODO: */ ""))
                .toImmutableSet());
    }

    @Nonnull @SuppressWarnings({"UnstableApiUsage"})
    public static NetconfState from(@Nonnull final EffectiveModelContext yangContext) {
        return new NetconfState(yangContext);
    }

    @Nonnull
    public Element toXmlElement() {
        return new JaxBSerializer().toXml(new org.opendaylight.netconf.test.tool.monitoring.NetconfState(this));
    }
}
