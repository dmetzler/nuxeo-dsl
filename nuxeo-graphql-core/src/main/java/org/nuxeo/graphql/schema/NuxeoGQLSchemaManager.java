package org.nuxeo.graphql.schema;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.graphql.descriptors.AliasDescriptor;
import org.nuxeo.graphql.descriptors.CrudDescriptor;
import org.nuxeo.graphql.descriptors.QueryDescriptor;
import org.nuxeo.graphql.schema.fetcher.DocPropertyDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentModelDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentMutationDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentMutationDataFetcher.Mode;
import org.nuxeo.graphql.schema.fetcher.NxqlQueryDataFetcher;
import org.nuxeo.graphql.schema.types.DocumentInputTypeBuilder;
import org.nuxeo.graphql.schema.types.DocumentTypeBuilder;
import org.nuxeo.graphql.schema.types.QueryFieldTypeBuilder;
import org.nuxeo.runtime.api.Framework;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;

public class NuxeoGQLSchemaManager {

    private GraphQLInterfaceType documentInterface;

    private Map<String, GraphQLObjectType> docTypeToGQLType = new HashMap<>();

    private Map<String, GraphQLObjectType> typesForSchema = new HashMap<>();

    private Map<String, GraphQLInputObjectType> inputTypesForSchema = new HashMap<>();

    private Map<String, AliasDescriptor> aliases;

    private Map<String, QueryDescriptor> queries;

    private Map<String, CrudDescriptor> cruds;

    public NuxeoGQLSchemaManager(Map<String, AliasDescriptor> aliases, Map<String, QueryDescriptor> queries,
            Map<String, CrudDescriptor> cruds) {
        this.aliases = aliases;
        this.queries = queries;
        this.cruds = cruds;
    }

    public GraphQLSchema getNuxeoSchema() {
        buildNuxeoTypes();
        Set<GraphQLType> dictionary = new HashSet<>(docTypeToGQLType.values());
        graphql.schema.GraphQLSchema.Builder builder = GraphQLSchema.newSchema().query(buildQueryType());
        if (cruds.size() > 0) {
            builder.mutation(buildMutationType());
        }
        return builder.build(dictionary);

    }

    private GraphQLObjectType buildQueryType() {
        Builder builder = newObject().name("nuxeo");
        builder.field(newFieldDefinition().name("document")
                                          .type(documentInterface)
                                          .argument(new GraphQLArgument("path", GraphQLString))
                                          .argument(new GraphQLArgument("id", GraphQLString))
                                          .dataFetcher(new DocumentModelDataFetcher())
                                          .build())
               .field(newFieldDefinition().name("documents")
                                          .type(new GraphQLList(documentInterface))
                                          .argument(new GraphQLArgument("nxql", new GraphQLNonNull(GraphQLString)))
                                          .dataFetcher(new NxqlQueryDataFetcher())
                                          .build());

        for (QueryDescriptor query : queries.values()) {
            builder.field(QueryFieldTypeBuilder.newField(query).build());
        }

        return builder.build();

    }

    private Builder buildMutationType() {
        Builder builder = newObject().name("nuxeoMutations");

        for (CrudDescriptor crud : cruds.values()) {
            buildMutationForDocType(builder, crud.targetDoctype);
        }

        return builder;
    }

    private void buildMutationForDocType(Builder builder, String docType) {
        GraphQLInputObjectType inputType = DocumentInputTypeBuilder.type(docType).build();

        builder.field(newFieldDefinition().name("create" + docType)
                                          .type(docTypeToGQLType.get(docType))
                                          .argument(newArgument().name(docType).type(inputType))
                                          .dataFetcher(new DocumentMutationDataFetcher(docType, Mode.CREATE)));

        builder.field(newFieldDefinition().name("update" + docType)
                                          .type(docTypeToGQLType.get(docType))
                                          .argument(newArgument().name(docType).type(inputType))
                                          .dataFetcher(new DocumentMutationDataFetcher(docType, Mode.UPDATE)));

        builder.field(newFieldDefinition().name("delete" + docType)
                                          .type(GraphQLString)
                                          .argument(newArgument().name(docType).type(inputType))
                                          .dataFetcher(new DocumentMutationDataFetcher(docType, Mode.DELETE)));
    }

    /**
     * Build a list of GraphQL types corresponding to each Nuxeo doc type.
     *
     * @return
     */
    private void buildNuxeoTypes() {
        if (documentInterface == null) {
            documentInterface = newInterface().name("document")
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("path")
                                                                         .dataFetcher(
                                                                                 new DocPropertyDataFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("id")
                                                                         .dataFetcher(new DocPropertyDataFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("name")
                                                                         .dataFetcher(new DocPropertyDataFetcher())
                                                                         .build())
                                              .typeResolver(getNuxeoDocumentTypeResolver())
                                              .build();

            docTypeToGQLType = new HashMap<>();
            SchemaManager sm = Framework.getService(SchemaManager.class);

            for (DocumentType type : sm.getDocumentTypes()) {
                docTypeToGQLType.put(type.getName(), DocumentTypeBuilder.newDocumentType(type).build());
            }
        }

    }


    /**
     * Creates a GQL type for a Nuxeo schema
     *
     * @param schemaName
     * @return
     */

    private TypeResolver getNuxeoDocumentTypeResolver() {
        return new TypeResolver() {

            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment tre) {
                if (tre.getObject() instanceof DocumentModel) {
                    return docTypeToGQLType.get(((DocumentModel) tre.getObject()).getType());
                } else {
                    return null;
                }
            }
        };
    }

    public GraphQLInterfaceType getDocumentInterface() {
        return documentInterface;
    }

    public GraphQLType docTypeToGQLType(String resultType) {
        return docTypeToGQLType.get(resultType);
    }

    public Map<String, GraphQLInputObjectType> getInputTypeRegistry() {
        return inputTypesForSchema;
    }

    public Map<String, GraphQLObjectType> getTypeRegistry() {
        return typesForSchema;
    }

    public Map<String, AliasDescriptor> getAliases() {
        return aliases;
    }

}
