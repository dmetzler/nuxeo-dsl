# Nuxeo DSL

[![Build Status](https://travis-ci.org/dmetzler/nuxeo-dsl.png?branch=master)](https://travis-ci.org/dmetzler/nuxeo-dsl)


This is a toy project to play with [Chevrotain](https://github.com/SAP/chevrotain) to create a Nuxeo Domain Specific language. The idea is to create an interpreter that creates Java descriptor thru Nashorn and an online editor with visual rendition of the domain model (UML style).


Document types can  be defined like this:
                
    doctype Library extends Folder {
    schemas {
        common dublincore
          lib:library { city, country}
      }    
      aliases {
        name prop {"dc:title"}
          city prop {"lib:city"}
          country prop {"lib:country"}        
          books query { "select * from Book where ecm:parentId = '${this.id}'", "Book"}
      }
      crud
    }
    
    doctype Book {
      schemas {
        common dublincore
          bk:book { author, isbn, pageCount integer}
      }
      aliases {
        title prop {"dc:title"}
          author prop {"bk:author"}
          isbn  prop {"bk:isbn"}
          pageCount prop {"bk:pageCount"}
      }
      crud
    }
    
    queries {
      libraries:Library "SELECT * From Library"
    }



Some advantages of this strategy:

  * Simpler and clearer syntax to define the model
  * With a parser, the syntax can be checked
  * As it's JS it can be used server side or developer side.
  * New features like alias that could be used by the GraphQL server
  * ...


# TODO

 * ~~Studio sync~~
 * ~~Doc Facets~~
 * ~~Doc schemas~~
 * ~~GraphQL integration~~
 * ~~GraphiQL in studio~~
 * ~~Alias in grammar~~
 * ~~Alias resolvers~~
 * ~~GraphQL schema reload on HotReload~~
 * ~~Parameterized alias and queries~~
 * ~~Mutations (basic CRUD)~~
 * React library app with Appolo
 * CodeMirror: Syntax higlighting and code completion


# Licensing

Most of the source code in the Nuxeo Platform is copyright Nuxeo and
contributors, and licensed under the Apache License, Version 2.0.

See [/licenses](/licenses) and the documentation page [Licenses](http://doc.nuxeo.com/x/gIK7) for details.

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).