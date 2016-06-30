package fi.vm.sade.valinta.kooste.sijoitteluntulos.resource;

import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.sijoitteluntulos.service.HyvaksymiskirjeetHaulleHakukohteittain;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.service.HyvaksymiskirjeetKokoHaulleService;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosHyvaksymiskirjeetRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * @Autowired(required = false) Camel-reitit valinnaisiksi poisrefaktorointia odotellessa.
 */
@Controller("SijoittelunTulosHaulleResource")
@Path("sijoitteluntuloshaulle")
@PreAuthorize("isAuthenticated()")
@Api(value = "/sijoitteluntuloshaulle", description = "Sijoitteluntulosten generointi koko haulle")
public class SijoittelunTulosHaulleResource {
    private static final Logger LOG = LoggerFactory.getLogger(SijoittelunTulosHaulleResource.class);

    @Autowired
    private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
    @Autowired(required = false)
    private SijoittelunTulosTaulukkolaskentaRoute sijoittelunTulosTaulukkolaskentaRoute;
    @Autowired(required = false)
    private SijoittelunTulosOsoitetarratRoute sijoittelunTulosOsoitetarratRoute;
    @Autowired
    private HyvaksymiskirjeetKokoHaulleService hyvaksymiskirjeetKokoHaulleService;
    @Autowired
    private HyvaksymiskirjeetHaulleHakukohteittain hyvaksymiskirjeetHakukohteittain;

    @POST
    @Path("/osoitetarrat")
    @Consumes("application/json")
    @Produces("application/json")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
    public ProsessiId osoitetarratKokoHaulle(@QueryParam("hakuOid") String hakuOid) {
        try {
            SijoittelunTulosProsessi prosessi = new SijoittelunTulosProsessi(
                    null, // ei asiointikielirajausta
                    "osoitetarrat", "Luo osoitetarrat haulle", null, Arrays.asList("osoitetarrat", "haulle"));
            sijoittelunTulosOsoitetarratRoute.osoitetarratHaulle(prosessi, hakuOid, SijoitteluResource.LATEST, SecurityContextHolder.getContext().getAuthentication());
            dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
            return prosessi.toProsessiId();
        } catch (Exception e) {
            LOG.error("Osoitetarrojen luonnissa virhe!", e);
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            throw new RuntimeException("Osoitetarrojen luonnissa virhe!", e);
        }
    }

    @POST
    @Path("/hyvaksymiskirjeet")
    @Consumes("application/json")
    @Produces("application/json")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
    public ProsessiId hyvaksymiskirjeetKokoHaulle(@QueryParam("hakuOid") String hakuOid,
                                                  @QueryParam("letterBodyText") String letterBodyText,
                                                  @QueryParam("asiointikieli") String asiointikieli) {
        try {
            SijoittelunTulosProsessi prosessi = new SijoittelunTulosProsessi(
                    Optional.ofNullable(asiointikieli).map(KieliUtil::normalisoiKielikoodi),
                    "hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", null, Arrays.asList("hyvaksymiskirjeet", "haulle"));

            if(asiointikieli != null ) {
                hyvaksymiskirjeetKokoHaulleService.muodostaHyvaksymiskirjeetKokoHaulle(hakuOid, asiointikieli, prosessi, Optional.ofNullable(letterBodyText));
            } else {
                hyvaksymiskirjeetHakukohteittain.muodostaKirjeet(hakuOid, prosessi, Optional.ofNullable(letterBodyText));
            }
            dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
            return prosessi.toProsessiId();
        } catch (Exception e) {
            LOG.error("Hyväksymiskirjeiden luonnissa virhe!", e);
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            throw new RuntimeException("Hyväksymiskirjeiden luonnissa virhe!", e);
        }
    }

    @POST
    @Path("/taulukkolaskennat")
    @Consumes("application/json")
    @Produces("application/json")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
    @ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
    public ProsessiId taulukkolaskennatKokoHaulle(@QueryParam("hakuOid") String hakuOid) {
        try {
            SijoittelunTulosProsessi prosessi = new SijoittelunTulosProsessi(
                    null, // ei asiointikielirajausta
                    "taulukkolaskennat", "Luo taulukkolaskennat haulle", null, Arrays.asList("taulukkolaskennat", "haulle"));
            sijoittelunTulosTaulukkolaskentaRoute.taulukkolaskennatHaulle(prosessi, hakuOid, SijoitteluResource.LATEST, SecurityContextHolder.getContext().getAuthentication());
            dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
            return prosessi.toProsessiId();
        } catch (Exception e) {
            LOG.error("Taulukkolaskentojen luonnissa virhe!", e);
            // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
            // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
            // Ylläpitäjä voi lukea logeista todellisen syyn!
            throw new RuntimeException("Taulukkolaskentojen luonnissa virhe!", e);
        }
    }
}
