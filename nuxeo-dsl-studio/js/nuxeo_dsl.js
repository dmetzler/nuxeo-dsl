"use strict";

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

// wrapping in UMD to allow code to work both in node.js (the tests/specs)
// and in the browser (css_diagrams.html)
;(function (root, factory) {
  if ((typeof module === "undefined" ? "undefined" : _typeof(module)) === "object" && module.exports) {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory(require("chevrotain"));
  } else {
    // Browser globals (root is window)\
    root["nuxeo_dsl"] = factory(root.chevrotain);
  }
})(global, function (chevrotain) {

  var createToken = chevrotain.createToken;

  var DocType = createToken({ name: "DocType", pattern: /doctype/, label: 'docType' });
  var Schema = createToken({ name: "Schema", pattern: /schema/ });
  var Facet = createToken({ name: "Facet", pattern: /facet/ });
  var Identifier = createToken({ name: "Identifier", pattern: /\w+/ });
  var Extends = createToken({ name: "Extends", pattern: /extends/ });
  var Schemas = createToken({ name: "Schemas", pattern: /schemas/ });
  var Aliases = createToken({ name: "Aliases", pattern: /aliases/ });
  var Queries = createToken({ name: "Queries", pattern: /queries/ });
  var Facets = createToken({ name: "Facets", pattern: /facets/ });
  var Lazy = createToken({ name: "Lazy", pattern: /lazy/ });
  var LCurly = createToken({ name: "LCurly", pattern: /{/ });
  var RCurly = createToken({ name: "RCurly", pattern: /}/ });
  var LParenthesis = createToken({ name: "LParenthesis", pattern: /\(/ });
  var RParenthesis = createToken({ name: "RParenthesis", pattern: /\)/ });
  var Comma = createToken({ name: "Comma", pattern: /,/ });
  var Colon = createToken({ name: "Colon", pattern: /:/ });
  var StringLiteral = createToken({
    name: "StringLiteral",
    pattern: /"(?:[^\\"]|\\(?:[bfnrtv"\\/]|u[0-9a-fA-F]{4}))*"/
  });

  var WhiteSpace = createToken({
    name: "WhiteSpace",
    pattern: /\s+/,
    group: chevrotain.Lexer.SKIPPED,
    line_breaks: true
  });

  var Comment = createToken({
    name: 'Comment',
    pattern: /\/\*[^*]*\*+([^/*][^*]*\*+)*\//,
    group: chevrotain.Lexer.SKIPPED,
    line_breaks: true
  });

  var allTokens = [WhiteSpace, Comment, Comma, StringLiteral, Colon, DocType, Schemas, Facets, Aliases, Queries, Schema, Facet, Lazy, Extends, LCurly, RCurly, LParenthesis, RParenthesis, Identifier];

  LCurly.LABEL = "'{'";
  RCurly.LABEL = "'}'";
  Comma.LABEL = "','";
  Colon.LABEL = "':'";

  var NuxeoLexer = new chevrotain.Lexer(allTokens);

  var NuxeoDSLParser = function (_chevrotain$Parser) {
    _inherits(NuxeoDSLParser, _chevrotain$Parser);

    /**
    * @param {IToken[]}
    *            input
    */
    function NuxeoDSLParser(input) {
      _classCallCheck(this, NuxeoDSLParser);

      var _this = _possibleConstructorReturn(this, (NuxeoDSLParser.__proto__ || Object.getPrototypeOf(NuxeoDSLParser)).call(this, input, allTokens, { recoveryEnabled: true, outputCst: true }));

      var $ = _this;

      $.RULE("NuxeoDSL", function () {
        $.MANY(function () {
          $.OR([{ ALT: function ALT() {
              $.SUBRULE($.doctype);
            } }, { ALT: function ALT() {
              $.SUBRULE($.schema);
            } }, { ALT: function ALT() {
              $.SUBRULE($.facet);
            } }, { ALT: function ALT() {
              $.SUBRULE($.queryList);
            } }]);
        });
      });

      $.RULE("doctype", function () {
        $.CONSUME(DocType);
        $.CONSUME1(Identifier);
        $.OPTION1(function () {
          $.CONSUME(Extends);
          $.CONSUME2(Identifier);
        });
        $.OPTION2(function () {
          $.CONSUME1(LCurly);
          $.MANY1(function () {
            $.OR([{ ALT: function ALT() {
                $.SUBRULE($.schemaList);
              } }, { ALT: function ALT() {
                $.SUBRULE($.facetList);
              } }, { ALT: function ALT() {
                $.SUBRULE($.aliasList);
              } }]);
          });
          $.CONSUME1(RCurly);
        });
      });

      $.RULE("schemaList", function () {
        $.CONSUME(Schemas);
        $.CONSUME2(LCurly);
        $.OPTION3(function () {
          $.OR([{ ALT: function ALT() {
              $.SUBRULE($.schemaRef);
            } }]);

          $.MANY2(function () {
            $.OPTION4(function () {
              $.CONSUME(Comma);
            });
            $.OR2([{ ALT: function ALT() {
                $.SUBRULE2($.schemaRef);
              } }]);
          });
        });
        $.CONSUME2(RCurly);
      });

      $.RULE("facetList", function () {
        $.CONSUME(Facets);
        $.CONSUME(LCurly);
        $.MANY(function () {
          $.CONSUME1(Identifier);
        });
        $.CONSUME(RCurly);
      });

      $.RULE("aliasList", function () {
        $.CONSUME(Aliases);
        $.CONSUME(LCurly);
        $.MANY1(function () {
          $.SUBRULE($.aliasDef);
        });
        $.CONSUME(RCurly);
      });

      $.RULE("aliasDef", function () {
        $.CONSUME(Identifier);
        $.CONSUME2(Identifier);
        $.CONSUME2(LCurly);
        $.AT_LEAST_ONE_SEP({ SEP: Comma, DEF: function DEF() {
            $.CONSUME3(StringLiteral);
          } });
        $.CONSUME2(RCurly);
      });

      $.RULE("queryList", function () {
        $.CONSUME(Queries);
        $.CONSUME(LCurly);
        $.MANY1(function () {
          $.SUBRULE($.queryDef);
        });
        $.CONSUME(RCurly);
      });

      $.RULE("queryDef", function () {
        $.CONSUME1(Identifier);
        $.OPTION(function () {
          $.CONSUME(LParenthesis);
          $.AT_LEAST_ONE_SEP({ SEP: Comma, DEF: function DEF() {
              $.CONSUME2(Identifier);
            } });
          $.CONSUME(RParenthesis);
        });
        $.CONSUME3(StringLiteral);
      });

      $.RULE("schemaRef", function () {
        $.OPTION1(function () {
          $.CONSUME1(Identifier);
          $.CONSUME(Colon);
        });
        $.CONSUME2(Identifier);
        $.OPTION2(function () {
          $.CONSUME(Lazy);
        });
        $.OPTION3(function () {
          $.SUBRULE2($.schemaBody);
        });
      });

      $.RULE("facet", function () {
        $.CONSUME(Facet);
        $.CONSUME(Identifier);
        $.OPTION2(function () {
          $.CONSUME(LCurly);
          $.SUBRULE($.schemaList);
          $.CONSUME1(RCurly);
        });
      });

      $.RULE("schema", function () {
        $.CONSUME(Schema);
        $.OPTION(function () {
          $.CONSUME1(Identifier);
          $.CONSUME(Colon);
        });
        $.CONSUME2(Identifier);
        $.SUBRULE2($.schemaBody);
      });

      $.RULE("schemaBody", function () {
        $.CONSUME(LCurly);
        $.OPTION(function () {
          $.SUBRULE($.fieldDescriptor);
          $.MANY(function () {
            $.CONSUME(Comma);
            $.SUBRULE2($.fieldDescriptor);
          });
        });
        $.CONSUME(RCurly);
      });

      $.RULE("fieldDescriptor", function () {
        $.CONSUME(Identifier);
        $.OPTION(function () {
          $.SUBRULE($.fieldType);
        });
      });

      $.RULE("fieldType", function () {
        $.CONSUME(Identifier);
      });

      chevrotain.Parser.performSelfAnalysis(_this);
      return _this;
    }

    return NuxeoDSLParser;
  }(chevrotain.Parser);
  // BaseVisitor constructors are accessed via a parser instance.


  var parser = new NuxeoDSLParser([]);

  var BaseVisitor = parser.getBaseCstVisitorConstructor();
  var BaseVisitorWithDefault = parser.getBaseCstVisitorConstructorWithDefaults();

  var NuxeoInterpreter = function (_BaseVisitorWithDefau) {
    _inherits(NuxeoInterpreter, _BaseVisitorWithDefau);

    function NuxeoInterpreter() {
      _classCallCheck(this, NuxeoInterpreter);

      var _this2 = _possibleConstructorReturn(this, (NuxeoInterpreter.__proto__ || Object.getPrototypeOf(NuxeoInterpreter)).call(this));

      _this2.validateVisitor();
      return _this2;
    }

    /* Visit methods go here */


    _createClass(NuxeoInterpreter, [{
      key: "NuxeoDSL",
      value: function NuxeoDSL(ctx) {
        var _this3 = this;

        var result = {};

        if (ctx.schema.length > 0) {
          result.schemas = ctx.schema.map(function (schema) {
            return _this3.visit(schema);
          });
        }

        if (ctx.doctype.length > 0) {
          result.doctypes = ctx.doctype.map(function (type) {
            var doctype = _this3.visit(type);

            if (doctype.schemas) {
              var schemas = _this3.extractInlineSchemas(doctype);

              schemas.forEach(function (schema) {
                result.schemas = result.schemas || [];
                result.schemas.push(schema);
              });
            }

            return doctype;
          });
        }

        if (ctx.facet.length > 0) {
          result.facets = ctx.facet.map(function (facet) {
            return _this3.visit(facet);
          });
        }

        if (ctx.queryList.length > 0) {
          result.queries = this.visit(ctx.queryList);
        }

        return result;
      }
    }, {
      key: "extractInlineSchemas",
      value: function extractInlineSchemas(doctype) {
        var inlineSchema = [];
        doctype.schemas.forEach(function (schema) {

          if (schema.hasOwnProperty("fields")) {

            if (!schema.hasOwnProperty("prefix")) {
              schema.prefix = schema.name;
            }
            inlineSchema.push(schema);

            doctype.schemas = doctype.schemas.filter(function (s) {
              return s.name !== schema.name;
            });
            doctype.schemas.push({ name: schema.name, lazy: schema.lazy });
          }
        });
        return inlineSchema;
      }
    }, {
      key: "doctype",
      value: function doctype(ctx) {
        var extendsValue = "Document";
        if (ctx.Identifier.length > 1) {
          extendsValue = ctx.Identifier[1].image;
        }

        var doctype = {
          name: ctx.Identifier[0].image,
          "extends": extendsValue
        };

        if (ctx.schemaList.length > 0) {
          doctype.schemas = this.visit(ctx.schemaList);
        }

        if (ctx.facetList.length > 0) {
          doctype.facets = this.visit(ctx.facetList);
        }

        if (ctx.aliasList.length > 0) {
          doctype.aliases = this.visit(ctx.aliasList);
        }

        return doctype;
      }
    }, {
      key: "schemaList",
      value: function schemaList(ctx) {
        var _this4 = this;

        if (ctx.schemaRef.length > 0) {
          return ctx.schemaRef.map(function (schema) {
            return _this4.visit(schema);
          });
        } else {
          return [];
        }
      }
    }, {
      key: "facetList",
      value: function facetList(ctx) {
        if (ctx.Identifier.length > 0) {
          return ctx.Identifier.map(function (id) {
            return id.image;
          });
        } else {
          return [];
        }
      }
    }, {
      key: "aliasList",
      value: function aliasList(ctx) {
        var _this5 = this;

        if (ctx.aliasDef.length > 0) {
          return ctx.aliasDef.map(function (alias) {
            return _this5.visit(alias);
          });
        }
        return [];
      }
    }, {
      key: "aliasDef",
      value: function aliasDef(ctx) {
        var alias = {};
        alias.name = ctx.Identifier[0].image;
        alias.type = ctx.Identifier[1].image;
        alias.args = ctx.StringLiteral.map(function (s) {
          return s.image.substr(0, s.image.length - 1).substr(1);
        });
        return alias;
      }
    }, {
      key: "queryList",
      value: function queryList(ctx) {
        var _this6 = this;

        if (ctx.queryDef.length > 0) {
          return ctx.queryDef.map(function (query) {
            return _this6.visit(query);
          });
        }
        return [];
      }
    }, {
      key: "queryDef",
      value: function queryDef(ctx) {
        var query = {};
        query.name = ctx.Identifier.shift().image;
        var s = ctx.StringLiteral[0].image;
        query.query = s.substr(0, s.length - 1).substr(1);
        query.params = ctx.Identifier.map(function (a) {
          return a.image;
        });
        return query;
      }
    }, {
      key: "schemaRef",
      value: function schemaRef(ctx) {

        var result = {
          lazy: ctx.Lazy.length > 0
        };

        if (ctx.Identifier.length > 1) {
          result.name = ctx.Identifier[1].image;
          result.prefix = ctx.Identifier[0].image;
        } else {
          result.name = ctx.Identifier[0].image;
        }

        if (ctx.schemaBody.length > 0) {
          result.fields = this.visit(ctx.schemaBody);
        }

        return result;
      }
    }, {
      key: "schema",
      value: function schema(ctx) {
        var name = "",
            prefix = "";
        if (ctx.Identifier.length > 1) {
          name = ctx.Identifier[1].image;
          prefix = ctx.Identifier[0].image;
        } else {
          name = ctx.Identifier[0].image;
          prefix = ctx.Identifier[0].image;
        }

        var result = {
          name: name,
          fields: this.visit(ctx.schemaBody),
          prefix: prefix
          //result.delete.lazy
        };return result;
      }
    }, {
      key: "facet",
      value: function facet(ctx) {
        var facet = {
          name: ctx.Identifier[0].image
        };

        if (ctx.schemaList.length > 0) {
          facet.schemas = this.visit(ctx.schemaList);
        }

        return facet;
      }
    }, {
      key: "schemaDescriptor",
      value: function schemaDescriptor(ctx) {
        return { name: ctx.Identifier[0].image, lazy: ctx.Lazy.length > 0 };
      }
    }, {
      key: "schemaBody",
      value: function schemaBody(ctx) {
        var _this7 = this;

        return ctx.fieldDescriptor.map(function (field) {
          return _this7.visit(field);
        }).reduce(function (map, field) {
          map[field.name] = { type: field.type };
          return map;
        }, {});
      }
    }, {
      key: "fieldDescriptor",
      value: function fieldDescriptor(ctx) {
        var fieldType = "String";
        if (ctx.fieldType.length > 0) {
          fieldType = this.visit(ctx.fieldType);
        }

        return { name: ctx.Identifier[0].image, type: fieldType };
      }
    }, {
      key: "fieldType",
      value: function fieldType(ctx) {
        return ctx.Identifier[0].image;
      }
    }]);

    return NuxeoInterpreter;
  }(BaseVisitorWithDefault);

  var NuxeoInterpreterInstance = new NuxeoInterpreter();

  return {
    parse: function parse(text) {
      var lexResult = NuxeoLexer.tokenize(text);
      // setting a new input will RESET the parser instance's state.
      parser.input = lexResult.tokens;
      // any top level rule may be used as an entry point

      var value = parser.NuxeoDSL();

      if (parser.errors.length > 0) {
        throw Error("Sad sad panda, parsing errors detected!\n" + parser.errors[0].message);
      }

      return {
        value: NuxeoInterpreterInstance.visit(value),
        lexErrors: lexResult.errors,
        parseErrors: parser.errors
      };
    },

    NuxeoDSLParser: NuxeoDSLParser,
    NuxeoLexer: NuxeoLexer,
    NuxeoInterpreter: NuxeoInterpreter

  };
});