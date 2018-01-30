package org.nuxeo.dsl.builder.v9;

import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.Builder;
import org.nuxeo.dsl.features.DslFeature;
import org.nuxeo.dsl.features.GraphQLFeature;
import org.nuxeo.graphql.descriptors.AliasDescriptor;
import org.nuxeo.graphql.descriptors.QueryDescriptor;

public class GraphQLBuilder implements Builder {

    @Override
    public void build(DslFeature f, BuildContext ctx) {


        ctx.registerXMap(AliasDescriptor.class);
        ctx.registerXMap(QueryDescriptor.class);
        GraphQLFeature feature = (GraphQLFeature) f;

        feature.getAliases().stream().forEach(a -> ctx.registerXP("org.nuxeo.graphql.component", "alias", a));
        feature.getQueries().stream().forEach(a -> ctx.registerXP("org.nuxeo.graphql.component", "query", a));

    }

    @Override
    public Class<? extends DslFeature> getFeatureClass() {
        return GraphQLFeature.class;
    }

}
