package org.nuxeo.graphql.schema.fetcher;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class DocumentModelDataFetcher extends AbstractDataFetcher implements DataFetcher<DocumentModel>{

    @Override
    public DocumentModel get(DataFetchingEnvironment environment) {
        String path = environment.getArgument("path");
        String id = environment.getArgument("id");

        CoreSession session = getSession(environment.getContext());
        if (session != null) {
            if (path != null) {
                return session.getDocument(new PathRef(path));
            }
            if (id != null) {
                return session.getDocument(new IdRef(id));
            }
        }
        return null;
    }

}
