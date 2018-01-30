package org.nuxeo.graphql.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("crud")
public class CrudDescriptor {

    @XNode("@targetDoctype")
    public String targetDoctype;
}
