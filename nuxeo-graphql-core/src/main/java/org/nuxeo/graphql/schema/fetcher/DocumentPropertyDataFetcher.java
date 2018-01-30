package org.nuxeo.graphql.schema.fetcher;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class DocumentPropertyDataFetcher implements DataFetcher<Object> {

    private String property;

    public DocumentPropertyDataFetcher(String property) {
        this.property = property;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        Object source = environment.getSource();
        if (source instanceof DataModel) {
            DataModel dm = (DataModel) source;
            return dm.getValue(property);
        } else if (source instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) source;
            return doc.getPropertyValue(property);
        }
        return null;
    }




}
