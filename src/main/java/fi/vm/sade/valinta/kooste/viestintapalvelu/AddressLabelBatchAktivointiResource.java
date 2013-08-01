package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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

    @Autowired
    AddressLabelBatchAktivointiProxy addressLabelBatchProxy;

    @POST
    @Path("aktivoi")
    @Produces("text/plain")
    public Response aktivoiViestintapalvelu(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        try {
            URI contentLocation = URI.create(addressLabelBatchProxy.addressLabelBatchAktivointi(hakukohdeOid));
            return Response.status(Status.ACCEPTED).contentLocation(contentLocation).entity(contentLocation.toString())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
