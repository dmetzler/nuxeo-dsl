package org.nuxeo.graphql;

import org.nuxeo.runtime.model.DefaultComponent;

public class GraphQLComponent extends DefaultComponent{



    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if(GraphQLService.class.equals(adapter)) {
            return (T) getGraphQLService();
        }
        return super.getAdapter(adapter);
    }

    private GraphQLService getGraphQLService() {
        return new GraphQLServiceImpl();
    }
}
