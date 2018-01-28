package org.nuxeo.graphql;

import org.nuxeo.ecm.core.api.CoreSession;

public interface NuxeoGraphqlContext {

    CoreSession getSession();
}
