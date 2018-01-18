package org.nuxeo.dsl.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.features.DocumentTypeFeature;
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
            importJs(engine, "js/lib/global-polyfill.js");
            importJs(engine, "js/lib/chevrotain.min.js");

            File compiledJS = getFile("js/dist/nuxeo_dsl.js");
            if (compiledJS != null && compiledJS.exists()) {
                importJs(engine, "js/dist/nuxeo_dsl.js");
                importJs(engine, "js/dist/nuxeo_dsl_javainterpreter.js");
            } else {
                log.info("Transpiling parser (babel)");
                importJs(engine, "js/lib/babel.min.js");
                importES(engine, "js/nuxeo_dsl.js");
                importES(engine, "js/nuxeo_dsl_javainterpreter.js");
            }

            engine.eval("var parse = function(dsl) { return global.nuxeo_dsl_javainterpreter.parse(dsl)}");
            log.info("DSL compilator compiled");
        } catch (ScriptException | FileNotFoundException e) {
            log.error(e);
            throw new NuxeoException("Unable to compile DSL compilator", e);
        }
    }

    private static void importES(ScriptEngine engine, String file) throws ScriptException {
        String unTranspiledJsx = Utilities.readFromResourcesAsString(file);
        engine.put("input", unTranspiledJsx);
        String transpileJavaScript = (String) engine.eval("Babel.transform(input, { presets: ['es2015'] }).code");
        engine.eval(transpileJavaScript);
    }

    /**
     * @param engine
     * @param string
     * @throws ScriptException
     * @throws FileNotFoundException
     * @since TODO
     */
    private static void importJs(ScriptEngine engine, String file) throws FileNotFoundException, ScriptException {
        File f = getFile(file);
        engine.eval(new FileReader(f));
    }

    private static File getFile(String file) {
        URL lib = DslParserImpl.class.getClassLoader().getResource(file);
        return lib != null ? new File(lib.getFile()) : null;
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
    public DslModel parse(String dsl) {
        DslModel model = DslModel.Builder.make(DocumentTypeFeature.class);
        try {
            Map<String, Object> result = (Map) ((Invocable) engine).invokeFunction("parse", dsl);

            model.visit((Map<String, Object>) result.get("value"));
            return model;

        } catch (NoSuchMethodException | ScriptException e) {
            throw new NuxeoException(e);
        }
    }
}
