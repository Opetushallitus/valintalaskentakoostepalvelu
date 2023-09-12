package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.*;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.KirjeenVastaanottaja;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.EPostiService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.service.OsoitetarratService;
import io.reactivex.Observable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Ei palauta PDF-tiedostoa vaan URI:n varsinaiseen resurssiin - koska AngularJS resurssin
 * palauttaman datan konvertoiminen selaimen ladattavaksi tiedostoksi on ongelmallista (mutta ei
 * mahdotonta - onko tarpeen?).
 */
@RestController("ViestintapalveluAktivointiResource")
@RequestMapping("/resources/viestintapalvelu")
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "/viestintapalvelu",
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

  @PostMapping(
      value = "/osoitetarrat/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi osoitetarrojen luonnin hakukohteelle",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiOsoitetarrojenLuonti(
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus,
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "valintakoeTunnisteet", required = false)
          List<String> valintakoeTunnisteet) {
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
  @PostMapping(
      value = "/osoitetarrat/sijoittelussahyvaksytyille/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi hyväksyttyjen osoitteiden luonnin hakukohteelle haussa",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiHyvaksyttyjenOsoitetarrojenLuonti(
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "hakuOid", required = false) String hakuOid) {
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
  @PostMapping(
      value = "/osoitetarrat/hakemuksille/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiOsoitetarrojenLuontiHakemuksille(
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus) {
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

  @PostMapping(
      value = "/jalkiohjauskirjeet/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi jälkiohjauskirjeiden luonnin valitsemattomille",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiJalkiohjauskirjeidenLuonti(
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus,
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "templateName", required = false) String templateName,
      @RequestParam(value = "tarjoajaOid", required = false) String tarjoajaOid,
      @RequestParam(value = "tag", required = false) String tag) {
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
        KirjeenVastaanottaja kirjeenVastaanottaja =
            "jalkiohjauskirje_huoltajille".equals(templateName)
                ? KirjeenVastaanottaja.HUOLTAJAT
                : KirjeenVastaanottaja.HAKIJA;
        return hyvaksymiskirjeetService.jalkiohjauskirjeetHaulle(
            jalkiohjauskirjeDTO, kirjeenVastaanottaja);
      } else {
        return hyvaksymiskirjeetService.jalkiohjauskirjeetHakemuksille(
            jalkiohjauskirjeDTO, hakemuksillaRajaus.getHakemusOids());
      }
    } catch (Exception e) {
      LOG.error("Jälkiohjauskirjeiden luonnissa virhe!", e);
      throw new RuntimeException("Jälkiohjauskirjeiden luonti epäonnistui!", e);
    }
  }

  @PostMapping(
      value = "/hakukohteessahylatyt/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi hakukohteessa hylatyille kirjeiden luonnin",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiHakukohteessahylatyilleLuonti(
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "tarjoajaOid", required = false) String tarjoajaOid,
      @RequestParam(value = "templateName", required = false) String templateName,
      @RequestParam(value = "palautusAika", required = false) String palautusAika,
      @RequestParam(value = "palautusPvm", required = false) String palautusPvm,
      @RequestParam(value = "tag", required = false) String tag,
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "sijoitteluajoId", required = false) Long sijoitteluajoId,
      @RequestParam(value = "vainTulosEmailinKieltaneet", defaultValue = "false")
          boolean vainTulosEmailinKieltaneet) {
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

  @PostMapping(
      value = "/hyvaksymiskirjeet/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi hyväksymiskirjeiden luonnin hakukohteelle haussa",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiHyvaksymiskirjeidenLuonti(
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "tarjoajaOid", required = false) String tarjoajaOid,
      @RequestParam(value = "palautusAika", required = false) String palautusAika,
      @RequestParam(value = "palautusPvm", required = false) String palautusPvm,
      @RequestParam(value = "templateName", required = false) String templateName,
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "sijoitteluajoId", required = false) Long sijoitteluajoId,
      @RequestParam(value = "asiointikieli", required = false) String asiointikieli,
      @RequestParam(value = "vainTulosEmailinKieltaneet", required = false)
          boolean vainTulosEmailinKieltaneet) {
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
                    "Hyväksymiskirjeiden luonti aktivoitu haulle %s, vainTulosEmailinKieltaneet: %s, asiointikieli: %s, templateName: %s",
                    hakuOid, vainTulosEmailinKieltaneet, asiointikieli, templateName));
            KirjeenVastaanottaja hyvaksymiskirjeenVastaanottaja =
                "hyvaksymiskirje_huoltajille".equals(templateName)
                    ? KirjeenVastaanottaja.HUOLTAJAT
                    : KirjeenVastaanottaja.HAKIJA;
            return hyvaksymiskirjeetService.hyvaksymiskirjeetHaulle(
                hyvaksymiskirjeDTO, asiointikieli, hyvaksymiskirjeenVastaanottaja);
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

  @PostMapping(
      value = "/koekutsukirjeet/aktivoi",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Aktivoi koekutsukirjeiden luonnin hakukohteelle haussa",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId aktivoiKoekutsukirjeidenLuonti(
      @RequestParam(OPH.HAKUOID) String hakuOid,
      @RequestParam(OPH.HAKUKOHDEOID) String hakukohdeOid,
      @RequestParam(OPH.TARJOAJAOID) String tarjoajaOid,
      @RequestParam("templateName") String templateName,
      @RequestParam("valintakoeTunnisteet") List<String> valintakoeTunnisteet,
      @RequestBody DokumentinLisatiedot hakemuksillaRajaus) {
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

  @PostMapping(value = "/securelinkit/aktivoi", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Lähettää Secure Linkit ryhmäsähköpostilla",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public DeferredResult<ResponseEntity<EPostiResponse>> secureLinkkienLahetys(
      @RequestBody EPostiRequest ePostiRequest) {

    DeferredResult<ResponseEntity<EPostiResponse>> result = new DeferredResult<>(5 * 60 * 1000l);
    result.onTimeout(
        () -> {
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body(
                      "Securelinkien lähetys -palvelukutsu on aikakatkaistu: /viestintapalvelu/securelinkit/aktivoi/"));
        });

    if (validateEPostiRequest(ePostiRequest, result)) {
      ePostiService.lahetaSecurelinkit(
          ePostiRequest,
          (ePostiResponse) ->
              result.setResult(ResponseEntity.status(HttpStatus.OK).body(ePostiResponse)),
          (errorMessage) ->
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(String.format("Securelinkien lähetys epäonnistui! %s", errorMessage))));
    }

    return result;
  }

  @GetMapping(value = "/securelinkit/esikatselu", produces = "message/rfc822")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @Operation(
      summary = "Esikatsele Secure Linkin ryhmäsähköposti",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = byte[].class)))
      })
  public DeferredResult<ResponseEntity<byte[]>> secureLinkkienEsikatselu(
      @RequestParam(OPH.HAKUOID) String hakuOid,
      @RequestParam("kirjeenTyyppi") String kirjeenTyyppi,
      @RequestParam("asiointikieli") String asiointikieli) {

    DeferredResult<ResponseEntity<byte[]>> result = new DeferredResult<>(5 * 60 * 1000l);
    result.onTimeout(
        () ->
            result.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(
                        "Securelinkin esikatselu -palvelukutsu on aikakatkaistu: /viestintapalvelu/securelinkit/esikatselu")));

    EPostiRequest ePostiRequest = new EPostiRequest();
    ePostiRequest.setHakuOid(hakuOid);
    ePostiRequest.setKirjeenTyyppi(kirjeenTyyppi);
    ePostiRequest.setAsiointikieli(asiointikieli);
    validateEPostiRequest(ePostiRequest, result);

    ePostiService.esikatseleSecurelinkki(ePostiRequest, result);

    return result;
  }

  private <T> boolean validateEPostiRequest(
      EPostiRequest ePostiRequest, DeferredResult<ResponseEntity<T>> result) {
    String hakuOid = ePostiRequest.getHakuOid();
    String kirjeenTyyppi = ePostiRequest.getKirjeenTyyppi();
    String asiointikieli = ePostiRequest.getAsiointikieli();

    if (StringUtils.isBlank(hakuOid)
        || StringUtils.isBlank(kirjeenTyyppi)
        || StringUtils.isBlank(asiointikieli)) {
      LOG.error("HakuOid, asiointikieli ja kirjeenTyyppi ovat pakollisia parametreja.");
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("HakuOid, asiointikieli ja kirjeenTyyppi ovat pakollisia parametreja."));
      return false;
    }
    if (!("jalkiohjauskirje".equals(kirjeenTyyppi)
        || "jalkiohjauskirje_huoltajille".equals(kirjeenTyyppi)
        || "hyvaksymiskirje".equals(kirjeenTyyppi)
        || "hyvaksymiskirje_huoltajille".equals(kirjeenTyyppi))) {
      LOG.error(
          "{} ei ole validi kirjeen tyyppi. Pitää olla 'jalkiohjauskirje', 'jalkiohjauskirje_huoltajille', 'hyvaksymiskirje' tai 'hyvaksymiskirje_huoltajille'.",
          kirjeenTyyppi);
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(
                  kirjeenTyyppi
                      + " ei ole validi kirjeen tyyppi. Pitää olla 'jalkiohjauskirje', 'jalkiohjauskirje_huoltajille', 'hyvaksymiskirje' tai 'hyvaksymiskirje_huoltajille'."));
      return false;
    }
    if (!("fi".equals(asiointikieli) || "sv".equals(asiointikieli) || "en".equals(asiointikieli))) {
      LOG.error("{} ei ole validi asiointikieli. Pitää olla 'fi', 'sv' tai 'en'.", asiointikieli);
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(
                  asiointikieli + " ei ole validi asiointikieli. Pitää olla 'fi', 'sv' tai 'en'."));
      return false;
    }
    return true;
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
