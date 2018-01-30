package org.nuxeo.graphql.schema.fetcher;

import graphql.schema.DataFetchingEnvironment;

public class QueryDataFetcher extends NxqlQueryDataFetcher {
    private String query;

    public QueryDataFetcher(String query) {
        this.query = query;
    }

    @Override
    protected String getQuery(DataFetchingEnvironment environment) {
        return query;
    }
}
