package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiRequest;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuProsessiImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.EPostiService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.service.OsoitetarratService;
import io.reactivex.Observable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Ei palauta PDF-tiedostoa vaan URI:n varsinaiseen resurssiin - koska AngularJS resurssin
 * palauttaman datan konvertoiminen selaimen ladattavaksi tiedostoksi on ongelmallista (mutta ei
 * mahdotonta - onko tarpeen?).
 */
@Controller("ViestintapalveluAktivointiResource")
@Path("viestintapalvelu")
@PreAuthorize("isAuthenticated()")
@Api(
    value = "/viestintapalvelu",
    description = "Osoitetarrojen, jälkiohjauskirjeiden ja hyväksymiskirjeiden tuottaminen")
public class ViestintapalveluAktivointiResource {
  private static final Logger LOG =
      LoggerFactory.getLogger(ViestintapalveluAktivointiResource.class);

  @Autowired private OsoitetarratService osoitetarratService;
  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
  @Autowired private KoekutsukirjeetService koekutsukirjeetService;
  @Autowired private HyvaksymiskirjeetService hyvaksymiskirjeetService;
  @Autowired private EPostiService ePostiService;
  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @POST
  @Path("/osoitetarrat/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Aktivoi osoitetarrojen luonnin hakukohteelle", response = Response.class)
  public ProsessiId aktivoiOsoitetarrojenLuonti(
      DokumentinLisatiedot hakemuksillaRajaus,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("valintakoeTunnisteet") List<String> valintakoeTunnisteet) {
    try {
      DokumentinLisatiedot lisatiedot =
          hakemuksillaRajaus == null ? new DokumentinLisatiedot() : hakemuksillaRajaus;
      DokumenttiProsessi osoiteProsessi =
          new DokumenttiProsessi(
              "Osoitetarrat", "Luo osoitetarrat", null, tags("osoitetarrat", lisatiedot.getTag()));
      dokumenttiProsessiKomponentti.tuoUusiProsessi(osoiteProsessi);

      Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
          .subscribe(
              haku -> {
                if (lisatiedot.getHakemusOids() != null) {
                  osoitetarratService.osoitetarratHakemuksille(
                      osoiteProsessi, lisatiedot.getHakemusOids());
                } else {
                  osoitetarratService.osoitetarratValintakokeeseenOsallistujille(
                      osoiteProsessi, haku, hakukohdeOid, Sets.newHashSet(valintakoeTunnisteet));
                }
              });
      return new ProsessiId(osoiteProsessi.getId());
    } catch (Exception e) {
      LOG.error("Osoitetarrojen luonnissa virhe!", e);
      // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
      // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
      // Ylläpitäjä voi lukea logeista todellisen syyn!
      throw new RuntimeException("Osoitetarrojen luonti epäonnistui!", e);
    }
  }

  /**
   * https://test-virkailija.oph.ware.fi/valintalaskentakoostepalvelu/resources/viestintapalvelu/osoitetarrat/sijoittelussahyvaksytyille/aktivoi?hakuOid=1.2.246.562.5.2013080813081926341927&hakukohdeOid=1.2.246.562.5.85532589612&sijoitteluajoId=1392302745967
   */
  @POST
  @Path("/osoitetarrat/sijoittelussahyvaksytyille/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi hyväksyttyjen osoitteiden luonnin hakukohteelle haussa",
      response = Response.class)
  public ProsessiId aktivoiHyvaksyttyjenOsoitetarrojenLuonti(
      DokumentinLisatiedot hakemuksillaRajaus,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
    try {
      DokumentinLisatiedot lisatiedot =
          hakemuksillaRajaus == null ? new DokumentinLisatiedot() : hakemuksillaRajaus;
      DokumenttiProsessi osoiteProsessi =
          new DokumenttiProsessi(
              "Osoitetarrat",
              "Sijoittelussa hyväksytyille",
              hakuOid,
              tags("osoitetarrat", lisatiedot.getTag()));
      dokumenttiProsessiKomponentti.tuoUusiProsessi(osoiteProsessi);
      Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
          .subscribe(
              haku -> {
                if (lisatiedot.getHakemusOids() != null) {
                  osoitetarratService.osoitetarratHakemuksille(
                      osoiteProsessi, lisatiedot.getHakemusOids());
                } else {
                  osoitetarratService.osoitetarratSijoittelussaHyvaksytyille(
                      osoiteProsessi, haku, hakukohdeOid);
                }
              });
      return osoiteProsessi.toProsessiId();
    } catch (Exception e) {
      LOG.error("Hyväksyttyjen osoitetarrojen luonnissa virhe!", e);
      throw new RuntimeException("Hyväksyttyjen osoitetarrojen luonnissa virhe!", e);
    }
  }

  /**
   * @Deprecated Tehdaan eri luontivariaatiot reitin alustusmuuttujilla. Ei enää monta resurssia per
   * toiminto.
   */
  @POST
  @Path("/osoitetarrat/hakemuksille/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille",
      response = Response.class)
  public ProsessiId aktivoiOsoitetarrojenLuontiHakemuksille(
      DokumentinLisatiedot hakemuksillaRajaus) {
    try {
      if (hakemuksillaRajaus == null) {
        hakemuksillaRajaus = new DokumentinLisatiedot();
      }
      DokumenttiProsessi osoiteProsessi =
          new DokumenttiProsessi(
              "Osoitetarrat",
              "Luo osoitetarrat",
              null,
              tags("osoitetarrat", hakemuksillaRajaus.getTag()));
      dokumenttiProsessiKomponentti.tuoUusiProsessi(osoiteProsessi);
      osoitetarratService.osoitetarratHakemuksille(
          osoiteProsessi, hakemuksillaRajaus.getHakemusOids());
      return new ProsessiId(osoiteProsessi.getId());
    } catch (Exception e) {
      LOG.error("Osoitetarrojen luonnissa virhe!", e);
      // Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
      // todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
      // Ylläpitäjä voi lukea logeista todellisen syyn!
      throw new RuntimeException("Osoitetarrojen luonnissa virhe!", e);
    }
  }

  @POST
  @Path("/jalkiohjauskirjeet/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi jälkiohjauskirjeiden luonnin valitsemattomille",
      response = Response.class)
  public ProsessiId aktivoiJalkiohjauskirjeidenLuonti(
      DokumentinLisatiedot hakemuksillaRajaus,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("templateName") String templateName,
      @QueryParam("tarjoajaOid") String tarjoajaOid,
      @QueryParam("tag") String tag) {
    try {
      if (hakemuksillaRajaus == null) {
        hakemuksillaRajaus = new DokumentinLisatiedot();
      }
      JalkiohjauskirjeDTO jalkiohjauskirjeDTO =
          new JalkiohjauskirjeDTO(
              tarjoajaOid,
              hakemuksillaRajaus.getLetterBodyText(),
              templateName,
              tag,
              hakuOid,
              hakemuksillaRajaus.getLanguageCode() == null
                  ? KieliUtil.SUOMI
                  : hakemuksillaRajaus.getLanguageCode());
      if (hakemuksillaRajaus.getHakemusOids() == null) {
        return hyvaksymiskirjeetService.jalkiohjauskirjeetHaulle(jalkiohjauskirjeDTO);
      } else {
        return hyvaksymiskirjeetService.jalkiohjauskirjeetHakemuksille(
            jalkiohjauskirjeDTO, hakemuksillaRajaus.getHakemusOids());
      }
    } catch (Exception e) {
      LOG.error("Jälkiohjauskirjeiden luonnissa virhe!", e);
      throw new RuntimeException("Jälkiohjauskirjeiden luonti epäonnistui!", e);
    }
  }

  @POST
  @Path("/hakukohteessahylatyt/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi hakukohteessa hylatyille kirjeiden luonnin",
      response = Response.class)
  public ProsessiId aktivoiHakukohteessahylatyilleLuonti(
      DokumentinLisatiedot hakemuksillaRajaus,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("tarjoajaOid") String tarjoajaOid,
      @QueryParam("templateName") String templateName,
      @QueryParam("palautusAika") String palautusAika,
      @QueryParam("palautusPvm") String palautusPvm,
      @QueryParam("tag") String tag,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("sijoitteluajoId") Long sijoitteluajoId,
      @QueryParam("vainTulosEmailinKieltaneet") boolean vainTulosEmailinKieltaneet) {
    try {
      if (templateName == null) {
        templateName = "jalkiohjauskirje";
      }
      if (hakemuksillaRajaus == null) {
        hakemuksillaRajaus = new DokumentinLisatiedot();
      }

      HyvaksymiskirjeDTO hyvaksymiskirjeDTO =
          new HyvaksymiskirjeDTO(
              tarjoajaOid,
              hakemuksillaRajaus.getLetterBodyText(),
              templateName,
              hakemuksillaRajaus.getTag(),
              hakukohdeOid,
              hakuOid,
              sijoitteluajoId,
              palautusPvm,
              palautusAika,
              vainTulosEmailinKieltaneet);
      return hyvaksymiskirjeetService.jalkiohjauskirjeHakukohteelle(hyvaksymiskirjeDTO);
    } catch (Exception e) {
      LOG.error("Hyväksymiskirjeiden luonnissa virhe!", e);
      throw new RuntimeException("Hyväksymiskirjeiden luonti epäonnistui!", e);
    }
  }

  @POST
  @Path("/hyvaksymiskirjeet/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi hyväksymiskirjeiden luonnin hakukohteelle haussa",
      response = Response.class)
  public ProsessiId aktivoiHyvaksymiskirjeidenLuonti(
      DokumentinLisatiedot hakemuksillaRajaus,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("tarjoajaOid") String tarjoajaOid,
      @QueryParam("palautusAika") String palautusAika,
      @QueryParam("palautusPvm") String palautusPvm,
      @QueryParam("templateName") String templateName,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("sijoitteluajoId") Long sijoitteluajoId,
      @QueryParam("asiointikieli") String asiointikieli,
      @QueryParam("vainTulosEmailinKieltaneet") boolean vainTulosEmailinKieltaneet) {
    if (hakuOid == null
        && hakukohdeOid == null
        && (hakemuksillaRajaus.getHakemusOids() == null
            || hakemuksillaRajaus.getHakemusOids().isEmpty())) {
      throw new IllegalArgumentException(
          "Parametri hakuOid tai hakukohdeOid, tai body parametri hakemusOids on pakollinen");
    }
    try {
      if (templateName == null) {
        templateName = "hyvaksymiskirje";
      }
      if (hakemuksillaRajaus == null) {
        hakemuksillaRajaus = new DokumentinLisatiedot();
      }

      HyvaksymiskirjeDTO hyvaksymiskirjeDTO =
          new HyvaksymiskirjeDTO(
              tarjoajaOid,
              hakemuksillaRajaus.getLetterBodyText(),
              templateName,
              hakemuksillaRajaus.getTag(),
              hakukohdeOid,
              hakuOid,
              sijoitteluajoId,
              palautusPvm,
              palautusAika,
              vainTulosEmailinKieltaneet);
      if (hakemuksillaRajaus.getHakemusOids() == null
          || hakemuksillaRajaus.getHakemusOids().isEmpty()) {
        if (hakukohdeOid == null) {
          if (asiointikieli == null) {
            LOG.info(
                String.format(
                    "Hyväksymiskirjeiden luonti aktivoitu hakukohteittain haulle %s, vainTulosEmailinKieltaneet: %s",
                    hakuOid, vainTulosEmailinKieltaneet));
            return hyvaksymiskirjeetService.hyvaksymiskirjeetHaulleHakukohteittain(
                hyvaksymiskirjeDTO);
          } else {
            LOG.info(
                String.format(
                    "Hyväksymiskirjeiden luonti aktivoitu haulle %s, vainTulosEmailinKieltaneet: %s, asiointikieli: %s",
                    hakuOid, vainTulosEmailinKieltaneet, asiointikieli));
            return hyvaksymiskirjeetService.hyvaksymiskirjeetHaulle(
                hyvaksymiskirjeDTO, asiointikieli);
          }
        } else {
          LOG.info(
              String.format(
                  "Hyväksymiskirjeiden luonti aktivoitu hakukohteelle %s, vainTulosEmailinKieltaneet: %s",
                  hakukohdeOid, vainTulosEmailinKieltaneet));
          return hyvaksymiskirjeetService.hyvaksymiskirjeetHakukohteelle(hyvaksymiskirjeDTO);
        }
      } else {
        LOG.info(
            String.format(
                "Hyväksymiskirjeiden luonti aktivoitu hakemuksille, vainTulosEmailinKieltaneet: %s",
                vainTulosEmailinKieltaneet));
        return hyvaksymiskirjeetService.hyvaksymiskirjeetHakemuksille(
            hyvaksymiskirjeDTO, hakemuksillaRajaus.getHakemusOids());
      }
    } catch (Exception e) {
      LOG.error("Hyväksymiskirjeiden luonnissa virhe!", e);
      throw new RuntimeException("Hyväksymiskirjeiden luonti epäonnistui!", e);
    }
  }

  @POST
  @Path("/koekutsukirjeet/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(
      value = "Aktivoi koekutsukirjeiden luonnin hakukohteelle haussa",
      response = Response.class)
  public ProsessiId aktivoiKoekutsukirjeidenLuonti(
      @QueryParam(OPH.HAKUOID) String hakuOid,
      @QueryParam(OPH.HAKUKOHDEOID) String hakukohdeOid,
      @QueryParam(OPH.TARJOAJAOID) String tarjoajaOid,
      @QueryParam("templateName") String templateName,
      @QueryParam("valintakoeTunnisteet") List<String> valintakoeTunnisteet,
      DokumentinLisatiedot hakemuksillaRajaus) {
    if ((hakemuksillaRajaus == null
            || hakemuksillaRajaus.getHakemusOids() == null
            || hakemuksillaRajaus.getHakemusOids().isEmpty())
        && (hakukohdeOid == null
            || valintakoeTunnisteet == null
            || valintakoeTunnisteet.isEmpty())) {
      LOG.error(
          "Valintakokeen tunniste tai tunnisteet ja hakukohde on pakollisia tietoja koekutsukirjeen luontiin!");
      throw new RuntimeException(
          "Valintakokeen tunniste tai tunnisteet ja hakukohde on pakollisia tietoja koekutsukirjeen luontiin!");
    }
    KoekutsuProsessiImpl prosessi = new KoekutsuProsessiImpl(2);
    try {
      String template = templateName == null ? "koekutsukirje" : templateName;
      DokumentinLisatiedot lisatiedot =
          hakemuksillaRajaus == null ? new DokumentinLisatiedot() : hakemuksillaRajaus;
      String tag = lisatiedot.getTag();
      Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
          .subscribe(
              haku -> {
                if (lisatiedot.getHakemusOids() != null) {
                  LOG.info(
                      "Koekutsukirjeiden luonti aloitettu yksittaiselle hakemukselle {}",
                      lisatiedot.getHakemusOids());
                  koekutsukirjeetService.koekutsukirjeetHakemuksille(
                      prosessi,
                      new KoekutsuDTO(
                          lisatiedot.getLetterBodyText(),
                          tarjoajaOid,
                          tag,
                          hakukohdeOid,
                          haku,
                          template),
                      lisatiedot.getHakemusOids());
                } else {
                  LOG.info("Koekutsukirjeiden luonti aloitettu");
                  koekutsukirjeetService.koekutsukirjeetOsallistujille(
                      prosessi,
                      new KoekutsuDTO(
                          lisatiedot.getLetterBodyText(),
                          tarjoajaOid,
                          tag,
                          hakukohdeOid,
                          haku,
                          template),
                      valintakoeTunnisteet);
                }
              });
      dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
    } catch (Exception e) {
      LOG.error("Koekutsukirjeiden luonti epäonnistui!", e);
      throw new RuntimeException(e);
    }
    return new ProsessiId(prosessi.getId()); // Response.ok().build();
  }

  @POST
  @Path("/securelinkit/aktivoi")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Lähettää Secure Linkit ryhmäsähköpostilla", response = AsyncResponse.class)
  public void secureLinkkienLahetys(
      EPostiRequest ePostiRequest, @Suspended AsyncResponse asyncResponse) {

    setAsyncTimeout(
        asyncResponse,
        "Securelinkien lähetys -palvelukutsu on aikakatkaistu: /viestintapalvelu/securelinkit/aktivoi/");

    validateEPostiRequest(ePostiRequest, asyncResponse);

    ePostiService.lahetaSecurelinkit(
        ePostiRequest,
        (response) ->
            asyncResponse.resume(Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build()),
        (errorMessage) ->
            errorResponse(
                String.format("Securelinkien lähetys epäonnistui! %s", errorMessage),
                asyncResponse));
  }

  @GET
  @Path("/securelinkit/esikatselu")
  @Produces("message/rfc822")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Esikatsele Secure Linkin ryhmäsähköposti", response = AsyncResponse.class)
  public void secureLinkkienEsikatselu(
      @QueryParam(OPH.HAKUOID) String hakuOid,
      @QueryParam("kirjeenTyyppi") String kirjeenTyyppi,
      @QueryParam("asiointikieli") String asiointikieli,
      @Suspended AsyncResponse asyncResponse) {

    setAsyncTimeout(
        asyncResponse,
        "Securelinkin esikatselu -palvelukutsu on aikakatkaistu: /viestintapalvelu/securelinkit/esikatselu");

    EPostiRequest ePostiRequest = new EPostiRequest();
    ePostiRequest.setHakuOid(hakuOid);
    ePostiRequest.setKirjeenTyyppi(kirjeenTyyppi);
    ePostiRequest.setAsiointikieli(asiointikieli);
    validateEPostiRequest(ePostiRequest, asyncResponse);

    ePostiService.esikatseleSecurelinkki(
        ePostiRequest,
        (response) -> asyncResponse.resume(response),
        (errorMessage) ->
            errorResponse(
                String.format("Securelinkin esikatselu epäonnistui! %s", errorMessage),
                asyncResponse));
  }

  private void validateEPostiRequest(EPostiRequest ePostiRequest, AsyncResponse asyncResponse) {
    String hakuOid = ePostiRequest.getHakuOid();
    String kirjeenTyyppi = ePostiRequest.getKirjeenTyyppi();
    String asiointikieli = ePostiRequest.getAsiointikieli();

    if (StringUtils.isBlank(hakuOid)
        || StringUtils.isBlank(kirjeenTyyppi)
        || StringUtils.isBlank(asiointikieli)) {
      LOG.error("HakuOid, asiointikieli ja kirjeenTyyppi ovat pakollisia parametreja.");
      errorResponse(
          "HakuOid, asiointikieli ja kirjeenTyyppi ovat pakollisia parametreja.", asyncResponse);
    }
    if (!("jalkiohjauskirje".equals(kirjeenTyyppi) || "hyvaksymiskirje".equals(kirjeenTyyppi))) {
      LOG.error(
          "{} ei ole validi kirjeen tyyppi. Pitää olla 'jalkiohjauskirje' tai 'hyvaksymiskirje'.",
          kirjeenTyyppi);
      errorResponse(
          kirjeenTyyppi
              + " ei ole validi kirjeen tyyppi. Pitää olla 'jalkiohjauskirje' tai 'hyvaksymiskirje'.",
          asyncResponse);
    }
    if (!("fi".equals(asiointikieli) || "sv".equals(asiointikieli) || "en".equals(asiointikieli))) {
      LOG.error("{} ei ole validi asiointikieli. Pitää olla 'fi', 'sv' tai 'en'.", asiointikieli);
      errorResponse(
          asiointikieli + " ei ole validi asiointikieli. Pitää olla 'fi', 'sv' tai 'en'.",
          asyncResponse);
    }
  }

  private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
    response.setTimeout(5L, TimeUnit.MINUTES);
    response.setTimeoutHandler(asyncResponse -> errorResponse(timeoutMessage, asyncResponse));
  }

  private void errorResponse(String timeoutMessage, AsyncResponse asyncResponse) {
    asyncResponse.resume(
        Response.serverError()
            .entity(ImmutableMap.of("error", timeoutMessage))
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build());
  }

  private List<String> tags(String... tag) {
    List<String> l = Lists.newArrayList();
    for (String t : tag) {
      if (t != null) {
        l.add(t);
      }
    }
    return l;
  }
}
