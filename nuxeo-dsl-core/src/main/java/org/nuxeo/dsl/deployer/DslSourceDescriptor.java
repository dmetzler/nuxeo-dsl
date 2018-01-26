package org.nuxeo.dsl.deployer;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value="dsl")
public class DslSourceDescriptor {

    @XNode("@name")
    String name;

    @XContent("src")
    String src;


    public DslSourceDescriptor() {
        this.name = "default";
    }

    public DslSourceDescriptor(String src) {
        this.name = "default";
        this.src = src;
    }

    public String getName() {
        return name;
    }

    public String getSrc() {
        return src;
    }


}
