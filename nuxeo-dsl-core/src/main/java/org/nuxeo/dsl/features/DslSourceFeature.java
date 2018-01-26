package org.nuxeo.dsl.features;

import java.util.Map;

import org.nuxeo.dsl.DslModel;

public class DslSourceFeature implements DslFeature {

    private String src;

    @Override
    public void visit(DslModel model, Map<String, Object> ast) {
        src = model.getSource();
    }

    public String getSrc() {
        return src;
    }

}
