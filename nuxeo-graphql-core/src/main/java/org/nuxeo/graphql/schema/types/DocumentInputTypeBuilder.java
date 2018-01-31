package org.nuxeo.graphql.schema.types;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;

import java.util.Map;

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
import org.nuxeo.graphql.GraphQLService;
import org.nuxeo.runtime.api.Framework;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputObjectType.Builder;

public class DocumentInputTypeBuilder extends GraphQLInputObjectType.Builder {

    private String docType;

    private DocumentInputTypeBuilder(String docType) {
        this.docType = docType;

    }

    @Override
    public GraphQLInputObjectType build() {
        SchemaManager sm = Framework.getService(SchemaManager.class);

        Builder inputTypeBuilder = GraphQLInputObjectType.newInputObject().name(docType + "Input");

        inputTypeBuilder.field(newInputObjectField().name("path").type(GraphQLString));
        inputTypeBuilder.field(newInputObjectField().name("id").type(GraphQLString));
        inputTypeBuilder.field(newInputObjectField().name("name").type(GraphQLString));

        for (Schema schema : sm.getDocumentType(docType).getSchemas()) {
            String name = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix : schema.getName();

            GraphQLInputObjectType inputTypeForSchema = inputTypeForSchema(schema.getName());
            if (!inputTypeForSchema.getFieldDefinitions().isEmpty()) {
                inputTypeBuilder.field(newInputObjectField().name(name).type(inputTypeForSchema));
            }
        }

        GraphQLInputObjectType inputType = inputTypeBuilder.build();
        return inputType;
    }


    private GraphQLInputObjectType inputTypeForSchema(String schemaName) {
        GraphQLService gql = Framework.getService(GraphQLService.class);
        Map<String, GraphQLInputObjectType> inputTypesForSchema = gql.getSchemaManager().getInputTypeRegistry();
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


    public static DocumentInputTypeBuilder type(String docType) {
        return new DocumentInputTypeBuilder(docType);
    }

}
