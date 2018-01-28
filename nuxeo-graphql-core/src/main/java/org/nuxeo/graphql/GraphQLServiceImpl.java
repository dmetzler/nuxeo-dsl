package org.nuxeo.graphql;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.google.common.base.Joiner;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

public class GraphQLServiceImpl implements GraphQLService {
    private NuxeoGQLSchemaManager sm;

    public GraphQLServiceImpl() {
        sm = new NuxeoGQLSchemaManager();
    }

    @Override
    public Object query(CoreSession session, String gqlQuery) {
        ExecutionResult result = GraphQL.newGraphQL(getGraphQLSchema()).build().execute(gqlQuery, new DefaultNuxeoGraphqlContext(session));
        if (result.getErrors().size() > 0) {
            throw new NuxeoException(Joiner.on(", ").join(result.getErrors()));
        }

        return result.getData();

    }

    @Override
    public GraphQLSchema getGraphQLSchema() {
        return sm.getNuxeoSchema();
    }

}
