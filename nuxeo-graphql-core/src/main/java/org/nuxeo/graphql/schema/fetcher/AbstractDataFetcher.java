package org.nuxeo.graphql.schema.fetcher;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.graphql.NuxeoGraphqlContext;

public abstract class AbstractDataFetcher {

    protected CoreSession getSession(Object ctx) {
        if (ctx instanceof NuxeoGraphqlContext) {
            return ((NuxeoGraphqlContext) ctx).getSession();
        }
        return null;
    }


    protected ExpressionEvaluator getEl(Object ctx) {
        if (ctx instanceof NuxeoGraphqlContext) {
            return ((NuxeoGraphqlContext) ctx).getEvaluator();
        }
        return null;
    }
}
