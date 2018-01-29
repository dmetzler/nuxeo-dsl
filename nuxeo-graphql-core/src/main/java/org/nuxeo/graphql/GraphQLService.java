package org.nuxeo.graphql;

import org.nuxeo.ecm.core.api.CoreSession;

import graphql.schema.GraphQLSchema;

public interface GraphQLService {

    Object query(CoreSession session, String gqlQuery);

    GraphQLSchema getGraphQLSchema();

    void registerReloadListener(SchemaReloadedListener listener);
}
