package org.nuxeo.graphql;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import graphql.schema.GraphQLSchema;
import graphql.servlet.DefaultExecutionStrategyProvider;
import graphql.servlet.GraphQLContext;
import graphql.servlet.GraphQLServletListener;
import graphql.servlet.SimpleGraphQLServlet;

public class NuxeoGraphqlServlet extends SimpleGraphQLServlet  {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NuxeoGraphqlServlet() {
        super(new NuxeoGraphQLSchemaProvider(), new DefaultExecutionStrategyProvider(), null, null);

        this.addListener(new GraphQLServletListener() {
            @Override
            public GraphQLServletListener.OperationCallback onOperation(GraphQLContext context, String operationName,
                    String query, Map<String, Object> variables) {

                return new GraphQLServletListener.OperationCallback() {

                    @Override
                    public void onFinally(GraphQLContext context, String operationName, String query,
                            Map<String, Object> variables, Object data) {
                        ((NuxeoHttpGraphQLContext) context).closeSession();
                    }
                };
            }

            @Override
            public RequestCallback onRequest(HttpServletRequest request, HttpServletResponse response) {
                if (!TransactionHelper.isTransactionActive()) {
                    TransactionHelper.startTransaction();
                }
                return new RequestCallback() {
                    @Override
                    public void onFinally(HttpServletRequest request, HttpServletResponse response) {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                };
            }
        });
    }

    @Override
    protected GraphQLContext createContext(Optional<HttpServletRequest> request,
            Optional<HttpServletResponse> response) {
        return new NuxeoHttpGraphQLContext(request, response);
    }

    private static GraphQLSchema buildSchema() {
        return Framework.getService(GraphQLService.class).getGraphQLSchema();
    }


}
