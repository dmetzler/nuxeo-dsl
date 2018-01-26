// wrapping in UMD to allow code to work both in node.js (the tests/specs)
// and in the browser (css_diagrams.html)
;(function(root, factory) {
    if (typeof module === "object" && module.exports) {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like environments that support module.exports,
        // like Node.
        module.exports = factory(require("chevrotain"), require("nuxeo_dsl"))
    } else {
        // Browser globals (root is window)\
        root["nuxeo_dsl_javainterpreter"] = factory(root.chevrotain,root.global.nuxeo_dsl)
    }
})(global, function(chevrotain, nuxeo_dsl) {


	const parser = new nuxeo_dsl.NuxeoDSLParser([]);
	const NuxeoLexer = nuxeo_dsl.NuxeoLexer;

	const BaseVisitor = parser.getBaseCstVisitorConstructor()
	const BaseVisitorWithDefault = parser.getBaseCstVisitorConstructorWithDefaults()




	const ArrayList = Java.type('java.util.ArrayList');	
	const Map = Java.type('java.util.HashMap');
	const DocumentTypeDescriptor = Java.type("org.nuxeo.ecm.core.schema.DocumentTypeDescriptor")
	const SchemaDescriptor = Java.type("org.nuxeo.ecm.core.schema.SchemaDescriptor")
	const SchemaBindingDescriptor = Java.type("org.nuxeo.ecm.core.schema.SchemaBindingDescriptor")


	class NuxeoInterpreter extends nuxeo_dsl.NuxeoInterpreter {
	      constructor() {
	          super()
	          // The "validateVisitor" method is a helper utility which performs
	      // static analysis
	          // to detect missing or redundant visitor methods
	          this.validateVisitor()
	      }


	      /* Visit methods go here */
	      NuxeoDSL(ctx) {
	    	let ast = super.NuxeoDSL(ctx)
	    	let result = new Map()


	        if (ast.schemas && ast.schemas.length > 0) {
	          var schemas = new ArrayList();
	          result.put("schemas", schemas)
	          
	          ast.schemas.map((schema)=> {

	          	var item = { fields: new ArrayList()}
		        item.descriptor = new SchemaBindingDescriptor()
		        item.descriptor.name = schema.name
		        item.descriptor.prefix = schema.prefix

	          	for (var name in schema.fields) {
	          		var field = new Map()
	          		field.put("name", name)
	          		field.put("type", schema.fields[name].type)
	          		item.fields.add(field)
	          	}
	          	schemas.add(item)
	          })
	        }


	        if (ast.doctypes && ast.doctypes.length > 0) {
	          var doctypes = new ArrayList()
	          ast.doctypes.forEach(function(d){

		          var descriptor = new DocumentTypeDescriptor();
		          descriptor.name = d.name
		          descriptor.superTypeName = d.extends;

		          if(d.facets && d.facets.length > 0) {
			          descriptor.facets = d.facets
			      }

			      if(d.schemas && d.schemas.length > 0) {
			      	descriptor.schemas = d.schemas.map( s => {
			      		var sd = new SchemaDescriptor()
			      		sd.name = s.name
			      		sd.isLazy = s.lazy
			      		return sd;
			      	})
			      }


		          doctypes.add(descriptor)

	          })
	          result.put("doctypes", doctypes )
	        }

	        return result

	      }

	  }



	  const NuxeoInterpreterInstance = new NuxeoInterpreter()


	  return {
	    parse: function(text) {
	            var lexResult = NuxeoLexer.tokenize(text)
	            // setting a new input will RESET the parser instance's state.
	            parser.input = lexResult.tokens
	            // any top level rule may be used as an entry point

	            var value = parser.NuxeoDSL()

	            if (parser.errors.length > 0) {
	              throw Error(
	                  "Sad sad panda, parsing errors detected!\n" +
	                      parser.errors[0].message
	              )
	          }

	            var result = new Map()
	            result.put("value", NuxeoInterpreterInstance.visit(value))
	            result.put("lexErrors", lexResult.errors)
	            result.put("parseErrors", "")
	            return result
	        }

	  }



})