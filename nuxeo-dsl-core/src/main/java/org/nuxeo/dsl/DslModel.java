package org.nuxeo.dsl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.dsl.features.DslFeature;

import com.google.common.collect.ImmutableMap;;

public class DslModel {

    private final ImmutableMap<Class<? extends DslFeature>, DslFeature> features;
    private String src;

    private DslModel(Set<Class<? extends DslFeature>> featureClasses) {
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
             feature.visit(this, ast);
         }
    }


    @SuppressWarnings("unchecked")
    public <T extends DslFeature> T  getFeature(Class<T> klass) {
        return (T) features.get(klass);
    }

    public static DslModelBuilder builder() {
        return new DslModelBuilder();
    }

    public static class DslModelBuilder {
        Set<Class<? extends DslFeature>> classes = new HashSet<>();

        public DslModel build() {
            return new DslModel(classes);
        }

        public DslModelBuilder with(Class<? extends DslFeature> klass) {
            classes.add(klass);
            return this;
        }
    }

    public void setSource(String src) {
        this.src = src;
    }

    public String getSource() {
        return src;
    }



}
