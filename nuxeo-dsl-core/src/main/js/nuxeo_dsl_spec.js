"use strict"
const expect = require("chai").expect
const parse = require("./nuxeo_dsl").parse

describe("Nuxeo DSL", () => {
    context("Document type", () => {
        it("Can be defined", () => {
            let inputText =
                "doctype myDoc {}" +
                "\r\ndoctype otherDoc"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                  {name:"myDoc",extends:"Document"},
                  {name:"otherDoc",extends:"Document"}
                ]
            })
        })

        it("Can inherit from a parent type", () => {
            let inputText =
                "doctype myDoc extends File {}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                  {name:"myDoc", extends: "File"}
                ]
            })
        })

        it("Can set the schemas", () => {
            let inputText =
                "doctype myDoc extends File {" +
                "\r\n   schemas {" +
                "\r\n      common" +
                "\r\n      dublincore lazy" +
                "\r\n   }" +
                "\r\n}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                  {
                        name:"myDoc",
                        extends: "File",
                        schemas: [
                              {name: "common", lazy: false},
                              {name: "dublincore", lazy: true}
                        ]
                  }
                ]
            })
        })


        it("Can set the facets", () => {
            let inputText =
                "doctype myDoc extends File {" +
                "\r\n   facets {" +
                "\r\n      Folderish" +                
                "\r\n   }" +
                "\r\n}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                  {
                        name:"myDoc",
                        extends: "File",
                        facets: [
                          "Folderish"
                        ]
                  }
                ]
            })
        })


        it("Can ask for CRUD mutations", () => {
            let inputText =
                "doctype myDoc  {" +
                "\r\n   crud" +
                "\r\n}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                  {
                        name:"myDoc",
                        extends: "Document",
                        crud: {}
                  }
                ]
            })
        })
    })

    context("Schemas definition", () => {
      it("Can be defined", () => {
            let inputText =
                "schema mySchema {prop1 String, prop2 , prop3 Integer }"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                schemas: [
                  {
                        name:"mySchema",
                        prefix:"mySchema",
                        fields:{
                              prop1: { type: "String"},
                              prop2: { type: "String"},
                              prop3: { type: "Integer"}
                        }
                  }
                ]
            })
        })

      it("Can have a prefix", () => {
          let inputText =
              "schema my:mySchema {prop1 String, prop2 , prop3 Integer }"
          let result = parse(inputText)

          expect(result.value).to.deep.equal({
              schemas: [
                {
                      name:"mySchema",
                      prefix:"my",
                      fields:{
                            prop1: { type: "String"},
                            prop2: { type: "String"},
                            prop3: { type: "Integer"}
                      }
                }
              ]
          })
      })

      it("Can be inlined", () => {
            let inputText =
                "doctype myDoc {" +
                "\r\n   schemas {" +
                "\r\n      custom {prop1 String, prop2 , prop3 Integer }" +
                "\r\n      my:scheme {prop1 String, prop2 , prop3 Integer }" +
                "\r\n   }" +
                "\r\n}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                  {
                        name:"myDoc",
                        extends: "Document",
                        schemas: [                              
                              {
                                name: "custom", 
                                lazy: false                                
                            },
                            {
                                name: "scheme", 
                                lazy: false                                
                            }
                        ]

                  }
                ],
                schemas: [
                  {
                        name:"custom",
                        lazy: false,
                        prefix: "custom",
                        fields:{
                              prop1: { type: "String"},
                              prop2: { type: "String"},
                              prop3: { type: "Integer"}
                        }
                  },
                  {
                        name:"scheme",
                        lazy: false,
                        prefix: "my",
                        fields:{
                              prop1: { type: "String"},
                              prop2: { type: "String"},
                              prop3: { type: "Integer"}
                        }
                  }
                ]
            })
        })


    })

    context("Facets definition", () => {
        it("Can be defined", () => {
            let inputText =
                "facet myFacet { schemas { dublincore, picture }}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                facets: [
                    {
                        name:"myFacet",
                        schemas:[
                              {name: "dublincore", lazy: false},
                              {name: "picture", lazy: false},
                        ]
                  }
                ]
            })
        })

    })

    context("Alias definitions", () => {
      it("Can be defined", () => {
            let inputText =
                "doctype myDoc { aliases { title prop { \"dc:title\" } description prop { \"dc:description\" }}}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                doctypes: [
                    {
                        name:"myDoc",
                        extends:"Document",
                        aliases:[
                              {name: "title", type: "prop", args: ["dc:title"] },
                              {name: "description", type: "prop", args: ["dc:description"] }
                        ]
                  }
                ]
            })
        })
    })

    context("Query definitions", () => {
      it("Can be defined", () => {
            let inputText =
                "queries { libraries:Library \"SELECT * FROM Library\" library(name) \"SELECT * FROM Library WHERE dc:title= '$name'\" librari(name):Library \"SELECT * FROM Library WHERE dc:title= '$name'\"}"
            let result = parse(inputText)

            expect(result.value).to.deep.equal({
                queries:[
                  {name: "libraries", params:[], query: "SELECT * FROM Library", resultType: "Library" },
                  {name: "library", params:["name"], query: "SELECT * FROM Library WHERE dc:title= '$name'", resultType: "document" },
                  {name: "librari", params:["name"], query: "SELECT * FROM Library WHERE dc:title= '$name'", resultType: "Library" }
                ]
                  
            })
        })
    })



})