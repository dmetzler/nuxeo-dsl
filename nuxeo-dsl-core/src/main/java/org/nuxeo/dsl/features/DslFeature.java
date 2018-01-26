package org.nuxeo.dsl.features;

import java.util.Map;

import org.nuxeo.dsl.DslModel;

public interface DslFeature {

    void visit(DslModel model, Map<String, Object> ast);

}
