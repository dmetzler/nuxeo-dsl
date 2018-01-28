package org.nuxeo.dsl.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.BuilderService;
import org.nuxeo.dsl.features.DocumentTypeFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaDescriptor;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.dsl.nuxeo-dsl-core")
public class TestDslParser {

    @Inject
    protected DslParser dslparser;

    @Inject
    protected CoreSession session;

    @Inject
    protected BuilderService bs;

    @Inject
    ReloadService reload;

    @Test
    public void it_can_retrieve_the_service() {
        assertNotNull(dslparser);
    }


    @Test
    public void testName() throws Exception {
        DocumentTypeDescriptor dtd = new DocumentTypeDescriptor();

        dtd.schemas = new SchemaDescriptor[]{new SchemaDescriptor("common"),new SchemaDescriptor("dublincore")};

        XMap xmap = new XMap();
        xmap.register(DocumentTypeDescriptor.class);
        xmap.register(SchemaDescriptor.class);
        System.out.println(xmap.toXML(dtd));


    }

    @Test
    public void it_can_parse_a_dsl() throws Exception {

        DslModel model = dslparser.parse("doctype NewType { schemas { common dublincore } facets {Folderish}}");
        DocumentTypeFeature feature = model.getFeature(DocumentTypeFeature.class);

        assertThat(feature.getDocTypes()).hasSize(1);

        DocumentTypeDescriptor descriptor = feature.getDocTypes().get(0);
        assertThat(descriptor.name).isEqualTo("NewType");
        assertThat(descriptor.superTypeName).isEqualTo("Document");
        XMap xmap = new XMap();
        xmap.register(DocumentTypeDescriptor.class);
        System.out.println(xmap.toXML(descriptor));

    }

    @Test
    public void it_can_build_a_model() throws Exception {

        InputStream is = getClass().getResourceAsStream("/sample-dsl.txt");
        String src = IOUtils.toString(is, Charset.defaultCharset());



        DslModel model = dslparser.parse(src);


        try (BuildContext ctx = BuildContext.newContext("dsl")) {
            bs.compile(model, ctx);
            ctx.buildJar(new File("/tmp"));
        }
    }
}
