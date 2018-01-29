package org.nuxeo.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.base.Joiner;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

public class GraphQLComponent extends DefaultComponent implements GraphQLService {

    private Map<String, AliasDescriptor> aliases = new HashMap<>();

    private Map<String, QueryDescriptor> queries = new HashMap<>();

    private NuxeoGQLSchemaManager sm;

    @Override
    public void start(ComponentContext context) {
        sm = null;
        listeners.forEach(l -> l.onSchemaReloaded());
    }

    private List<SchemaReloadedListener> listeners = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (GraphQLService.class.equals(adapter)) {
            return (T) this;
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("alias".equals(extensionPoint)) {
            AliasDescriptor alias = (AliasDescriptor) contribution;
            aliases.put(alias.name,alias);
        } else if ("query".equals(extensionPoint)) {
            QueryDescriptor query = (QueryDescriptor) contribution;
            queries.put(query.name,query);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("alias".equals(extensionPoint)) {
            AliasDescriptor alias = (AliasDescriptor) contribution;
            aliases.remove(alias.name);
        } else if ("query".equals(extensionPoint)) {
            QueryDescriptor query = (QueryDescriptor) contribution;
            queries.remove(query.name);
        }
    }

    @Override
    public Object query(CoreSession session, String gqlQuery) {
        ExecutionResult result = GraphQL.newGraphQL(getGraphQLSchema()).build().execute(gqlQuery,
                new DefaultNuxeoGraphqlContext(session));
        if (result.getErrors().size() > 0) {
            throw new NuxeoException(Joiner.on(", ").join(result.getErrors()));
        }

        return result.getData();

    }


    @Override
    public GraphQLSchema getGraphQLSchema() {
        return getSchemaManager().getNuxeoSchema();
    }

    private NuxeoGQLSchemaManager getSchemaManager() {
        if (sm == null) {
            sm = new NuxeoGQLSchemaManager(aliases, queries);
        }
        return sm;
    }


    @Override
    public void registerReloadListener(SchemaReloadedListener listener) {
        listeners.add(listener);
    }

}