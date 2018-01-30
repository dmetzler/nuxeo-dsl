package org.nuxeo.graphql;

import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;

import graphql.schema.GraphQLSchema;

public interface GraphQLService {

    Object query(CoreSession session, String gqlQuery);

    Object query(CoreSession session,  String gqlQuery, Map<String, Object> arguments);

    GraphQLSchema getGraphQLSchema();

    void registerReloadListener(SchemaReloadedListener listener);
}
