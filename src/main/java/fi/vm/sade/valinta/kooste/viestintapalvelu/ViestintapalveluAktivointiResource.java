package fi.vm.sade.valinta.kooste.viestintapalvelu;

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

import fi.vm.sade.valinta.kooste.dto.Vastaus;
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
            LOG.error("Osoitetarrojen luonnissa virhe! {}", e.getMessage());
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus
                            .virhe("Osoitetarrojen luonti epäonnistui! Hakemuspalvelu saattaa olla ylikuormittunut, yritä uudelleen!"))
                    .build();
        }
    }

    @Autowired
    private JalkiohjauskirjeBatchAktivointiProxy jalkiohjauskirjeBatchProxy;

    @GET
    @Path("jalkiohjauskirjeet/aktivoi")
    @Produces("application/json")
    public Response aktivoiJalkiohjauskirjeidenLuonti(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("hakuOid") String hakuOid, @QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
        try {
            return Response
                    .status(Status.OK)
                    .entity(new Gson().toJson(new LatausUrl(jalkiohjauskirjeBatchProxy.jalkiohjauskirjeetAktivoi(
                            hakukohdeOid, hakuOid, sijoitteluajoId)))).build();
        } catch (Exception e) {
            LOG.error("Jälkiohjauskirjeiden luonnissa virhe! {}", e.getMessage());
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus
                            .virhe("Jälkiohjauskirjeiden luonti epäonnistui! Hakemuspalvelu saattaa olla ylikuormittunut, yritä uudelleen!"))
                    .build();
        }
    }

    @Autowired
    private HyvaksymiskirjeBatchAktivointiProxy hyvaksymiskirjeBatchProxy;

    @GET
    @Path("hyvaksymiskirjeet/aktivoi")
    @Produces("application/json")
    public Response aktivoiHyvaksymiskirjeidenLuonti(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("hakuOid") String hakuOid, @QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
        try {
            return Response
                    .status(Status.OK)
                    .entity(new Gson().toJson(new LatausUrl(hyvaksymiskirjeBatchProxy.hyvaksymiskirjeetAktivointi(
                            hakukohdeOid, hakuOid, sijoitteluajoId)))).build();
        } catch (Exception e) {
            LOG.error("Hyväksymiskirjeiden luonnissa virhe! {}", e.getMessage());
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus
                            .virhe("Hyväksymiskirjeiden luonti epäonnistui! Hakemuspalvelu saattaa olla ylikuormittunut, yritä uudelleen!"))
                    .build();
        }
    }

}
