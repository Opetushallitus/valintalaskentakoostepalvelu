package fi.vm.sade.valinta.kooste.viestintapalvelu;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller
@Path("jalkiohjauskirjeBatch")
public class JalkiohjauskirjeBatchAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeBatchAktivointiResource.class);

    @Autowired
    JalkiohjauskirjeBatchAktivointiProxy jalkiohjauskirjeBatchProxy;

    @GET
    @Consumes("application/json")
    @Path("aktivoi")
    public Response aktivoiViestintapalvelu(String jalkiohjauskirjeBatchJson) {
        try {
            LOG.debug("JalkiohjauskirjeBatch json {}", jalkiohjauskirjeBatchJson);

            return Response.status(Response.Status.OK)
                    .header("Content-Disposition", "attachment; filename*=UTF-8''jalkiohjauskirje.pdf;")
                    .entity(jalkiohjauskirjeBatchProxy.jalkiohjauskirjeBatchAktivoi(jalkiohjauskirjeBatchJson)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
