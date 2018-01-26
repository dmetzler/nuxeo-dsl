package org.nuxeo.dsl.builder.v9;

import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.Builder;
import org.nuxeo.dsl.builder.XmlWriter;
import org.nuxeo.dsl.features.DocumentTypeFeature;
import org.nuxeo.dsl.features.DslFeature;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DocumentTypeBuilder implements Builder {



    @Override
    public Class<DocumentTypeFeature> getFeatureClass() {
        return DocumentTypeFeature.class;
    }

    @Override
    public void build(DslFeature f, BuildContext ctx) {

        DocumentTypeFeature feature = (DocumentTypeFeature) f;
        for (DocumentTypeDescriptor doctype : feature.getDocTypes()) {
            ctx.registerXP("org.nuxeo.ecm.core.schema.TypeService","doctype",new DoctypeXmlWriter(doctype));
        }
    }

    public static class DoctypeXmlWriter implements XmlWriter {

        private DocumentTypeDescriptor doctype;

        public DoctypeXmlWriter(DocumentTypeDescriptor doctype) {
            this.doctype = doctype;
        }

        @Override
        public void toXml(Document doc, Element contrib) {
            Element doctypeElt = doc.createElement("doctype");
            doctypeElt.setAttribute("name", doctype.name);
            doctypeElt.setAttribute("extends", doctype.superTypeName);
            contrib.appendChild(doctypeElt);

            for (SchemaDescriptor schemaDesc: doctype.schemas) {
                Element schema = doc.createElement("schema");
                schema.setAttribute("name", schemaDesc.name);
                schema.setAttribute("lazy", schemaDesc.isLazy ? "true" : "false");
                doctypeElt.appendChild(schema);
            }

        }

    }

}
