package org.nuxeo.dsl.jaxrs;

import java.util.LinkedHashSet;
import java.util.Set;

import org.nuxeo.ecm.webengine.app.WebEngineModule;

public class DslModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new LinkedHashSet<Object>();

        return result;
    }
}
