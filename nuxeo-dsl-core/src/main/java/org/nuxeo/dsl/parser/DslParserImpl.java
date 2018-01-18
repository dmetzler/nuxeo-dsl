package org.nuxeo.dsl.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@SuppressWarnings("restriction")
public class DslParserImpl extends DefaultComponent implements DslParser {

	private static final Log log = LogFactory.getLog(DslParserImpl.class);

	private ScriptEngine engine;

	/**
	 * Component activated notification. Called when the component is activated.
	 * All component dependencies are resolved at that moment. Use this method
	 * to initialize the component.
	 *
	 * @param context
	 *            the component context.
	 */
	@Override
	public void activate(ComponentContext context) {
		super.activate(context);

		log.info("Start compiling DSL compilator");
		engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			// Nashorn polyfill
			importJs(engine, "js/lib/global-polyfill.js");
			importJs(engine, "js/lib/babel.min.js");
			importJs(engine, "js/lib/chevrotain.min.js");
			importES(engine, "js/nuxeo_dsl.js");
			importES(engine, "js/nuxeo_dsl_javainterpreter.js");
			engine.eval("var parse = function(dsl) { return global.nuxeo_dsl_javainterpreter.parse(dsl)}");
			log.info("DSL compilator compiled");
		} catch (ScriptException | FileNotFoundException e) {
			log.error(e);
			throw new NuxeoException("Unable to compile DSL compilator",e);
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
		URL lib = DslParserImpl.class.getClassLoader().getResource(file);
		engine.eval(new FileReader(new File(lib.getFile())));
	}

	/**
	 * Component deactivated notification. Called before a component is
	 * unregistered. Use this method to do cleanup if any and free any resources
	 * held by the component.
	 *
	 * @param context
	 *            the component context.
	 */
	@Override
	public void deactivate(ComponentContext context) {
		super.deactivate(context);
	}

	/**
	 * Application started notification. Called after the application started.
	 * You can do here any initialization that requires a working application
	 * (all resolved bundles and components are active at that moment)
	 *
	 * @param context
	 *            the component context. Use it to get the current bundle
	 *            context
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
	public Collection<Object> parse(String dsl) {

		try {
			Map<String, Object> result = (Map) ((Invocable) engine).invokeFunction("parse", dsl);
			Map<String, Object> value = (Map<String, Object>) result.get("value");
			List<DocumentTypeDescriptor> descs = (List<DocumentTypeDescriptor>) value.get("doctypes");
			for (DocumentTypeDescriptor d : descs) {
				d.schemas = new SchemaDescriptor[] {};
				d.facets = new String[] {};
				SchemaManagerImpl sm = (SchemaManagerImpl) Framework.getService(SchemaManager.class);
				sm.registerDocumentType(d);
			}
			return Collections.singletonList(descs);

		} catch (NoSuchMethodException | ScriptException e) {
			throw new NuxeoException(e);
		}
	}
}
