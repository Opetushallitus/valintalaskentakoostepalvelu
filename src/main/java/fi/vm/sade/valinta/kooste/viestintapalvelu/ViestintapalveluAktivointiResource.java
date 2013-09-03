package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.net.URI;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.LatausUrl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.HyvaksymiskirjeBatchAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.JalkiohjauskirjeBatchAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.OsoitetarratAktivointiProxy;

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
@Path("viestintapalvelu")
public class ViestintapalveluAktivointiResource {

    private static final Logger LOG = LoggerFactory.getLogger(ViestintapalveluAktivointiResource.class);

    @Autowired
    private OsoitetarratAktivointiProxy addressLabelBatchProxy;

    @GET
    @Path("osoitetarrat/aktivoi")
    @Produces("application/json")
    public Response aktivoiOsoitetarrojenLuonti(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintakoeOid") List<String> valintakoeOids) {
        try {
            return Response
                    .status(Status.OK)
                    .entity(new Gson().toJson(new LatausUrl(addressLabelBatchProxy.osoitetarratAktivointi(hakukohdeOid,
                            valintakoeOids)))).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Autowired
    private JalkiohjauskirjeBatchAktivointiProxy jalkiohjauskirjeBatchProxy;

    @GET
    @Path("jalkiohjauskirjeet/aktivoi")
    @Produces("application/json")
    public Response aktivoiJalkiohjauskirjeidenLuonti(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintakoeOid") List<String> valintakoeOids) {
        try {
            URI contentLocation = URI.create(jalkiohjauskirjeBatchProxy.jalkiohjauskirjeetAktivoi(hakukohdeOid,
                    valintakoeOids));
            return Response.status(Status.ACCEPTED).contentLocation(contentLocation).entity(contentLocation.toString())
                    .build();
            /*
             * return Response.status(Response.Status.OK)
             * .header("Content-Disposition",
             * "attachment; filename*=UTF-8''jalkiohjauskirje.pdf;")
             * .entity(jalkiohjauskirjeBatchProxy
             * .jalkiohjauskirjeBatchAktivoi(jalkiohjauskirjeBatchJson
             * )).build();
             */
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @Autowired
    private HyvaksymiskirjeBatchAktivointiProxy hyvaksymiskirjeBatchProxy;

    @GET
    @Path("hyvaksymiskirjeet/aktivoi")
    @Produces("application/json")
    public Response aktivoiHyvaksymiskirjeidenLuonti(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintakoeOid") List<String> valintakoeOids) {
        try {
            URI contentLocation = URI.create(hyvaksymiskirjeBatchProxy.hyvaksymiskirjeetAktivointi(hakukohdeOid,
                    valintakoeOids));
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
