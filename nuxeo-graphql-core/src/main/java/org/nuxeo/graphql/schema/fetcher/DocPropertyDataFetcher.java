package org.nuxeo.graphql.schema.fetcher;

import org.nuxeo.ecm.core.api.DocumentModel;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class DocPropertyDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(DataFetchingEnvironment environment) {
        String fieldName = getFieldName(environment);
        if (environment.getSource() instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) environment.getSource();
            if ("_path".equals(fieldName)) {
                return doc.getPathAsString();
            } else if ("_id".equals(fieldName)) {
                return doc.getId();
            }
            else if ("_name".equals(fieldName)) {
                return doc.getName();
            }
        }
        return null;
    }

    private String getFieldName(DataFetchingEnvironment environment) {
        String fieldName = environment.getFields().get(0).getName();
        return fieldName;
    }
}
