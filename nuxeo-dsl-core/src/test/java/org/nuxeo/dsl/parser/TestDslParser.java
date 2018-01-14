package org.nuxeo.dsl.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
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
	ReloadService reload;

	@Test
	public void it_can_retrieve_the_service() {
		assertNotNull(dslparser);
	}

	@Test
	public void it_can_parse_a_dsl() throws Exception {


		Collection<Object> descriptors = dslparser.parse("doctype NewType {}");
		List<DocumentTypeDescriptor> docctypes = (List<DocumentTypeDescriptor>) descriptors.iterator().next();


		assertEquals("NewType", docctypes.get(0).name);



	}
}
