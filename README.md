# Nuxeo DSL

[![Build Status](https://travis-ci.org/dmetzler/nuxeo-dsl.png?branch=master)](https://travis-ci.org/dmetzler/nuxeo-dsl)


This is a toy project playing with [Chevrotain](https://github.com/SAP/chevrotain) to create a Nuxeo Domain Specific language. The idea is to create an interpreter that create Java descriptor thru Nashorm and an online editor with visual rendition of the domain model (UML style).


A document could be defined like this:
                
        document Department extends Document {
           schemas {
              common
              resource
              dublincore
              //Inline declaration of a schema
              department with prefix dpt { id long, name }
           }
           
           alias {
              // Aliases are a way to provide an indirection to
              // the physical property or to a query.
              created_at prop dc:created
              modified_at prop dc:modified
              allEmployee query "SELECT * FROM Employee WHERE emp:deptId = '$this.id'"
           }              
        }


Some advantages of this strategy:

  * Simpler and clearer syntax to define the model
  * With a parser, the syntax can be checked
  * As it's JS it can be used server side or developer side.
  * Can add new featrures like alias that could be useful for a GraphQL server
  * ...


# Licensing

Most of the source code in the Nuxeo Platform is copyright Nuxeo and
contributors, and licensed under the Apache License, Version 2.0.

See [/licenses](/licenses) and the documentation page [Licenses](http://doc.nuxeo.com/x/gIK7) for details.

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).