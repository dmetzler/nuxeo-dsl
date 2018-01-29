package org.nuxeo.graphql;

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
import java.util.Map.Entry;
import java.util.Set;

import javax.el.ELContext;

import org.apache.commons.lang3.StringUtils;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
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
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.api.Framework;

import graphql.TypeResolutionEnvironment;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
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

    private ExpressionEvaluator el = new ExpressionEvaluator(new ExpressionFactoryImpl());

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
        Builder builder = newObject().name("nuxeo")
                          .field(getDocumentQueryTypeField())
                          .field(getNXQLQueryTypeField());
        for(QueryDescriptor query : queries.values()) {
            builder.field(getQueryFieldType(query));
        }
        return builder.build();


    }

    private GraphQLFieldDefinition getQueryFieldType(QueryDescriptor query) {
        graphql.schema.GraphQLFieldDefinition.Builder builder = newFieldDefinition().name(query.name);
        if(query.args.size() >0) {
            for(String arg : query.args) {
                builder.argument(new GraphQLArgument(arg, new GraphQLNonNull(GraphQLString)));
            }
        }

        if("document".equals(query.resultType)) {
            builder.type(new GraphQLList(documentInterface));
        } else {
            builder.type(new GraphQLList(docTypeToGQLType.get(query.resultType)));
        }

        builder.dataFetcher(getQueryDataFetcher(query.query));
        return builder.build();

    }

    private GraphQLFieldDefinition getNXQLQueryTypeField() {
        return newFieldDefinition().name("documents")
                                   .type(new GraphQLList(documentInterface))
                                   .argument(new GraphQLArgument("nxql", new GraphQLNonNull(GraphQLString)))
                                   .dataFetcher(getNxqlQueryFetcher())
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
                                   .dataFetcher(getDocFetcher())
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
                                                                                 getDocPropertyFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(GraphQLString)//
                                                                         .name("id")
                                                                         .dataFetcher(getDocPropertyFetcher())
                                                                         .build())
                                              .field(newFieldDefinition().type(
                                                      new GraphQLList(new GraphQLTypeReference("document")))
                                                                         .name("children")
                                                                         .dataFetcher(getChildrenDataFetcher())
                                                                         .build())

                                              .typeResolver(getNuxeoDocumentTypeResolver())
                                              .build();

            docTypeToGQLType = new HashMap<>();
            SchemaManager sm = Framework.getService(SchemaManager.class);

            for (DocumentType type : sm.getDocumentTypes()) {
                Builder docTypeBuilder = newObject().name(type.getName()).withInterface(documentInterface);
                docTypeBuilder.field(newFieldDefinition().type(GraphQLString)//
                                                         .name("path")
                                                         .dataFetcher(getDocPropertyFetcher())
                                                         .build())
                              .field(newFieldDefinition().type(GraphQLString)//
                                                         .name("id")
                                                         .dataFetcher(getDocPropertyFetcher())
                                                         .build())
                              .field(newFieldDefinition().type(new GraphQLList(new GraphQLTypeReference("document")))
                                                         .name("children")
                                                         .dataFetcher(getChildrenDataFetcher())
                                                         .build());

                for (Schema schema : type.getSchemas()) {
                    String name = schema.getNamespace().prefix;
                    name = StringUtils.isNotBlank(name) ? name : schema.getName();
                    GraphQLObjectType typeForSchema = typeForSchema(schema.getName());

                    if (!typeForSchema.getFieldDefinitions().isEmpty()) {
                        docTypeBuilder.field(
                                newFieldDefinition().name(name)
                                                    .type(typeForSchema)
                                                    .dataFetcher(getSchemaFetcher(schema))
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

            if(alias.args.size()>1) {
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
            return dataModelPropertyFetcher(alias.args.get(0));
        } else if ("query".equals(alias.type)) {
            return getQueryDataFetcher(alias.args.get(0));
        } else {
            return null;
        }
    }

    private DataFetcher getQueryDataFetcher(String query) {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                CoreSession session = getSession(environment.getContext());
                if (session != null) {
                    String finalQuery = query;

                    ELService elService = Framework.getService(ELService.class);
                    ELContext elContext = elService.createELContext();
                    el.bindValue(elContext, "principal", session.getPrincipal());

                    if(environment.getArguments().size()>0) {
                        for(Entry<String,Object> paramEntry : environment.getArguments().entrySet()) {
                            el.bindValue(elContext, paramEntry.getKey(), paramEntry.getValue());
                        }
                    }

                    if (environment.getSource() instanceof DocumentModel) {
                        DocumentModel doc = (DocumentModel) environment.getSource();
                        el.bindValue(elContext, "this", doc);
                    }

                    finalQuery = el.evaluateExpression(elContext, query, String.class);
                    return session.query(finalQuery);
                }
                return null;
            }

        };
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
                                    dataModelPropertyFetcher(f.getName().getPrefixedName()));
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

    private DataFetcher dataModelPropertyFetcher(final String prefixedName) {
        return new DataFetcher() {

            @Override
            public Object get(DataFetchingEnvironment environment) {
                Object source = environment.getSource();
                if (source instanceof DataModel) {
                    DataModel dm = (DataModel) source;
                    return dm.getValue(prefixedName);
                } else if (source instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) source;
                    return doc.getPropertyValue(prefixedName);
                }
                return null;
            }
        };
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

    private CoreSession getSession(Object ctx) {
        if (ctx instanceof NuxeoGraphqlContext) {
            return ((NuxeoGraphqlContext) ctx).getSession();
        }
        return null;
    }

    private DataFetcher getDocFetcher() {
        return new DataFetcher() {

            public Object get(DataFetchingEnvironment environment) {
                String path = environment.getArgument("path");
                String id = environment.getArgument("id");

                CoreSession session = getSession(environment.getContext());
                if (session != null) {
                    if (path != null) {
                        return session.getDocument(new PathRef(path));
                    }
                    if (id != null) {
                        return session.getDocument(new IdRef(id));
                    }
                }
                return null;
            }
        };

    }

    private DataFetcher getNxqlQueryFetcher() {
        return new DataFetcher() {

            @Override
            public Object get(DataFetchingEnvironment environment) {
                String nxql = environment.getArgument("nxql");
                CoreSession session = getSession(environment.getContext());
                if (session != null) {
                    return session.query(nxql);
                }
                return null;
            }
        };
    }

    private DataFetcher getChildrenDataFetcher() {
        return new DataFetcher() {
            @Override
            public Object get(DataFetchingEnvironment environment) {
                CoreSession session = getSession(environment.getContext());
                if (session != null) {

                    if (environment.getSource() instanceof DocumentModel) {
                        DocumentModel doc = (DocumentModel) environment.getSource();
                        return session.getChildren(doc.getRef());
                    }
                    return null;
                }
                return null;
            }

        };
    }

    private DataFetcher getSchemaFetcher(final Schema schema) {
        return new DataFetcher() {

            @Override
            public Object get(DataFetchingEnvironment environment) {
                if (environment.getSource() instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) environment.getSource();
                    return doc.getDataModel(schema.getName());
                }
                return null;
            }

        };
    }

    private DataFetcher getDocPropertyFetcher() {
        return new DataFetcher() {
            public Object get(DataFetchingEnvironment environment) {
                String fieldName = environment.getFields().get(0).getName();
                if (environment.getSource() instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) environment.getSource();
                    if ("path".equals(fieldName)) {
                        return doc.getPathAsString();
                    } else if ("id".equals(fieldName)) {
                        return doc.getId();
                    }
                }
                return null;
            }

        };
    }

}
