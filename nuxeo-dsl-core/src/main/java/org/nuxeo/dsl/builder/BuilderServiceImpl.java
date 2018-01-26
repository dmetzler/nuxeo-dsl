package org.nuxeo.dsl.builder;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.builder.Compiler.CompilerBuilder;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class BuilderServiceImpl extends DefaultComponent implements BuilderService {

    private static final String XP_BUILDER = "builder";

    private Set<Class<? extends Builder>> builders = new HashSet<>();

    private Compiler compiler;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_BUILDER.equals(extensionPoint)) {
            BuilderDescriptor desc = (BuilderDescriptor) contribution;
            builders.add(desc.klass);
            compiler = null;
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_BUILDER.equals(extensionPoint)) {
            BuilderDescriptor desc = (BuilderDescriptor) contribution;
            builders.remove(desc.klass);
            compiler = null;
        }
    }

    @Override
    public void compile(DslModel model, BuildContext ctx) {
        getCompiler().compile(model, ctx);
    }

    private Compiler getCompiler() {
        if (compiler == null) {
            CompilerBuilder builder = Compiler.builder();
            for (Class<? extends Builder> builderKlass : builders) {
                builder.with(builderKlass);
            }
            compiler = builder.build();
        }
        return compiler;
    }
}
