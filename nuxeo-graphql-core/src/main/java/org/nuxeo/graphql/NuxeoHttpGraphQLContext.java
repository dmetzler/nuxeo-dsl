package org.nuxeo.graphql;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;

import graphql.servlet.GraphQLContext;

public class NuxeoHttpGraphQLContext extends GraphQLContext implements NuxeoGraphqlContext {

    // TODO in 10.1 use a CloseableCoreSession
    private CoreSession session;

    public NuxeoHttpGraphQLContext(Optional<HttpServletRequest> request, Optional<HttpServletResponse> response) {
        super(request, response);
        if (request.get().getUserPrincipal() == null) {
            throw new java.lang.IllegalStateException("Not authenticated user is trying to get a core session");
        }
    }

    public CoreSession getSession() {
        if (session == null) {
            session = CoreInstance.openCoreSession("default");
        }
        return session;

    }

    public void closeSession() {
        if (session != null) {
            session.close();
        }
    }

}
