package org.nuxeo.graphql.schema.fetcher;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.Schema;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public class SchemaDataFetcher implements DataFetcher{

    private Schema schema;

    public SchemaDataFetcher(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) {
        if (environment.getSource() instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) environment.getSource();
            return doc.getDataModel(schema.getName());
        }
        return null;
    }

}
