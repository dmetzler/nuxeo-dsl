package org.nuxeo.graphql.web;

import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.graphql.NuxeoGraphqlContext;

public class DefaultNuxeoGraphqlContext implements NuxeoGraphqlContext {

    private CoreSession session;
    private ExpressionEvaluator el;

    public DefaultNuxeoGraphqlContext(CoreSession session) {
        this.session = session;
        el = new ExpressionEvaluator(new ExpressionFactoryImpl());
    }

    @Override
    public CoreSession getSession() {
        return session;
    }

    @Override
    public ExpressionEvaluator getEvaluator() {
        return el;
    }

}
