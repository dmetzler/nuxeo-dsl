package org.nuxeo.dsl.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.features.DocumentTypeFeature;
import org.nuxeo.dsl.features.DslSourceFeature;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

@SuppressWarnings("restriction")
public class DslParserImpl extends DefaultComponent implements DslParser {

    private static final Log log = LogFactory.getLog(DslParserImpl.class);

    private ScriptEngine engine;

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

    /**
     * Component deactivated notification. Called before a component is unregistered. Use this method to do cleanup if
     * any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification. Called after the application started. You can do here any initialization that
     * requires a working application (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Add some logic here to handle contributions
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Logic to do when unregistering any contribution
    }

    @Override
    @SuppressWarnings("unchecked")
    public DslModel parse(String dsl) {

        DslModel model = DslModel.builder().with(DocumentTypeFeature.class).with(DslSourceFeature.class).build();
        try {
            Map<String, Object> result = (Map<String, Object>) ((Invocable) engine).invokeFunction("parse", dsl);
            model.setSource(dsl);
            Map<String, Object> ast = (Map<String, Object>) result.get("value");
            model.visit(ast);
            return model;

        } catch (NoSuchMethodException | ScriptException e) {
            throw new NuxeoException(e);
        }
    }
}
