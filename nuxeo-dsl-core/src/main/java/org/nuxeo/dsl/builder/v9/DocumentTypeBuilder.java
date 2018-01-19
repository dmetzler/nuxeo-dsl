package org.nuxeo.dsl.builder.v9;

import java.io.IOException;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.Builder;
import org.nuxeo.dsl.features.DocumentTypeFeature;
import org.nuxeo.dsl.features.DslFeature;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;

public class DocumentTypeBuilder implements Builder {



    @Override
    public Class<DocumentTypeFeature> getFeatureClass() {
        return DocumentTypeFeature.class;
    }

    @Override
    public void build(DslFeature f, BuildContext ctx) {
        ctx.registerXMap(DocumentTypeDescriptor.class);

        DocumentTypeFeature feature = (DocumentTypeFeature) f;
        for (DocumentTypeDescriptor doctype : feature.getDocTypes()) {
            ctx.registerXP("org.nuxeo.ecm.core.schema.TypeService","doctype",doctype);
        }
    }

}
