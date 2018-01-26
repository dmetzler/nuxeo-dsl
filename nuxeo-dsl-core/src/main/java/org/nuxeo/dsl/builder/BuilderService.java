package org.nuxeo.dsl.builder;

import org.nuxeo.dsl.DslModel;

public interface BuilderService {


    void compile(DslModel model, BuildContext ctx);
}
