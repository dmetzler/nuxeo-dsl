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
  const Facet = createToken({name: "Facet", pattern: /facet/ });
  const Identifier = createToken({name: "Identifier", pattern: /\w+/ });
  const Extends = createToken({name: "Extends", pattern: /extends/ });
  const Schemas = createToken({name: "Schemas", pattern: /schemas/ });
  const Aliases = createToken({name: "Aliases", pattern: /aliases/ });
  const Queries = createToken({name: "Queries", pattern: /queries/ });
  const Facets = createToken({name: "Facets", pattern: /facets/ });
  const Crud  = createToken({name: "Crud", pattern: /crud/ });
  const Lazy = createToken({name: "Lazy", pattern: /lazy/ });
  const LCurly = createToken({name: "LCurly", pattern: /{/});
  const RCurly = createToken({name: "RCurly", pattern: /}/});
  const LParenthesis = createToken({name: "LParenthesis", pattern: /\(/});
  const RParenthesis = createToken({name: "RParenthesis", pattern: /\)/});
  const Comma = createToken({ name: "Comma", pattern: /,/ })
  const Colon = createToken({ name: "Colon", pattern: /:/ })
  const StringLiteral = createToken({
        name: "StringLiteral",
        pattern: /"(?:[^\\"]|\\(?:[bfnrtv"\\/]|u[0-9a-fA-F]{4}))*"/
    })

  const WhiteSpace = createToken({
    name: "WhiteSpace",
    pattern: /\s+/,
    group: chevrotain.Lexer.SKIPPED,
    line_breaks: true
  });

  const Comment = createToken({
    name: 'Comment',
    pattern: /\/\*[^*]*\*+([^/*][^*]*\*+)*\//,
    group: chevrotain.Lexer.SKIPPED,
    line_breaks: true
  });

  const allTokens = [
    WhiteSpace,
    Comment,
    Comma,
    StringLiteral,
    Colon,
    DocType,
    Schemas,
    Facets,
    Aliases,
    Queries,
    Schema,
    Facet,
    Crud,
    Lazy,
    Extends,
    LCurly,
    RCurly,
    LParenthesis,
    RParenthesis,
    Identifier
  ]

    LCurly.LABEL = "'{'"
    RCurly.LABEL = "'}'"
    Comma.LABEL = "','"
    Colon.LABEL = "':'"

  let NuxeoLexer = new chevrotain.Lexer(allTokens)

class NuxeoDSLParser extends chevrotain.Parser {
      /**
     * @param {IToken[]}
     *            input
     */
      constructor(input) {
          super(input, allTokens, {recoveryEnabled: true, outputCst: true})

          const $ = this;

          $.RULE("NuxeoDSL", () => {
            $.MANY(() => {
                $.OR([
                 {ALT: () => { $.SUBRULE($.doctype)}},
                 {ALT: () => { $.SUBRULE($.schema)}},
                 {ALT: () => { $.SUBRULE($.facet)}},
                 {ALT: () => { $.SUBRULE($.queryList)}},
              ])
            })
          })

          $.RULE("doctype", () => {
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
                           {ALT: () => { $.SUBRULE($.schemaList)}},
                           {ALT: () => { $.SUBRULE($.facetList)}},
                           {ALT: () => { $.SUBRULE($.aliasList)}},
                           {ALT: () => { $.CONSUME(Crud)}},
                        ])
                    })
                    $.CONSUME1(RCurly)
              })

          })

          $.RULE("schemaList", () => {
            $.CONSUME(Schemas)
            $.CONSUME2(LCurly)
            $.OPTION3(() => {
              $.OR([
                {ALT: () => { $.SUBRULE($.schemaRef)}}
              ])

              $.MANY2(() => {
                $.OPTION4(() => {
                  $.CONSUME(Comma)
                })
                $.OR2([
                  {ALT: () => { $.SUBRULE2($.schemaRef)}}
                ])
              })
            })
            $.CONSUME2(RCurly)
          })

          $.RULE("facetList", () => {
            $.CONSUME(Facets)
            $.CONSUME(LCurly)
            $.MANY(() => {
              $.CONSUME1(Identifier)
            })
            $.CONSUME(RCurly)
          })

          $.RULE("aliasList", () => {
            $.CONSUME(Aliases)
            $.CONSUME(LCurly)
            $.MANY1(() => {
              $.SUBRULE($.aliasDef)
            })
            $.CONSUME(RCurly)
          })


          $.RULE("aliasDef", () => {
              $.CONSUME(Identifier)              
              $.CONSUME2(Identifier)
              $.CONSUME2(LCurly)
              $.AT_LEAST_ONE_SEP({SEP: Comma, DEF: () => {
                $.CONSUME3(StringLiteral)
              }})              
              $.CONSUME2(RCurly)
          })

          $.RULE("queryList", () => {
            $.CONSUME(Queries)
            $.CONSUME(LCurly)
            $.MANY1(() => {
              $.SUBRULE($.queryDef)
            })
            $.CONSUME(RCurly)
          })

          $.RULE("queryDef", () => {
              $.CONSUME1(Identifier)
              $.OPTION1(() => {
                $.SUBRULE($.queryParams)
              })
              $.OPTION2(() => {
                $.CONSUME(Colon)
                $.CONSUME3(Identifier)                
              })

              $.CONSUME3(StringLiteral)              
          })

          $.RULE("queryParams", () => {
            $.CONSUME(LParenthesis)
            $.AT_LEAST_ONE_SEP({SEP: Comma, DEF: () => {
              $.CONSUME2(Identifier)
            }})
            $.CONSUME(RParenthesis)
          })


          $.RULE("schemaRef", ()=> {
            $.OPTION1(() => {
              $.CONSUME1(Identifier)
              $.CONSUME(Colon)
            })
            $.CONSUME2(Identifier)
            $.OPTION2(()=> {
              $.CONSUME(Lazy)
            })
            $.OPTION3(()=> {
              $.SUBRULE2($.schemaBody)
            })
          })



          $.RULE("facet", () => {
              $.CONSUME(Facet)
              $.CONSUME(Identifier)
              $.OPTION2(() => {
                $.CONSUME(LCurly)
                   $.SUBRULE($.schemaList)
                $.CONSUME1(RCurly)
              })
          })


          $.RULE("schema", ()=> {
            $.CONSUME(Schema)
            $.OPTION(() => {
              $.CONSUME1(Identifier)
              $.CONSUME(Colon)
            })
            $.CONSUME2(Identifier)
            $.SUBRULE2($.schemaBody)

          })


          $.RULE("schemaBody", ()=> {
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
  const BaseVisitorWithDefault = parser.getBaseCstVisitorConstructorWithDefaults()

  class NuxeoInterpreter extends BaseVisitorWithDefault {
      constructor() {
          super()
          this.validateVisitor()
      }

      /* Visit methods go here */
      NuxeoDSL(ctx) {
        let result = {}

        if (ctx.schema.length > 0) {
          result.schemas = ctx.schema.map((schema)=> this.visit(schema))
        }

        if (ctx.doctype.length > 0) {
          result.doctypes = ctx.doctype.map((type) => {
            let doctype = this.visit(type)

            if(doctype.schemas) {
              let schemas = this.extractInlineSchemas(doctype)

              schemas.forEach((schema)=>{
                result.schemas = result.schemas || []
                result.schemas.push(schema)
              })
            }

            return doctype
          })
        }


        if (ctx.facet.length > 0) {
          result.facets = ctx.facet.map((facet) => this.visit(facet))
        }

        if(ctx.queryList.length > 0) {
          result.queries = this.visit(ctx.queryList)
        }        


        return result

      }

      extractInlineSchemas(doctype) {
        let inlineSchema = []
        doctype.schemas.forEach((schema) => {

          if(schema.hasOwnProperty("fields")) {

            if(!schema.hasOwnProperty("prefix")) {
              schema.prefix = schema.name
            }
            inlineSchema.push(schema)

            doctype.schemas = doctype.schemas.filter((s)=>s.name !== schema.name)            
            doctype.schemas.push({name: schema.name, lazy: schema.lazy})
          }
        })
        return inlineSchema
      }

      doctype(ctx) {
        let extendsValue = "Document"
        if(ctx.Identifier.length >1) {
          extendsValue = ctx.Identifier[1].image
        }

        let doctype = {
          name: ctx.Identifier[0].image,
          "extends": extendsValue
        }

        if(ctx.schemaList.length > 0) {
          doctype.schemas = this.visit(ctx.schemaList)
        }

        if(ctx.facetList.length > 0) {
          doctype.facets = this.visit(ctx.facetList)
        }

        if(ctx.aliasList.length > 0) {
          doctype.aliases = this.visit(ctx.aliasList)
        }

        if(ctx.Crud.length > 0) {
          doctype.crud = {}
        }       

        return doctype
      }

      schemaList(ctx) {
        if(ctx.schemaRef.length > 0) {
           return ctx.schemaRef.map((schema) => this.visit(schema))
        } else {
          return []
        }
      }

      facetList(ctx) {
        if(ctx.Identifier.length > 0) {
           return ctx.Identifier.map((id) => id.image)
        } else {
          return []
        }
      }

      aliasList(ctx) {
        if(ctx.aliasDef.length > 0){
          return ctx.aliasDef.map((alias)=> this.visit(alias))
        }
        return []
      }

          
      aliasDef(ctx) {
        const alias = {}
        alias.name = ctx.Identifier[0].image
        alias.type = ctx.Identifier[1].image
        alias.args = ctx.StringLiteral.map((s) => s.image.substr(0,s.image.length-1).substr(1))
        return alias
      }      

      queryList(ctx) {
        if(ctx.queryDef.length > 0){
          return ctx.queryDef.map((query)=> this.visit(query))
        }
        return []
      }

          
      queryDef(ctx) {
        const query = {}
        query.name = ctx.Identifier.shift().image
        const s = ctx.StringLiteral[0].image
        query.query = s.substr(0,s.length-1).substr(1)

        if(ctx.queryParams.length > 0) {
          query.params = this.visit(ctx.queryParams)
        } else {
          query.params = []
        }


        

        query.resultType = ctx.Identifier[0] ? ctx.Identifier[0].image : "document"
        return query
      } 


      queryParams(ctx) {
        if(ctx.Identifier.length > 0) {
          return ctx.Identifier.map((p)=> p.image)
        } 

      }     

      schemaRef(ctx) {

        let result = {
          lazy: ctx.Lazy.length > 0
        }

        if(ctx.Identifier.length > 1) {
          result.name = ctx.Identifier[1].image
          result.prefix = ctx.Identifier[0].image
        } else {
          result.name = ctx.Identifier[0].image

        }

        
        if(ctx.schemaBody.length > 0) {
          result.fields = this.visit(ctx.schemaBody)        
        }

        return result
      }

      schema(ctx) {
        var name = "", prefix = "";
        if(ctx.Identifier.length > 1) {
          name = ctx.Identifier[1].image
          prefix = ctx.Identifier[0].image
        } else {
          name = ctx.Identifier[0].image
          prefix = ctx.Identifier[0].image
        }



        let result = {
            name: name,
            fields: this.visit(ctx.schemaBody),
            prefix: prefix
        }
        //result.delete.lazy
        return result
      }


      facet(ctx) {
        let facet = {
          name: ctx.Identifier[0].image,
        }

        if(ctx.schemaList.length > 0) {
          facet.schemas = this.visit(ctx.schemaList)
        }

        return facet
      }

      schemaDescriptor(ctx) {
        return { name: ctx.Identifier[0].image, lazy: ctx.Lazy.length > 0 }
      }

      schemaBody(ctx) {
        return ctx.fieldDescriptor.map((field)=> this.visit(field))
                    .reduce((map,field)=>{
                      map[field.name] = {type: field.type}
                      return map
                    },{})

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
    NuxeoLexer: NuxeoLexer,
    NuxeoInterpreter: NuxeoInterpreter

  }


})