package org.nuxeo.dsl.deployer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.dsl.DslModel;
import org.nuxeo.dsl.builder.BuildContext;
import org.nuxeo.dsl.builder.BuilderService;
import org.nuxeo.dsl.parser.DslParser;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.nuxeo.runtime.reload.ReloadContext;
import org.nuxeo.runtime.reload.ReloadService;
import org.osgi.framework.BundleException;

public class DslDeployerImpl extends DefaultComponent implements DslDeployer {

    private static final String XP_DSL = "dsl";
    private DslConfigRegistry registry = new DslConfigRegistry();

    /**
     * Component activated notification. Called when the component is activated. All component dependencies are resolved
     * at that moment. Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification. Called before a component is unregistered. Use this method to do cleanup if
     * any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification. Called after the application started. You can do here any initialization that
     * requires a working application (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_DSL.equals(extensionPoint)) {
            DslSourceDescriptor desc = (DslSourceDescriptor) contribution;
            registry.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_DSL.equals(extensionPoint)) {
            DslSourceDescriptor desc = (DslSourceDescriptor) contribution;
            registry.removeContribution(desc);
        }
    }

    @Override
    public void deployDsl(String dsl) {
        DslParser parser = Framework.getService(DslParser.class);
        DslModel model = parser.parse(dsl);

        BuilderService bs = Framework.getService(BuilderService.class);
        try(BuildContext ctx = BuildContext.newContext("dsl-studio")) {
            bs.compile(model, ctx);
            try {

                File bundle = ctx.buildJar(FileUtils.getTempDirectory());

                ReloadService rs = Framework.getService(ReloadService.class);
                ReloadContext rctx = new ReloadContext();
                rctx.undeploy("dslstudio.extensions."+ctx.getProjectId());
                rctx.deploy(bundle);
                rs.reloadBundles(rctx);
                bundle.delete();
            } catch (IOException | BundleException e) {
                throw new NuxeoException("Unable to build studio jar", e);
            }
        }
    }

    @Override
    public String getDsl() {
        if (registry.getCurrentContribution("default") != null) {
            return registry.getCurrentContribution("default").getSrc();
        } else {
            return "";
        }
    }

    protected static class DslConfigRegistry extends SimpleContributionRegistry<DslSourceDescriptor> {

        @Override
        public String getContributionId(DslSourceDescriptor contrib) {
            return contrib.getName();
        }

        @Override
        public DslSourceDescriptor getCurrentContribution(String id) {
            return super.getCurrentContribution(id);
        }

    }

}
