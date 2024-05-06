package com.adva.netemu.northbound;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.w3c.dom.Element;

import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

import org.opendaylight.netconf.server.api.monitoring.Capability;
import org.opendaylight.netconf.server.api.monitoring.YangModuleCapability;
import org.opendaylight.netconf.test.tool.DummyMonitoringService;
import org.opendaylight.netconf.test.tool.monitoring.JaxBSerializer;


@SuppressWarnings({"UnstableApiUsage"})
public class NetconfState extends DummyMonitoringService {

    private NetconfState(@Nonnull final EffectiveModelContext yangContext) {
        super(StreamEx.of(yangContext.getModules()).map(module -> (Capability) new YangModuleCapability(
                module.getNamespace().toString(), module.getName(), module.getRevision().map(Object::toString).orElse(null),
                /* TODO? */ "")).toImmutableSet());
    }

    @Nonnull
    public static NetconfState from(@Nonnull final EffectiveModelContext yangContext) {
        return new NetconfState(yangContext);
    }

    @Nonnull
    public Element toXmlElement() {
        return new JaxBSerializer().toXml(new org.opendaylight.netconf.test.tool.monitoring.NetconfState(this));
    }
}
