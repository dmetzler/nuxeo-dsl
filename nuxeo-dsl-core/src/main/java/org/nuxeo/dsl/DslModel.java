package org.nuxeo.dsl;

import java.util.Map;

import org.nuxeo.dsl.features.DslFeature;

import com.google.common.collect.ImmutableMap;;

public class DslModel {

    private final ImmutableMap<Class<? extends DslFeature>, DslFeature> features;

    public DslModel(Class<? extends DslFeature>[] featureClasses) {
        ImmutableMap.Builder<Class<? extends DslFeature>, DslFeature> builder = new ImmutableMap.Builder<Class<? extends DslFeature>,DslFeature>();
        for(Class<? extends DslFeature> klass : featureClasses) {
            try {
                builder.put(klass, klass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("DslFeature must have a public blank constructor");
            }
        }
        features = builder.build();
    }

    public void visit(Map<String, Object> ast) {
         for(DslFeature feature : features.values()) {
             feature.visit(ast);
         }
    }

    @SuppressWarnings("unchecked")
    public <T extends DslFeature> T  getFeature(Class<T> klass) {
        return (T) features.get(klass);
    }

    public static class Builder {
        public static DslModel make(Class<? extends DslFeature>... classes ) {
            return new DslModel(classes);
        }
    }



}
