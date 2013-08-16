package fi.vm.sade.valinta.kooste.viestintapalvelu;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.LatausUrl;

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
    @Produces("application/json")
    public Response aktivoiViestintapalvelu(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintakoeOid") String valintakoeOid) {
        try {
            return Response
                    .status(Status.OK)
                    .entity(new Gson().toJson(new LatausUrl(addressLabelBatchProxy.addressLabelBatchAktivointi(
                            hakukohdeOid, valintakoeOid)))).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
