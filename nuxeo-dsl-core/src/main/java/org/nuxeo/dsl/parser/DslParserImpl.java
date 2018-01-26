package org.nuxeo.dsl.parser;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.DslModel.DslModelBuilder;
import org.nuxeo.dsl.features.DslFeature;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class DslParserImpl extends DefaultComponent implements DslParser {

    private static final String XP_FEATURE = "feature";

    private static final Log log = LogFactory.getLog(DslParserImpl.class);

    private ScriptEngine engine;

    private Set<Class<? extends DslFeature>> featureClasses = new HashSet<>();

    private DslModel.DslModelBuilder modelBuilder;

    /**
     * Component activated notification. Called when the component is activated. All component dependencies are resolved
     * at that moment. Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);

        log.info("Instanticating parser");
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            // Nashorn polyfill
            importJs(engine, "/js/lib/global-polyfill.js");
            importJs(engine, "/js/lib/chevrotain.min.js");

            importJs(engine, "/js/nuxeo_dsl.js");
            importJs(engine, "/js/nuxeo_dsl_javainterpreter.js");

            engine.eval("var parse = function(dsl) { return global.nuxeo_dsl_javainterpreter.parse(dsl)}");
            log.info("DSL compilator compiled");
        } catch (ScriptException | FileNotFoundException e) {

            log.error("Unable to compile DSL compilator", e);
            throw new NuxeoException(e);
        }
    }

    /**
     * @param engine
     * @param string
     * @throws ScriptException
     * @throws FileNotFoundException
     * @since TODO
     */
    private void importJs(ScriptEngine engine, String file) throws FileNotFoundException, ScriptException {
        InputStream is = getClass().getResourceAsStream(file);
        engine.eval(new InputStreamReader(is));
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_FEATURE.equals(extensionPoint)) {
            FeatureDescriptor desc = (FeatureDescriptor) contribution;
            featureClasses.add(desc.klass);
            modelBuilder = null;
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_FEATURE.equals(extensionPoint)) {
            FeatureDescriptor desc = (FeatureDescriptor) contribution;
            featureClasses.remove(desc.klass);
            modelBuilder = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DslModel parse(String dsl) {

        // TODO make the builder pluggable
        DslModel model = getDslModelBuilder().build();
        try {
            Map<String, Object> result = (Map<String, Object>) ((Invocable) engine).invokeFunction("parse", dsl);
            Map<String, Object> ast = (Map<String, Object>) result.get("value");

            model.setSource(dsl);
            model.visit(ast);
            return model;

        } catch (NoSuchMethodException | ScriptException e) {
            throw new NuxeoException(e);
        }
    }

    private DslModelBuilder getDslModelBuilder() {
        if (modelBuilder == null) {
            modelBuilder = DslModel.builder();

            for (Class<? extends DslFeature> featureKlass : featureClasses) {
                modelBuilder.with(featureKlass);
            }
        }
        return modelBuilder;
    }
}
