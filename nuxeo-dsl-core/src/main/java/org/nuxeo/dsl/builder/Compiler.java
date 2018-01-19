package org.nuxeo.dsl.builder;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.features.DslFeature;

public class Compiler {

    private final Set<Builder> builders;

    private Compiler(Set<Builder> builders) {
        this.builders = builders;
    }

    public <T extends DslFeature> void compile(DslModel model, BuildContext ctx) {
        for (Builder builder : builders) {
            builder.build(model.getFeature(builder.getFeatureClass()), ctx);
        }
    }

    public static CompilerBuilder builder() {
        return new CompilerBuilder();
    }

    public static class CompilerBuilder {
        private Set<Builder> builders = new HashSet<>();

        private CompilerBuilder() {
        }

        public CompilerBuilder with(Class<? extends Builder> builder) {
            try {
                builders.add(builder.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("DslFeature must have a public blank constructor");
            }
            return this;
        }

        public Compiler build() {
            return new Compiler(builders);
        }

    }
}
