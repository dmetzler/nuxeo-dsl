package org.nuxeo.graphql.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("alias")
public class AliasDescriptor {

    @XNode("@targetDoctype")
    public String targetDoctype;

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String type;

    @XNodeList(value = "args/arg", type = ArrayList.class, componentType = String.class)
    public List<String> args;

    public AliasDescriptor() {

    }
}
