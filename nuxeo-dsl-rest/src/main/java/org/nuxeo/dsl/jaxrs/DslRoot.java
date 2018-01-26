package org.nuxeo.dsl.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dsl.deployer.DslDeployer;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;


@Path("/dsl/")
@Produces("text/plain;charset=UTF-8")
@WebObject(type = "DslRoot")
public class DslRoot {

    private static Log log = LogFactory.getLog(DslRoot.class);
    @PUT
    public Object doUploadDsl(String dsl) {
        try {
            log.error("Getting dsl " + dsl);
            getDeployer().deployDsl(dsl);
            return Response.ok(doGetDsl()).build();
        } catch (NuxeoException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error during build : " + e.getMessage())
                           .build();
        }
    }

    @GET
    public String doGetDsl() {
        return getDeployer().getDsl();
    }

    private DslDeployer getDeployer() {
        return Framework.getService(DslDeployer.class);
    }

}
