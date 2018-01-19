package org.nuxeo.dsl.builder;

import org.nuxeo.dsl.features.DslFeature;

public interface  Builder {

    public void build(DslFeature feature, BuildContext ctx);

    public Class<? extends DslFeature> getFeatureClass();


}
