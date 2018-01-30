package org.nuxeo.graphql.schema.fetcher;

import java.util.Map.Entry;

import javax.el.ELContext;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.api.Framework;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class NxqlQueryDataFetcher extends AbstractDataFetcher implements DataFetcher<DocumentModelList> {

    @Override
    public DocumentModelList get(DataFetchingEnvironment environment) {
        CoreSession session = getSession(environment.getContext());
        if (session == null) {
            return new DocumentModelListImpl();
        }

        ExpressionEvaluator el = getEl(environment.getContext());
        String finalQuery = getQuery(environment);
        ELService elService = Framework.getService(ELService.class);
        ELContext elContext = elService.createELContext();
        el.bindValue(elContext, "principal", session.getPrincipal());

        if (environment.getArguments().size() > 0) {
            for (Entry<String, Object> paramEntry : environment.getArguments().entrySet()) {
                el.bindValue(elContext, paramEntry.getKey(), paramEntry.getValue());
            }
        }

        if (environment.getSource() instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) environment.getSource();
            el.bindValue(elContext, "this", doc);
        }

        finalQuery = el.evaluateExpression(elContext, finalQuery, String.class);
        return session.query(finalQuery);
    }

    protected String getQuery(DataFetchingEnvironment environment) {
        return environment.getArgument("nxql");
    }
}
