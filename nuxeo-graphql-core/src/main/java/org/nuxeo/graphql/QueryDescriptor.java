package org.nuxeo.graphql;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("query")
public class QueryDescriptor {

    @XNode("@namel")
    public String name;

    @XNode("nxql")
    public String query;

    @XNodeList(value = "args/arg", type = ArrayList.class, componentType = String.class)
    public List<String> args;

    public QueryDescriptor() {

    }

}
