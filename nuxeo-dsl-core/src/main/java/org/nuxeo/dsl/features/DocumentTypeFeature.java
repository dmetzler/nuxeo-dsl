package org.nuxeo.dsl.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;

public class DocumentTypeFeature implements DslFeature {

    private List<DocumentTypeDescriptor> doctypes = new ArrayList<>();

    @Override
    public void visit(Map<String, Object> ast) {
        doctypes.addAll((List<DocumentTypeDescriptor>) ast.get("doctypes"));
    }

    public List<DocumentTypeDescriptor> getDocTypes() {
        return doctypes;
    }

}
