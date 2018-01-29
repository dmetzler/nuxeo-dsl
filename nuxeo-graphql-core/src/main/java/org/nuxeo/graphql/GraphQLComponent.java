package org.nuxeo.graphql;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

import com.google.common.base.Joiner;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

public class GraphQLComponent extends DefaultComponent implements GraphQLService {

    private AliasRegistry aliases = new AliasRegistry();

    private QueryRegistry queries = new QueryRegistry();

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
            aliases.addContribution((AliasDescriptor) contribution);
        } else if ("query".equals(extensionPoint)) {
            queries.addContribution((QueryDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("alias".equals(extensionPoint)) {
            aliases.removeContribution((AliasDescriptor) contribution);
        } else if ("query".equals(extensionPoint)) {
            queries.removeContribution((QueryDescriptor) contribution);
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

    protected static class AliasRegistry extends SimpleContributionRegistry<AliasDescriptor> {

        @Override
        public String getContributionId(AliasDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public AliasDescriptor getCurrentContribution(String id) {
            return super.getCurrentContribution(id);
        }

    }

    protected static class QueryRegistry extends SimpleContributionRegistry<QueryDescriptor> {

        @Override
        public String getContributionId(QueryDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public QueryDescriptor getCurrentContribution(String id) {
            return super.getCurrentContribution(id);
        }

    }

    @Override
    public void registerReloadListener(SchemaReloadedListener listener) {
        listeners.add(listener);
    }

}
