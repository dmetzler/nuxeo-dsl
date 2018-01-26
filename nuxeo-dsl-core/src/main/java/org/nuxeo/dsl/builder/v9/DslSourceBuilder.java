package org.nuxeo.dsl.builder.v9;

import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.Builder;
import org.nuxeo.dsl.deployer.DslSourceDescriptor;
import org.nuxeo.dsl.features.DslFeature;
import org.nuxeo.dsl.features.DslSourceFeature;

public class DslSourceBuilder implements Builder {

    @Override
    public void build(DslFeature f, BuildContext ctx) {
        ctx.registerXMap(DslSourceDescriptor.class);

        DslSourceFeature feature = (DslSourceFeature) f;

        ctx.registerXP("org.nuxeo.dsl.deployer.DslDeployer", "dsl", new DslSourceDescriptor(feature.getSrc()));

    }

    @Override
    public Class<? extends DslFeature> getFeatureClass() {
        return DslSourceFeature.class;
    }

}
