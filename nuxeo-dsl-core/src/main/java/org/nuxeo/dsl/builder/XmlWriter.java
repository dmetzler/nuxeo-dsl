package org.nuxeo.dsl.builder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface XmlWriter {

    void toXml(Document doc, Element contrib);
}
