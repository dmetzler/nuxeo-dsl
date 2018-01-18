var nomnoml = nomnoml || {};

nomnoml.parse = function (source){
	function onlyCompilables(line){
		var ok = line[0] !== '#' && line[0] !== '/' && line[0] !== '*'
		return ok ? line.replace(/\/\/[^\n\r]*/mg, '') : ''
	}
	var isDirective = function (line){ return line.text[0] === '#' }
	var lines = source.split('\n').map(function (s, i){
		return {text: s.trim(), index: i }
	})
	var pureDirectives = _.filter(lines, isDirective)
	var directives = _.object(pureDirectives.map(function (line){
		try {
			var tokens =  line.text.substring(1).split(':')
			return [tokens[0].trim(), tokens[1].trim()]
		}
		catch (e) {
			throw new Error('line ' + (line.index + 1))
		}
	}))
	var pureDiagramCode = _.map(_.pluck(lines, 'text'), onlyCompilables).join('\n').trim()
	var parsed = nomnoml.intermediateParse(source)
	var ast = nomnoml.transformParseIntoSyntaxTree(parsed.value)
	ast.directives = directives
	ast.errors = parsed.errors
	ast.parserErrors = parsed.parserErrors
	ast.lexerErrors = parsed.lexerErrors

	return ast
}

nomnoml.intermediateParse = function (source){
	var lexResult = nuxeo_dsl.NuxeoLexer.tokenize(source)
	var parser = new nuxeo_dsl.NuxeoDSLParser([])
	var interpreter = new nuxeo_dsl.NuxeoInterpreter()

	
	parser.input = lexResult.tokens
	var parsed = parser.NuxeoDSL()

	
	return { 
		value:nomnoml.convertToNomnoml(interpreter.visit(parsed)),
		errors: parser.errors,
		parserErrors: parser.errors,
		lexerErrors: lexResult.errors
	};

}


nomnoml.convertToNomnoml = function(NDLObj){
	var parts = [ ], enumParts = []
	var required = function (line){ return line.key === 'required' }
	var isRequired = function (validations) {
		return _.filter(validations, required).length > 0
	}
	var setEnumRelation = function (a, part) {
		var enumPart = _.filter(enumParts, function (e){
			return e.id === a.type
		});
		if(enumPart.length > 0){
			parts.push({
				assoc: '->',
				start: part,
				end: enumPart[0],
				startLabel: '',
				endLabel: ''
			})
		}
	}
	var getCardinality = function (cardinality) {
		switch (cardinality) {
			case 'one-to-many':
				return '1-*'
			case 'one-to-one':
				return '1-1'
			case 'many-to-one':
				return '*-1'
			case 'many-to-many':
				return '*-*'
			default:
				return '1-*'
		}
	}
	var setParts = function (entity, isSchema) {
		var attrs = []
		if(isSchema){
			_.forOwn(entity.fields, function (value,name) {
				attrs.push([name, value.type].join(" "))
			})
		} else {
			_.each(entity.schemas, function (a) {
				attrs.push( a.name)				
			})
		}
		return {
			type: isSchema ? 'SCHEMA' : 'DOCUMENT',
			id: entity.name,
			parts:[
				[entity.name],
				attrs
			]
		}
	}
	
	_.each(NDLObj.doctypes, function (p){
		if (p.name){ // is a doctype
			var part = setParts(p)
			parts.push(part)

		}

		if (p.extends){
			parts.push({
			  assoc: '--:>',
			  start: setParts(p),
			  end: setParts({name:p.extends}),
			  startLabel: '',
			  endLabel: ''
			})
		}
	})


	_.each(NDLObj.schemas, function (p){
		if (p.name){ // is a doctype
			var part = setParts(p, true)
			parts.push(part)

		}
		
	})

	_.each(NDLObj.relationships, function (p){
		parts.push({
			assoc: '->',
			start: setParts(p.from),
			end: setParts(p.to),
			startLabel: p.from.injectedfield ? p.from.injectedfield : '',
			endLabel: (getCardinality(p.cardinality) + ' ' + (p.to.injectedfield ? p.to.injectedfield : ''))
		})
	})

	return parts;
}


nomnoml.transformParseIntoSyntaxTree = function (entity){

	var relationId = 0

	function transformCompartment(parts){		
		var lines = []
		var rawClassifiers = []
		var relations = []
		_.each(parts, function (p){
			
			if (typeof p === 'string')
				lines.push(p)
			if (p.assoc){ // is a relation
				rawClassifiers.push(p.start)
				rawClassifiers.push(p.end)
				relations.push({
                    id: relationId++,
                    assoc: p.assoc,
                    start: p.start.parts[0][0],
                    end: p.end.parts[0][0],
                    startLabel: p.startLabel,
                    endLabel: p.endLabel
                })
            }
			if (p.parts){ // is a classifier
				rawClassifiers.push(p)
            }
        })
		var allClassifiers = _.map(rawClassifiers, transformItem)
		var noDuplicates = _.map(_.groupBy(allClassifiers, 'name'), function (cList){
			return _.max(cList, function (c){ return c.compartments.length })
		})

		return nomnoml.Compartment(lines, noDuplicates, relations)
	}

	function transformItem(entity){
		if (typeof entity === 'string')
			return entity
		if (_.isArray(entity))
			return transformCompartment(entity)
		if (entity.parts){
			var compartments = _.map(entity.parts, transformCompartment)
			return nomnoml.Classifier(entity.type, entity.id, compartments)
		}
		return undefined
	}

	return transformItem(entity)
}
