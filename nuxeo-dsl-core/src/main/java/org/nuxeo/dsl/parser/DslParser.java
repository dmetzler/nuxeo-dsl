package org.nuxeo.dsl.parser;

import java.util.Collection;

public interface DslParser {

	Collection<Object> parse(String dsl);
}
