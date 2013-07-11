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
@Path("hyvaksymiskirjeBatch")
public class HyvaksymiskirjeBatchAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeBatchAktivointiResource.class);

    @Autowired
    HyvaksymiskirjeBatchAktivointiProxy hyvaksymiskirjeBatchProxy;

    @GET
    @Consumes("application/json")
    @Path("aktivoi")
    public Response aktivoiViestintapalvelu(String hyvaksymiskirjeBatchJson) {
        try {
            LOG.debug("HyvaksymiskirjeBatch json {}", hyvaksymiskirjeBatchJson);
            return Response.status(Response.Status.OK)
                    .header("Content-Disposition", "attachment; filename*=UTF-8''hyvaksymiskirje.pdf;")
                    .entity(hyvaksymiskirjeBatchProxy.hyvaksymiskirjeBatchAktivointi(hyvaksymiskirjeBatchJson)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
