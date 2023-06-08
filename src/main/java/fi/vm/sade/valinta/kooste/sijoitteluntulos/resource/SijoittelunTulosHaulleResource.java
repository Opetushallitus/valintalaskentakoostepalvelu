package fi.vm.sade.valinta.kooste.sijoitteluntulos.resource;

import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.KirjeenVastaanottaja;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/** @Autowired(required = false) Camel-reitit valinnaisiksi poisrefaktorointia odotellessa. */
@Controller("SijoittelunTulosHaulleResource")
@Path("sijoitteluntuloshaulle")
@PreAuthorize("isAuthenticated()")
@Api(value = "/sijoitteluntuloshaulle", description = "Sijoitteluntulosten generointi koko haulle")
public class SijoittelunTulosHaulleResource {
  private static final Logger LOG = LoggerFactory.getLogger(SijoittelunTulosHaulleResource.class);

  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

  @Autowired(required = false)
  private SijoittelunTulosTaulukkolaskentaRoute sijoittelunTulosTaulukkolaskentaRoute;

  @Autowired(required = false)
  private SijoittelunTulosOsoitetarratRoute sijoittelunTulosOsoitetarratRoute;

  @Autowired private HyvaksymiskirjeetService hyvaksymiskirjeetService;

  @Context private HttpServletRequest httpServletRequestJaxRS;

  @POST
  @Path("/osoitetarrat")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille",
      response = Response.class)
  public ProsessiId osoitetarratKokoHaulle(@QueryParam("hakuOid") String hakuOid) {
    try {
      SijoittelunTulosProsessi prosessi =
          new SijoittelunTulosProsessi(
              null, // ei asiointikielirajausta
              "osoitetarrat",
              "Luo osoitetarrat haulle",
              hakuOid,
              Arrays.asList("osoitetarrat", "haulle"));
      sijoittelunTulosOsoitetarratRoute.osoitetarratHaulle(
          prosessi, hakuOid, "latest", SecurityContextHolder.getContext().getAuthentication());
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
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille",
      response = Response.class)
  public ProsessiId hyvaksymiskirjeetKokoHaulle(
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("letterBodyText") String letterBodyText,
      @QueryParam("asiointikieli") String asiointikieli) {
    try {
      HyvaksymiskirjeDTO hyvaksymiskirjeDTO =
          new HyvaksymiskirjeDTO(
              null,
              letterBodyText,
              "hyvaksymiskirje",
              hakuOid,
              null,
              hakuOid,
              null,
              null,
              null,
              false);
      if (asiointikieli != null) {
        if (letterBodyText == null) {
          throw new IllegalArgumentException("Parametri letterBodyText on pakollinen");
        }
        return hyvaksymiskirjeetService.hyvaksymiskirjeetHaulle(
            hyvaksymiskirjeDTO, asiointikieli, KirjeenVastaanottaja.HAKIJA);
      } else {
        return hyvaksymiskirjeetService.hyvaksymiskirjeetHaulleHakukohteittain(hyvaksymiskirjeDTO);
      }
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
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille",
      response = Response.class)
  public ProsessiId taulukkolaskennatKokoHaulle(@QueryParam("hakuOid") String hakuOid) {
    try {
      SijoittelunTulosProsessi prosessi =
          new SijoittelunTulosProsessi(
              null, // ei asiointikielirajausta
              "taulukkolaskennat",
              "Luo taulukkolaskennat haulle",
              hakuOid,
              Arrays.asList("taulukkolaskennat", "haulle"));
      sijoittelunTulosTaulukkolaskentaRoute.taulukkolaskennatHaulle(
          prosessi,
          hakuOid,
          "latest",
          AuthorizationUtil.createAuditSession(httpServletRequestJaxRS),
          SecurityContextHolder.getContext().getAuthentication());
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
