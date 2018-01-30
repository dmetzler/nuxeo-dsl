package org.nuxeo.graphql.web;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.graphql.GraphQLService;
import org.nuxeo.graphql.SchemaReloadedListener;
import org.nuxeo.runtime.api.Framework;

import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLSchemaProvider;

public class NuxeoGraphQLSchemaProvider implements GraphQLSchemaProvider, SchemaReloadedListener {

    private GraphQLSchema schema;

    private GraphQLSchema readOnlySchema;

    public NuxeoGraphQLSchemaProvider() {
        Framework.getService(GraphQLService.class).registerReloadListener(this);
    }

    @Override
    public void onSchemaReloaded() {
        schema = null;
        readOnlySchema = null;
    }

    @Override
    public GraphQLSchema getSchema(HttpServletRequest request) {
        if (schema == null) {
            schema = Framework.getService(GraphQLService.class).getGraphQLSchema();
        }
        return schema;
    }

    @Override
    public GraphQLSchema getSchema() {
        return getSchema(null);
    }

    @Override
    public GraphQLSchema getReadOnlySchema(HttpServletRequest request) {
        if (readOnlySchema == null) {
            readOnlySchema = GraphQLSchemaProvider.copyReadOnly(getSchema(request));
        }
        return readOnlySchema;
    }

}
