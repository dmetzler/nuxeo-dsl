package org.nuxeo.dsl.builder;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("builder")
public class BuilderDescriptor {

    @XNode("@class")
    public Class<? extends Builder> klass;
}
