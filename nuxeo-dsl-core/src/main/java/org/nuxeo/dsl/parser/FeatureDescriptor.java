package org.nuxeo.dsl.parser;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.dsl.features.DslFeature;

@XObject("feature")
public class FeatureDescriptor {

    @XNode("@class")
    public Class<? extends DslFeature> klass;
}
