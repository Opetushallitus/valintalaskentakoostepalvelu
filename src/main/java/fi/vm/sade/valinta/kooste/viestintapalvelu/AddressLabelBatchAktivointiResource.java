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
@Path("addressLabelBatch")
public class AddressLabelBatchAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(AddressLabelBatchAktivointiResource.class);

    @Autowired
    AddressLabelBatchAktivointiProxy addressLabelBatchProxy;

    @POST
    @Consumes("application/json")
    @Path("aktivoi")
    @Produces("text/plain")
    public Response aktivoiViestintapalvelu(String addressLabelBatchJson) {
        try {
            LOG.debug("AddressLabelBatch json {}", addressLabelBatchJson);
            URI contentLocation = URI.create(addressLabelBatchProxy.addressLabelBatchAktivointi(addressLabelBatchJson));
            return Response.status(Status.ACCEPTED).contentLocation(contentLocation).entity(contentLocation.toString())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
