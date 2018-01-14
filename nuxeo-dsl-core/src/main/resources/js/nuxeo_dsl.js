// wrapping in UMD to allow code to work both in node.js (the tests/specs)
// and in the browser (css_diagrams.html)
;(function(root, factory) {
    if (typeof module === "object" && module.exports) {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like environments that support module.exports,
        // like Node.
        module.exports = factory(require("chevrotain"))
    } else {
        // Browser globals (root is window)\
        root["nuxeo_dsl"] = factory(root.chevrotain)
    }
})(global, function(chevrotain) {

  const createToken = chevrotain.createToken


  const DocType = createToken({name: "DocType", pattern: /doctype/, label: 'docType'});
  const Schema = createToken({name: "Schema", pattern: /schema/ });
  const Identifier = createToken({name: "Identifier", pattern: /\w+/ });
  const Extends = createToken({name: "Extends", pattern: /extends/ });
  const Schemas = createToken({name: "Schemas", pattern: /schemas/ });
  const Lazy = createToken({name: "Lazy", pattern: /lazy/ });
  const LCurly = createToken({name: "LCurly", pattern: /{/});
    const RCurly = createToken({name: "RCurly", pattern: /}/});
    const Comma = createToken({ name: "Comma", pattern: /,/ })
    const WhiteSpace = createToken({
      name: "WhiteSpace",
      pattern: /\s+/,
      group: chevrotain.Lexer.SKIPPED,
      line_breaks: true
  });

  const allTokens = [
    WhiteSpace,
    Comma,
    DocType,
    Schemas,
    Schema,
    Lazy,
    Extends,
    LCurly,
    RCurly,
    Identifier
  ]

    LCurly.LABEL = "'{'"
    RCurly.LABEL = "'}'"
    Comma.LABEL = "','"

  let NuxeoLexer = new chevrotain.Lexer(allTokens)

  class NuxeoDSLParser extends chevrotain.Parser {
      /**
     * @param {IToken[]}
     *            input
     */
      constructor(input) {
          super(input, allTokens, {outputCst: true})

          const $ = this;

          $.RULE("NuxeoDSL", () => {
            $.MANY(() => {
                $.OR([
                 {ALT: () => { $.SUBRULE($.docType)}},
                 {ALT: () => { $.SUBRULE($.schemaDef)}},
              ])
            })
          })

          $.RULE("docType", () => {
              $.CONSUME(DocType)
              $.CONSUME1(Identifier)
              $.OPTION1(() => {
                $.CONSUME(Extends)
                  $.CONSUME2(Identifier)
              })
              $.OPTION2(() => {
                $.CONSUME1(LCurly)
                    $.MANY1(() => {
                        $.OR([
                           {ALT: () => {
                             $.CONSUME(Schemas)
                             $.CONSUME2(LCurly)
                             $.OPTION3(() => {
                               $.SUBRULE($.schemaDescriptor)
                               $.MANY2(() => {
                                 $.OPTION4(() => {
                                   $.CONSUME(Comma)
                                 })
                                 $.SUBRULE2($.schemaDescriptor)
                               })
                             })
                             $.CONSUME2(RCurly)
                           }},
                        ])
                    })
                    $.CONSUME1(RCurly)
              })
          })


          $.RULE("schemaDescriptor", ()=> {
            $.CONSUME(Identifier)
            $.OPTION(()=> {
              $.CONSUME(Lazy)
            })
          })


          $.RULE("schemaDef", ()=> {
            $.CONSUME(Schema)
            $.CONSUME(Identifier)
            $.CONSUME(LCurly)
            $.OPTION(() => {
                $.SUBRULE($.fieldDescriptor)
                $.MANY(() => {
                    $.CONSUME(Comma)
                    $.SUBRULE2($.fieldDescriptor)
                })
            })
            $.CONSUME(RCurly)
          })

          $.RULE("fieldDescriptor", () => {
            $.CONSUME(Identifier)
            $.OPTION(() => {
                $.SUBRULE($.fieldType)
            })
          })

          $.RULE("fieldType", () => {
            $.CONSUME(Identifier)
          })

          chevrotain.Parser.performSelfAnalysis(this)
      }
    }

      // BaseVisitor constructors are accessed via a parser instance.
  const parser = new NuxeoDSLParser([]);

  const BaseVisitor = parser.getBaseCstVisitorConstructor()
  const BaseVisitorWithDefaults = parser.getBaseCstVisitorConstructorWithDefaults()

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
        let result = {}

        if (ctx.schemaDef.length > 0) {
          result.schemas = ctx.schemaDef.map((schema)=> this.visit(schema))
        }

        if (ctx.docType.length > 0) {
          result.doctypes = ctx.docType.map((type) => this.visit(type))
        }

        return result

      }

      docType(ctx) {
            let extendsValue = "Document"
            if(ctx.Identifier.length >1) {
          extendsValue = ctx.Identifier[1].image
            }

        let doctype = {
          name: ctx.Identifier[0].image,
          "extends": extendsValue
        }

            if(ctx.schemaDescriptor.length > 0) {
          doctype.schemas = ctx.schemaDescriptor.map((schema) => this.visit(schema))
        }

        return doctype
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



  class NuxeoInterpreter2 extends BaseVisitor {
      constructor() {
          super()
          // The "validateVisitor" method is a helper utility which performs
      // static analysis
          // to detect missing or redundant visitor methods
          this.validateVisitor()
      }


      /* Visit methods go here */
      NuxeoDSL(ctx) {
        let result = {}

        if (ctx.schemaDef.length > 0) {
          result.schemas = ctx.schemaDef.map((schema)=> this.visit(schema))
        }

        if (ctx.docType.length > 0) {
          result.doctypes = ctx.docType.map((type) => this.visit(type))
        }

        return result

      }

      docType(ctx) {

          let extendsValue = "Document"
          if(ctx.Identifier.length >1) {
            extendsValue = ctx.Identifier[1].image
          }

          var type = Java.type("org.nuxeo.ecm.core.schema.DocumentTypeDescriptor")
          var descriptor = new type();


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


            return {
                value: NuxeoInterpreterInstance.visit(value),
                lexErrors: lexResult.errors,
                parseErrors: parser.errors
            }
        },

        NuxeoDSLParser: NuxeoDSLParser,
        NuxeoLexer: NuxeoLexer

  }


})