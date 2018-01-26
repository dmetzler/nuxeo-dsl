package org.nuxeo.dsl.builder.v9;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.Builder;
import org.nuxeo.dsl.features.DslFeature;
import org.nuxeo.dsl.features.SchemaFeature;
import org.nuxeo.dsl.features.SchemaFeature.FieldsDef;
import org.nuxeo.ecm.core.schema.SchemaBindingDescriptor;

public class SchemaBuilder implements Builder {

    @Override
    public void build(DslFeature f, BuildContext ctx) {
        ctx.registerXMap(SchemaBindingDescriptor.class);

        SchemaFeature feature = (SchemaFeature) f;

        if (!feature.getShemaBindings().isEmpty()) {

            for (SchemaBindingDescriptor desc : feature.getShemaBindings()) {
                desc.src = "schemas/" + desc.name + ".xsd";
                ctx.registerXP("org.nuxeo.ecm.core.schema.TypeService", "schema", desc);
            }

            File schemaDir = new File(ctx.getBuildDir(), "schemas");
            schemaDir.mkdir();

            for (Entry<String, FieldsDef> entry : feature.getFieldDefs().entrySet()) {
                buildXsdForShema(ctx, schemaDir, entry.getKey(), entry.getValue());
            }
        }

    }

    private void buildXsdForShema(BuildContext ctx, File schemaDir, String schemaName, FieldsDef fields) {

        try {
            Document doc = loadXsdTemplate();
            String schemaUri = "http://www.nuxeo.org/ecm/project/schemas/" + ctx.getProjectId() + "/" + schemaName;
            Element root = doc.getRootElement();
            // local namespace
            root.addAttribute("targetNamespace", schemaUri);
            root.addNamespace("nxs", schemaUri);

            for (SchemaFeature.Field field : fields.getFields()) {
                Element elem = root.addElement(QName.get("xs:element"));
                String name = field.getName();
                elem.addAttribute("name", name);
                String type = field.getType();
                elem.addAttribute("type", toXSDType(type));
            }

            FileWriter fw = new FileWriter(new File(schemaDir, schemaName + ".xsd"));
            try {
                XMLWriter writer = new XMLWriter(fw, OutputFormat.createPrettyPrint());
                writer.write(doc);
            } finally {
                fw.close();
            }

        } catch (DocumentException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String toXSDType(String type) {
        if ("String".equalsIgnoreCase(type)) {
            return "xs:string";
        } else if ("Integer".equalsIgnoreCase(type)) {
            return "xs:integer";
        } else if ("Double".equalsIgnoreCase(type)) {
            return "xs:double";
        } else if ("Date".equalsIgnoreCase(type)) {
            return "xs:date";
        } else if ("Boolean".equalsIgnoreCase(type)) {
            return "xs:boolean";
        } else {
            throw new IllegalArgumentException("Unknown type : " + type);
        }
    }

    private Document loadXsdTemplate() throws DocumentException, IOException {
        InputStream in = getClass().getResourceAsStream("/templates/schema-template.xsd");
        try {
            return new SAXReader().read(in);
        } finally {
            in.close();
        }
    }

    @Override
    public Class<? extends DslFeature> getFeatureClass() {
        return SchemaFeature.class;
    }

}
