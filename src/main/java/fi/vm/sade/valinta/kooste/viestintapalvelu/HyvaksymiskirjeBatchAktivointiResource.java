package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Ei palauta PDF-tiedostoa vaan URI:n varsinaiseen resurssiin - koska
 *         AngularJS resurssin palauttaman datan konvertoiminen selaimen
 *         ladattavaksi tiedostoksi on ongelmallista (mutta ei mahdotonta - onko
 *         tarpeen?).
 */
@Controller
@Path("hyvaksymiskirjeBatch")
public class HyvaksymiskirjeBatchAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeBatchAktivointiResource.class);

    @Autowired
    HyvaksymiskirjeBatchAktivointiProxy hyvaksymiskirjeBatchProxy;

    @POST
    @Consumes("application/json")
    @Path("aktivoi")
    @Produces("text/plain")
    public Response aktivoiViestintapalvelu(String hyvaksymiskirjeBatchJson) {
        try {
            LOG.debug("HyvaksymiskirjeBatch json {}", hyvaksymiskirjeBatchJson);
            URI contentLocation = URI.create(hyvaksymiskirjeBatchProxy
                    .hyvaksymiskirjeBatchAktivointi(hyvaksymiskirjeBatchJson));
            return Response.status(Status.ACCEPTED).contentLocation(contentLocation).entity(contentLocation.toString())
                    .build();
            /*
             * return Response.status(Response.Status.OK)
             * .header("Content-Disposition",
             * "attachment; filename*=UTF-8''hyvaksymiskirje.pdf;")
             * .entity(hyvaksymiskirjeBatchProxy
             * .hyvaksymiskirjeBatchAktivointi(hyvaksymiskirjeBatchJson
             * )).build();
             */
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
