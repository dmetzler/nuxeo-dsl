package org.nuxeo.dsl.parser;

import java.util.Collection;

import org.nuxeo.dsl.DslModel;

public interface DslParser {

	DslModel parse(String dsl);
}
