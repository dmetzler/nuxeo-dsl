package org.nuxeo.graphql.schema;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
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
import org.nuxeo.graphql.NuxeoGraphqlContext;
import org.nuxeo.graphql.descriptors.AliasDescriptor;
import org.nuxeo.graphql.descriptors.CrudDescriptor;
import org.nuxeo.graphql.descriptors.QueryDescriptor;
import org.nuxeo.graphql.schema.fetcher.DocPropertyDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentModelDataFetcher;
import org.nuxeo.graphql.schema.fetcher.DocumentPropertyDataFetcher;
import org.nuxeo.graphql.schema.fetcher.NxqlQueryDataFetcher;
import org.nuxeo.graphql.schema.fetcher.QueryDataFetcher;
import org.nuxeo.graphql.schema.fetcher.SchemaDataFetcher;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.utility.StringUtil;
import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
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

    private Map<String, CrudDescriptor> cruds;

    private Map<String, GraphQLInputObjectType> inputTypesForSchema = new HashMap<>();

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

    private Builder buildMutationType() {
        Builder builder = newObject().name("nuxeoMutations");

        SchemaManager sm = Framework.getService(SchemaManager.class);

        for (CrudDescriptor crud : cruds.values()) {

            DocumentType docType = sm.getDocumentType(crud.targetDoctype);

            graphql.schema.GraphQLInputObjectType.Builder inputTypeBuilder = GraphQLInputObjectType.newInputObject()
                                                                                                   .name(crud.targetDoctype
                                                                                                           + "Input");
            inputTypeBuilder.field(newInputObjectField().name("path").type(GraphQLString));
            inputTypeBuilder.field(newInputObjectField().name("id").type(GraphQLString));
            inputTypeBuilder.field(newInputObjectField().name("name").type(GraphQLString));

            for (Schema schema : docType.getSchemas()) {
                String name = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix : schema.getName();
                GraphQLInputObjectType inputTypeForSchema = inputTypeForSchema(schema.getName());
                if (!inputTypeForSchema.getFieldDefinitions().isEmpty()) {
                    inputTypeBuilder.field(newInputObjectField().name(name).type(inputTypeForSchema));
                }
            }

            GraphQLInputObjectType inputType = inputTypeBuilder.build();

            builder.field(newFieldDefinition().name("create" + crud.targetDoctype)
                                              .type(docTypeToGQLType.get(crud.targetDoctype))
                                              .argument(newArgument().name(crud.targetDoctype).type(inputType))
                                              .dataFetcher(mutationDataFetcherForCreation(crud.targetDoctype)));

            builder.field(newFieldDefinition().name("update" + crud.targetDoctype)
                                              .type(docTypeToGQLType.get(crud.targetDoctype))
                                              .argument(newArgument().name(crud.targetDoctype).type(inputType))
                                              .dataFetcher(mutationDataFetcherForUpdate(crud.targetDoctype)));

            builder.field(newFieldDefinition().name("delete" + crud.targetDoctype)
                                              .type(GraphQLString)
                                              .argument(newArgument().name(crud.targetDoctype).type(inputType))
                                              .dataFetcher(mutationDataFetcherForDeletion(crud.targetDoctype)));

        }

        return builder;
    }

    private DataFetcher mutationDataFetcherForDeletion(String targetDoctype) {
        return new DataFetcher() {
            @Override
            public String get(DataFetchingEnvironment environment) {
                Map<String, Object> docInputMap = environment.getArgument(targetDoctype);

                String id = (String) docInputMap.get("id");
                String path = (String) docInputMap.get("path");
                CoreSession session = ((NuxeoGraphqlContext) environment.getContext()).getSession();

                if (StringUtils.isNotBlank(id)) {
                    session.removeDocument(new IdRef(id));
                } else {
                    session.removeDocument(new PathRef(path));
                }
                return "deleted";

            }
        };
    }

    private DataFetcher mutationDataFetcherForUpdate(String targetDoctype) {
        return new DataFetcher() {
            @Override
            public DocumentModel get(DataFetchingEnvironment environment) {
                Map<String, Object> docInputMap = environment.getArgument(targetDoctype);

                String id = (String) docInputMap.get("id");
                String path = (String) docInputMap.get("path");
                DocumentModel doc;
                CoreSession session = ((NuxeoGraphqlContext) environment.getContext()).getSession();

                if (StringUtils.isNotBlank(id)) {
                    doc = session.getDocument(new IdRef(id));
                } else {
                    doc = session.getDocument(new PathRef(path));
                }

                SchemaManager sm = Framework.getService(SchemaManager.class);
                DocumentType docType = sm.getDocumentType(targetDoctype);
                for (Schema schema : docType.getSchemas()) {
                    String schemaName = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix
                            : schema.getName();
                    Map<String, Object> dataModelMap = (Map<String, Object>) docInputMap.get(schemaName);
                    if (dataModelMap != null) {
                        for (Entry<String, Object> entry : dataModelMap.entrySet()) {
                            if (schema.getField(entry.getKey()).getType().isSimpleType()) {
                                doc.setProperty(schema.getName(), entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }

                doc = session.saveDocument(doc);
                return doc;
            }
        };
    }

    private DataFetcher mutationDataFetcherForCreation(String targetDoctype) {
        return new DataFetcher() {
            @Override
            public DocumentModel get(DataFetchingEnvironment environment) {
                Map<String, Object> docInputMap = environment.getArgument(targetDoctype);

                String path = (String) docInputMap.get("path");
                String name = (String) docInputMap.get("name");

                CoreSession session = ((NuxeoGraphqlContext) environment.getContext()).getSession();
                DocumentModel doc = session.createDocumentModel(path, name, targetDoctype);

                SchemaManager sm = Framework.getService(SchemaManager.class);
                DocumentType docType = sm.getDocumentType(targetDoctype);
                for (Schema schema : docType.getSchemas()) {
                    String schemaName = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix
                            : schema.getName();
                    Map<String, Object> dataModelMap = (Map<String, Object>) docInputMap.get(schemaName);
                    if (dataModelMap != null) {
                        for (Entry<String, Object> entry : dataModelMap.entrySet()) {
                            if (schema.getField(entry.getKey()).getType().isSimpleType()) {
                                doc.setProperty(schema.getName(), entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }

                doc = session.createDocument(doc);
                return doc;
            }
        };
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
                                                                         .dataFetcher(
                                                                                 new DocPropertyDataFetcher())
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
                docTypeBuilder.field(
                        newFieldDefinition().type(GraphQLString)//
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
                        docTypeBuilder.field(newFieldDefinition().name(name)
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

    /**
     * Creates a GQL type for a Nuxeo schema
     *
     * @param schemaName
     * @return
     */
    private GraphQLInputObjectType inputTypeForSchema(String schemaName) {

        if (!inputTypesForSchema.containsKey(schemaName)) {
            SchemaManager sm = Framework.getService(SchemaManager.class);
            Schema s = sm.getSchema(schemaName);

            graphql.schema.GraphQLInputObjectType.Builder schemaBuilder = GraphQLInputObjectType.newInputObject().name(
                    "ischema_" + schemaName);

            for (Field f : s.getFields()) {
                if (!f.getName().getLocalName().matches("[_A-Za-z][_0-9A-Za-z]*")) {
                    continue;
                }

                Type t = f.getType();
                if (t.isSimpleType()) {
                    graphql.schema.GraphQLInputObjectField.Builder fieldBuilder = GraphQLInputObjectField.newInputObjectField()
                                                                                                         .name(f.getName()
                                                                                                                .getLocalName());
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
            inputTypesForSchema.put(schemaName, schemaBuilder.build());
        }
        return inputTypesForSchema.get(schemaName);
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
