package org.nuxeo.graphql.schema.types;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import org.nuxeo.graphql.GraphQLService;
import org.nuxeo.graphql.descriptors.QueryDescriptor;
import org.nuxeo.graphql.schema.fetcher.QueryDataFetcher;
import org.nuxeo.runtime.api.Framework;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLFieldDefinition.Builder;

public class QueryFieldTypeBuilder extends Builder {

    private QueryDescriptor query;

    private QueryFieldTypeBuilder(QueryDescriptor query) {
        this.query = query;
    }

    @Override
    public GraphQLFieldDefinition build() {

        GraphQLService gql = Framework.getService(GraphQLService.class);
        Builder fieldBuilder = newFieldDefinition().name(query.name);
        if (query.args.size() > 0) {
            for (String arg : query.args) {
                fieldBuilder.argument(new GraphQLArgument(arg, new GraphQLNonNull(GraphQLString)));
            }
        }

        if ("document".equals(query.resultType)) {
            fieldBuilder.type(new GraphQLList(gql.getSchemaManager().getDocumentInterface()));
        } else {
            fieldBuilder.type(new GraphQLList(gql.getSchemaManager().docTypeToGQLType(query.resultType)));
        }

        fieldBuilder.dataFetcher(new QueryDataFetcher(query.query));
        return fieldBuilder.build();

    }

    public static QueryFieldTypeBuilder newField(QueryDescriptor query) {
        return new QueryFieldTypeBuilder(query);
    }
}
