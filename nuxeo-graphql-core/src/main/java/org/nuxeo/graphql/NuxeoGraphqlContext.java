package org.nuxeo.graphql;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public interface NuxeoGraphqlContext {

    CoreSession getSession();

    ExpressionEvaluator getEvaluator();
}
