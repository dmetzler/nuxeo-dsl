package org.nuxeo.graphql;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

public class GraphQLComponent extends DefaultComponent{

    private AliasRegistry aliases = new AliasRegistry();
    private QueryRegistry queries = new QueryRegistry();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if(GraphQLService.class.equals(adapter)) {
            return (T) getGraphQLService();
        }
        return super.getAdapter(adapter);
    }

    private GraphQLService getGraphQLService() {
        return new GraphQLServiceImpl(aliases, queries);
    }


    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if("alias".equals(extensionPoint)) {
            aliases.addContribution((AliasDescriptor) contribution);
        } else if ("query".equals(extensionPoint)) {
            queries.addContribution((QueryDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if("alias".equals(extensionPoint)) {
            aliases.removeContribution((AliasDescriptor) contribution);
        } else if ("query".equals(extensionPoint)) {
            queries.removeContribution((QueryDescriptor) contribution);
        }
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

}
