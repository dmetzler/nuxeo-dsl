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
})(this, function(chevrotain, nuxeo_dsl) {

	const parser = new nuxeo_dsl.NuxeoDSLParser([]);
	const NuxeoLexer = nuxeo_dsl.NuxeoLexer;

	const BaseVisitor = parser.getBaseCstVisitorConstructor()


	const ArrayList = Java.type('java.util.ArrayList');
	const Map = Java.type('java.util.HashMap');


	class NuxeoInterpreter extends BaseVisitor {
	      constructor() {
	          super()
	          // The "validateVisitor" method is a helper utility which performs
	      // static analysis
	          // to detect missing or redundant visitor methods
	          this.validateVisitor()
	      }


	      /* Visit methods go here */
	      NuxeoDSL(ctx) {
	        let result = new Map()

	        if (ctx.schemaDef.length > 0) {
	          var schemas = new ArrayList();
	          result.put("schemas", schemas)
	          ctx.schemaDef.map((schema)=> this.visit(schema)).forEach(s => schemas.add(s))
	        }

	        if (ctx.docType.length > 0) {
	          var doctypes = new ArrayList()
	          result.put("doctypes", doctypes )
	          ctx.docType.map((type) => this.visit(type)).forEach(d => doctypes.add(d))
	        }

	        return result

	      }

	      docType(ctx) {

	          let extendsValue = "Document"
	          if(ctx.Identifier.length >1) {
	            extendsValue = ctx.Identifier[1].image
	          }

	          var DocumentTypeDescriptor = Java.type("org.nuxeo.ecm.core.schema.DocumentTypeDescriptor")
	          var descriptor = new DocumentTypeDescriptor();


	          descriptor.name = ctx.Identifier[0].image
	          descriptor.superTypeName = extendsValue;




	          return descriptor;
	      }




	      schemaDescriptor(ctx) {
	        return { name: ctx.Identifier[0].image, lazy: ctx.Lazy.length > 0 }
	      }

	      schemaDef(ctx) {
	        return {
	          name: ctx.Identifier[0].image,
	          prefix: "",
	          fields: ctx.fieldDescriptor.map((field)=> this.visit(field)).reduce((map,field)=>{
	                      map[field.name] = {type: field.type}
	                      return map
	                    },{})
	        }
	      }

	      fieldDescriptor(ctx) {
	        var fieldType = "String"
	        if(ctx.fieldType.length>0) {
	          fieldType = this.visit(ctx.fieldType)
	        }

	        return { name: ctx.Identifier[0].image, type: fieldType}

	      }

	      fieldType(ctx) {
	        return ctx.Identifier[0].image

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