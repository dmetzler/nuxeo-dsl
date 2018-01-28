package org.nuxeo.graphql;

import org.nuxeo.ecm.core.api.CoreSession;

public class DefaultNuxeoGraphqlContext implements NuxeoGraphqlContext {

    private CoreSession session;

    public DefaultNuxeoGraphqlContext(CoreSession session) {
        this.session = session;
    }

    @Override
    public CoreSession getSession() {
        return session;
    }

}
