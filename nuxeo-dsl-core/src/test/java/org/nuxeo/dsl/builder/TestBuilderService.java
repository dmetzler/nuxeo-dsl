package org.nuxeo.dsl.builder;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.dsl.nuxeo-dsl-core")
public class TestBuilderService {

    @Inject
    protected BuilderService builderservice;

    @Test
    public void testService() {
        assertNotNull(builderservice);
    }
}
