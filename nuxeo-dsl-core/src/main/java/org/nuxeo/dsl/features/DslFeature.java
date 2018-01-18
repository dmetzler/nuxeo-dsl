package org.nuxeo.dsl.features;

import java.util.Map;

public interface DslFeature {

    void visit(Map<String, Object> ast);

}
