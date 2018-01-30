package org.nuxeo.graphql.schema;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.graphql.descriptors.AliasDescriptor;
import org.nuxeo.graphql.descriptors.QueryDescriptor;
import org.nuxeo.graphql.schema.fetcher.DocPropertyDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentModelDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentPropertyDataFetcher;
import org.nuxeo.graphql.schema.fetcher.NxqlQueryDataFetcher;
import org.nuxeo.graphql.schema.fetcher.QueryDataFetcher;
import org.nuxeo.graphql.schema.fetcher.SchemaDataFetcher;
import org.nuxeo.runtime.api.Framework;

import graphql.TypeResolutionEnvironment;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.TypeResolver;

public class NuxeoGQLSchemaManager {

    private GraphQLInterfaceType documentInterface;

    private Map<String, GraphQLObjectType> docTypeToGQLType = new HashMap<>();

    private Map<String, GraphQLObjectType> typesForSchema = new HashMap<>();

    private Map<String, AliasDescriptor> aliases;

    private Map<String, QueryDescriptor> queries;

    public NuxeoGQLSchemaManager(Map<String, AliasDescriptor> aliases, Map<String, QueryDescriptor> queries) {
        this.aliases = aliases;
        this.queries = queries;
    }

    public GraphQLSchema getNuxeoSchema() {
        buildNuxeoTypes();
        Set<GraphQLType> dictionary = new HashSet<>(docTypeToGQLType.values());
        return GraphQLSchema.newSchema().query(buildQueryType()).build(dictionary);
    }

    private GraphQLObjectType buildQueryType() {
        Builder builder = newObject().name("nuxeo").field(getDocumentQueryTypeField()).field(getNXQLQueryTypeField());
        for (QueryDescriptor query : queries.values()) {
            builder.field(getQueryFieldType(query));
        }
        return builder.build();

    }

    private GraphQLFieldDefinition getQueryFieldType(QueryDescriptor query) {
        graphql.schema.GraphQLFieldDefinition.Builder builder = newFieldDefinition().name(query.name);
        if (query.args.size() > 0) {
            for (String arg : query.args) {
                builder.argument(new GraphQLArgument(arg, new GraphQLNonNull(GraphQLString)));
            }
        }

        if ("document".equals(query.resultType)) {
            builder.type(new GraphQLList(documentInterface));
        } else {
            builder.type(new GraphQLList(docTypeToGQLType.get(query.resultType)));
        }

        builder.dataFetcher(new QueryDataFetcher(query.query));
        return builder.build();

    }

    private GraphQLFieldDefinition getNXQLQueryTypeField() {
        return newFieldDefinition().name("documents")
                                   .type(new GraphQLList(documentInterface))
                                   .argument(new GraphQLArgument("nxql", new GraphQLNonNull(GraphQLString)))
                                   .dataFetcher(new NxqlQueryDataFetcher())
                                   .build();
    }

    /**
     * Builds the document query type.
     *
     * @return
     */
    private GraphQLFieldDefinition getDocumentQueryTypeField() {

        return newFieldDefinition().name("document")
                                   .type(documentInterface)
                                   .argument(new GraphQLArgument("path", GraphQLString))
                                   .argument(new GraphQLArgument("id", GraphQLString))
                                   .dataFetcher(new DocumentModelDataFetcher())
                                   .build();
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
                                                                         .dataFetcher(new DocPropertyDataFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("id")
                                                                         .dataFetcher(new DocPropertyDataFetcher())
                                                                         .build())
                                              .typeResolver(getNuxeoDocumentTypeResolver())
                                              .build();

            docTypeToGQLType = new HashMap<>();
            SchemaManager sm = Framework.getService(SchemaManager.class);

            for (DocumentType type : sm.getDocumentTypes()) {
                Builder docTypeBuilder = newObject().name(type.getName()).withInterface(documentInterface);
                docTypeBuilder.field(newFieldDefinition().type(GraphQLString)//
                                                         .name("path")
                                                         .dataFetcher(new DocPropertyDataFetcher())
                                                         .build())
                              .field(newFieldDefinition().type(GraphQLString)//
                                                         .name("id")
                                                         .dataFetcher(new DocPropertyDataFetcher())
                                                         .build());

                for (Schema schema : type.getSchemas()) {
                    String name = schema.getNamespace().prefix;
                    name = StringUtils.isNotBlank(name) ? name : schema.getName();
                    GraphQLObjectType typeForSchema = typeForSchema(schema.getName());

                    if (!typeForSchema.getFieldDefinitions().isEmpty()) {
                        docTypeBuilder.field(
                                newFieldDefinition().name(name)
                                                    .type(typeForSchema)
                                                    .dataFetcher(new SchemaDataFetcher(schema))
                                                    .build());
                    }
                }

                aliases.values().stream().filter(a -> a.targetDoctype.equals(type.getName())).forEach(a -> {
                    docTypeBuilder.field(
                            newFieldDefinition().name(a.name)
                                                .type(getTypeForAlias(a))
                                                .dataFetcher(getFetcherForAlias(a))
                                                .build());
                });

                docTypeToGQLType.put(type.getName(), docTypeBuilder.build());
            }
        }

    }

    private GraphQLOutputType getTypeForAlias(AliasDescriptor alias) {
        if ("prop".equals(alias.type)) {
            return GraphQLString;
        } else if ("query".equals(alias.type)) {

            if (alias.args.size() > 1) {
                return new GraphQLList(docTypeToGQLType.get(alias.args.get(1)));
            } else {
                return new GraphQLList(new GraphQLTypeReference("document"));
            }
        } else {
            return null;
        }
    }

    private DataFetcher getFetcherForAlias(AliasDescriptor alias) {
        if ("prop".equals(alias.type)) {
            return new DocumentPropertyDataFetcher(alias.args.get(0));
        } else if ("query".equals(alias.type)) {
            return new QueryDataFetcher(alias.args.get(0));
        } else {
            return null;
        }
    }

    /**
     * Creates a GQL type for a Nuxeo schema
     *
     * @param schemaName
     * @return
     */
    private GraphQLObjectType typeForSchema(String schemaName) {

        if (!typesForSchema.containsKey(schemaName)) {
            SchemaManager sm = Framework.getService(SchemaManager.class);
            Schema s = sm.getSchema(schemaName);

            Builder schemaBuilder = newObject().name("schema_" + schemaName);

            for (Field f : s.getFields()) {
                if (!f.getName().getLocalName().matches("[_A-Za-z][_0-9A-Za-z]*")) {
                    continue;
                }

                Type t = f.getType();
                if (t.isSimpleType()) {
                    graphql.schema.GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition().name(
                            f.getName().getLocalName()).dataFetcher(
                                    new DocumentPropertyDataFetcher(f.getName().getPrefixedName()));
                    if (t instanceof StringType) {
                        fieldBuilder.type(GraphQLString);
                        schemaBuilder.field(fieldBuilder.build());
                    } else if (t instanceof BooleanType) {
                        fieldBuilder.type(GraphQLBoolean);
                        schemaBuilder.field(fieldBuilder.build());
                    } else if (t instanceof DateType) {
                        fieldBuilder.type(GraphQLString);
                        schemaBuilder.field(fieldBuilder.build());
                    } else if (t instanceof DoubleType) {
                        fieldBuilder.type(GraphQLFloat);
                        schemaBuilder.field(fieldBuilder.build());
                    } else if (t instanceof IntegerType) {
                        fieldBuilder.type(GraphQLInt);
                        schemaBuilder.field(fieldBuilder.build());
                    } else if (t instanceof LongType) {
                        fieldBuilder.type(GraphQLLong);
                        schemaBuilder.field(fieldBuilder.build());
                    }

                }
            }
            typesForSchema.put(schemaName, schemaBuilder.build());
        }
        return typesForSchema.get(schemaName);
    }

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




}
