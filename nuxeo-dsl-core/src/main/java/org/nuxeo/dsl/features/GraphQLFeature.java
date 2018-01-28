package org.nuxeo.dsl.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.dsl.DslModel;
import org.nuxeo.graphql.AliasDescriptor;
import org.nuxeo.graphql.QueryDescriptor;

public class GraphQLFeature implements DslFeature{

    private List<AliasDescriptor> aliases = new ArrayList<>();
    private List<QueryDescriptor> queries = new ArrayList<>();;
    @SuppressWarnings("unchecked")
    @Override
    public void visit(DslModel model, Map<String, Object> ast) {

        if(ast.get("aliases") != null) {
             aliases.addAll((List<AliasDescriptor>) ast.get("aliases"));
        }

        if(ast.get("queries") != null) {
            queries.addAll((List<QueryDescriptor>) ast.get("queries"));
       }
    }
    public List<AliasDescriptor> getAliases() {
        return aliases;
    }
    public List<QueryDescriptor> getQueries() {
        return queries;
    }



}
