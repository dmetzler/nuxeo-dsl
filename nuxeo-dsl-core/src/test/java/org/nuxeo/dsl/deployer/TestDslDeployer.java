package org.nuxeo.dsl.deployer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.dsl.nuxeo-dsl-core")
@LocalDeploy("org.nuxeo.dsl.nuxeo-dsl-core.test:test-dsl-contrib.xml")
public class TestDslDeployer {

    @Inject
    protected DslDeployer dsldeployer;

    @Test
    public void testService() {
        assertNotNull(dsldeployer);
    }

    @Test
    public void it_can_deploy_a_dsl() throws Exception {
        assertThat(dsldeployer.getDsl().trim()).isEqualTo("doctype NewType {}");
    }
}
