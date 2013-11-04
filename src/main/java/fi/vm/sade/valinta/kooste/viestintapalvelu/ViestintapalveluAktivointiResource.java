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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;

import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.LatausUrl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.HyvaksymiskirjeBatchAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.HyvaksyttyjenOsoitetarrojenAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.JalkiohjauskirjeBatchAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.OsoitetarratAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluMessageProxy;

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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus.virhe("Osoitetarrojen luonti epäonnistui! " + e.getMessage())).build();
        }
    }

    @Autowired
    private JalkiohjauskirjeBatchAktivointiProxy jalkiohjauskirjeBatchProxy;

    @Autowired
    private ViestintapalveluMessageProxy messageProxy;

    @GET
    @Path("jalkiohjauskirjeet/aktivoi")
    @Produces("application/json")
    public Response aktivoiJalkiohjauskirjeidenLuonti(@QueryParam("hakuOid") String hakuOid) {
        try {
            jalkiohjauskirjeBatchProxy.jalkiohjauskirjeetAktivoi(hakuOid, SecurityContextHolder.getContext()
                    .getAuthentication());
            try {
                messageProxy.message("Jälkiohjauskirjeen luonti on aloitettu.");
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Viestintäpalvelun viestirajapinta ei ole käytettävissä! {}", e.getMessage());
            }
            return Response
                    .status(Status.OK)
                    .entity(Vastaus
                            .viesti("Jälkiohjauskirjeen luonti on käynnistetty. Valmis kirje tulee näkyviin yhteisvalinnan hallinta välilehdelle."))
                    .build();

        } catch (Exception e) {
            LOG.error("Jälkiohjauskirjeiden luonnissa virhe! {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus.virhe("Jälkiohjauskirjeiden luonti epäonnistui! " + e.getMessage())).build();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus.virhe("Hyväksymiskirjeiden luonti epäonnistui! " + e.getMessage())).build();
        }
    }

    @Autowired
    private HyvaksyttyjenOsoitetarrojenAktivointiProxy hyvaksyttyjenOsoitetarratProxy;

    @GET
    @Path("hyvaksyttyjenosoitetarrat/aktivoi")
    @Produces("application/json")
    public Response aktivoiHyvaksyttyjenOsoitetarrojenLuonti(@QueryParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("hakuOid") String hakuOid, @QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
        try {
            return Response
                    .status(Status.OK)
                    .entity(new Gson().toJson(new LatausUrl(hyvaksyttyjenOsoitetarratProxy
                            .hyvaksyttyjenOsoitetarrojenAktivointi(hakukohdeOid, hakuOid, sijoitteluajoId)))).build();
        } catch (Exception e) {
            LOG.error("Hyväksyttyjen osoitetarrojen luonnissa virhe! {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Vastaus.virhe("Hyväksyttyjen osoitetarrojen luonti epäonnistui! " + e.getMessage()))
                    .build();
        }
    }

}
