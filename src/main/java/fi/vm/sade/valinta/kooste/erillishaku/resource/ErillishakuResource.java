package fi.vm.sade.valinta.kooste.erillishaku.resource;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.createAuditSession;
import static fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util.PseudoSatunnainenOID.oidHaustaJaHakukohteesta;
import static fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util.PseudoSatunnainenOID.trimToNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import fi.vm.sade.javautils.opintopolku_spring_security.Authorizer;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuJson;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller("ErillishakuResource")
@Path("erillishaku")
@PreAuthorize("isAuthenticated()")
@Api(value = "/erillishaku", description = "Resurssi erillishaun tietojen tuontiin ja vientiin")
public class ErillishakuResource {
  private static final Logger LOG = LoggerFactory.getLogger(ErillishakuResource.class);
  private static final String ROLE_TULOSTENTUONTI =
      "ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI";

  public static final String POIKKEUS_TYHJA_DATAJOUKKO =
      "Syötteestä ei saatu poimittua yhtään hakijaa sijoitteluun tuotavaksi!";
  public static final String RIVIN_TUNNISTE_KAYTTOLIITTYMAAN =
      "Syöte"; // Datarivin tunniste käyttöliittymään
  public static final String POIKKEUS_VIALLINEN_DATAJOUKKO = "Syötteessä oli virheitä!";
  public static final String POIKKEUS_OPPIJANUMEROREKISTERIN_VIRHE =
      "Oppijanumerorekisterikutsu epäonnistui!";
  public static final String POIKKEUS_HAKEMUSPALVELUN_VIRHE = "Hakemuspalvelukutsu epäonnistui!";
  public static final String POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE =
      "Erillishakurivin hakeminen henkilön tiedoilla epäonnistui!";

  @Autowired private Authorizer authorizer;

  @Autowired private DokumenttiProsessiKomponentti dokumenttiKomponentti;

  @Autowired private ErillishaunTuontiService tuontiService;

  @Autowired private ErillishaunVientiService vientiService;
  @Autowired private TarjontaAsyncResource tarjontaResource;

  @Context private HttpServletRequest httpServletRequestJaxRS;

  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @POST
  @Path("/vienti")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Erillishaun hakukohteen vienti taulukkolaskentaan",
      response = ProsessiId.class)
  public ProsessiId vienti(
      @QueryParam("hakutyyppi") Hakutyyppi tyyppi,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("valintatapajonoOid") String valintatapajonoOid) {
    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_TULOSTENTUONTI);
    ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
    dokumenttiKomponentti.tuoUusiProsessi(prosessi);
    vientiService.vie(
        createAuditSession(httpServletRequestJaxRS),
        prosessi,
        new ErillishakuDTO(
            tyyppi,
            hakuOid,
            hakukohdeOid,
            tarjoajaOid,
            Optional.ofNullable(trimToNull(valintatapajonoOid))
                .orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid))));
    return prosessi.toProsessiId();
  }

  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @POST
  @Path("/tuonti")
  @Consumes("application/octet-stream")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Erillishaun hakukohteen tuonti taulukkolaskennalla",
      response = ProsessiId.class)
  public ProsessiId tuonti(
      @QueryParam("hakutyyppi") Hakutyyppi tyyppi,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("valintatapajonoOid") String valintatapajonoOid,
      InputStream file)
      throws Exception {
    LOG.info(
        "Käyttäjä "
            + AuditLog.loggedInUserOid()
            + " tuo excelillä hakuun "
            + hakuOid
            + " hakemuksia");
    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_TULOSTENTUONTI);
    ByteArrayOutputStream b;
    IOUtils.copy(file, b = new ByteArrayOutputStream());
    IOUtils.closeQuietly(file);
    ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
    dokumenttiKomponentti.tuoUusiProsessi(prosessi);
    tuontiService.tuoExcelistä(
        createAuditSession(httpServletRequestJaxRS),
        prosessi,
        new ErillishakuDTO(
            tyyppi,
            hakuOid,
            hakukohdeOid,
            tarjoajaOid,
            Optional.ofNullable(trimToNull(valintatapajonoOid))
                .orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid))),
        new ByteArrayInputStream(b.toByteArray()));
    return prosessi.toProsessiId();
  }

  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @POST
  @Path("/tuonti/json")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Erillishaun hakukohteen tuonti JSON-tietueella",
      response = ProsessiId.class)
  public ProsessiId tuontiJson(
      @ApiParam(allowableValues = "TOISEN_ASTEEN_OPPILAITOS,KORKEAKOULU") @QueryParam("hakutyyppi")
          Hakutyyppi tyyppi,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("valintatapajonoOid") String valintatapajonoOid,
      @ApiParam(
              "maksuvelvollisuus=[EI_TARKISTETTU|MAKSUVELVOLLINEN|EI_MAKSUVELVOLLINEN]<br>"
                  + "maksuntila=[MAKSAMATTA|MAKSETTU|VAPAUTETTU]<br>"
                  + "hakemuksenTila=[HYLATTY|VARALLA|PERUUNTUNUT|HYVAKSYTTY|VARASIJALTA_HYVAKSYTTY|HARKINNANVARAISESTI_HYVAKSYTTY|PERUNUT|PERUUTETTU]<br>"
                  + "vastaanottoTila=[PERUNUT|KESKEN|EI_VASTAANOTTANUT_MAARA_AIKANA|VASTAANOTTANUT_SITOVASTI|PERUUTETTU]<br>"
                  + "ilmoittautumisTila=[EI_TEHTY|LASNA_KOKO_LUKUVUOSI|POISSA_KOKO_LUKUVUOSI|EI_ILMOITTAUTUNUT|LASNA_SYKSY|POISSA_SYKSY|LASNA|POISSA]<br>"
                  + "sukupuoli=[MIES|NAINEN|1|2]<br>"
                  + "aidinkieli=[fi|en|sv|ae|lo|sl|bm|mo|nr|kn|ga|tl|la|nv|ti|gl|to|sa|lv|hi|ke|ty|ho|cv|ts|kj|xx|vo|ro|mr|sd|ak|kv|98|fj|su|sq|<br>"
                  + "ie|ab|ug|hr|my|hy|is|gd|ko|tg|am|bi|so|te|lg|dz|wo|az|oc|kl|kw|sk|uz|oj|ng|uk|gg|se|gu|ii|ne|ce|ee|ur|hu|mt|mg|je|zu|pa|sg|<br>"
                  + "aa|ml|eu|bn|zh|rw|99|ha|nn|or|ta|ks|co|cr|mk|vi|io|lt|bo|ru|ik|ja|be|sc|ka|ay|he|xh|fy|dv|tn|eo|jv|sn|na|os|ln|rn|om|hz|rm|<br>"
                  + "ss|et|bs|af|za|ve|ia|gv|st|mn|mi|fo|ri|gn|ku|es|as|ff|ig|da|av|ch|lb|tr|cy|el|li|ki|nb|lu|sm|no|tw|sw|mh|wa|tt|fr|de|km|fa|<br>"
                  + "ht|kk|yo|ny|qu|ca|an|pt|yi|si|bg|cu|nd|ky|th|sr|ba|kr|ps|br|it|im|id|bh|iu|ar|pl|nl|ms|pi|tk|sh|cs|vk|kg]<br>")
          ErillishakuJson json) {
    LOG.info(
        "Käyttäjä "
            + AuditLog.loggedInUserOid()
            + " päivittää "
            + json.getRivit().size()
            + " kpl haun "
            + hakuOid
            + " hakemusta");
    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_TULOSTENTUONTI);
    ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
    dokumenttiKomponentti.tuoUusiProsessi(prosessi);
    tuontiService.tuoJson(
        createAuditSession(httpServletRequestJaxRS),
        prosessi,
        new ErillishakuDTO(
            tyyppi,
            hakuOid,
            hakukohdeOid,
            tarjoajaOid,
            Optional.ofNullable(trimToNull(valintatapajonoOid))
                .orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid))),
        json.getRivit(),
        true);
    return prosessi.toProsessiId();
  }

  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @POST
  @Path("/tuonti/ui")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Erillishaun hakukohteen tuonti JSON-tietueella",
      response = ProsessiId.class)
  public ProsessiId tuontiJsonFromUI(
      @ApiParam(allowableValues = "TOISEN_ASTEEN_OPPILAITOS,KORKEAKOULU") @QueryParam("hakutyyppi")
          Hakutyyppi tyyppi,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("valintatapajonoOid") String valintatapajonoOid,
      @ApiParam(
              "maksuvelvollisuus=[EI_TARKISTETTU|MAKSUVELVOLLINEN|EI_MAKSUVELVOLLINEN]<br>"
                  + "maksuntila=[MAKSAMATTA|MAKSETTU|VAPAUTETTU]<br>"
                  + "hakemuksenTila=[HYLATTY|VARALLA|PERUUNTUNUT|HYVAKSYTTY|VARASIJALTA_HYVAKSYTTY|HARKINNANVARAISESTI_HYVAKSYTTY|PERUNUT|PERUUTETTU]<br>"
                  + "vastaanottoTila=[PERUNUT|KESKEN|EI_VASTAANOTTANUT_MAARA_AIKANA|VASTAANOTTANUT_SITOVASTI|PERUUTETTU]<br>"
                  + "ilmoittautumisTila=[EI_TEHTY|LASNA_KOKO_LUKUVUOSI|POISSA_KOKO_LUKUVUOSI|EI_ILMOITTAUTUNUT|LASNA_SYKSY|POISSA_SYKSY|LASNA|POISSA]<br>"
                  + "sukupuoli=[MIES|NAINEN|1|2]<br>"
                  + "aidinkieli=[fi|en|sv|ae|lo|sl|bm|mo|nr|kn|ga|tl|la|nv|ti|gl|to|sa|lv|hi|ke|ty|ho|cv|ts|kj|xx|vo|ro|mr|sd|ak|kv|98|fj|su|sq|<br>"
                  + "ie|ab|ug|hr|my|hy|is|gd|ko|tg|am|bi|so|te|lg|dz|wo|az|oc|kl|kw|sk|uz|oj|ng|uk|gg|se|gu|ii|ne|ce|ee|ur|hu|mt|mg|je|zu|pa|sg|<br>"
                  + "aa|ml|eu|bn|zh|rw|99|ha|nn|or|ta|ks|co|cr|mk|vi|io|lt|bo|ru|ik|ja|be|sc|ka|ay|he|xh|fy|dv|tn|eo|jv|sn|na|os|ln|rn|om|hz|rm|<br>"
                  + "ss|et|bs|af|za|ve|ia|gv|st|mn|mi|fo|ri|gn|ku|es|as|ff|ig|da|av|ch|lb|tr|cy|el|li|ki|nb|lu|sm|no|tw|sw|mh|wa|tt|fr|de|km|fa|<br>"
                  + "ht|kk|yo|ny|qu|ca|an|pt|yi|si|bg|cu|nd|ky|th|sr|ba|kr|ps|br|it|im|id|bh|iu|ar|pl|nl|ms|pi|tk|sh|cs|vk|kg]<br>")
          ErillishakuJson json) {
    LOG.info(
        "Käyttäjä "
            + AuditLog.loggedInUserOid()
            + " päivittää "
            + json.getRivit().size()
            + " kpl haun "
            + hakuOid
            + " hakemusta");
    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_TULOSTENTUONTI);
    ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
    dokumenttiKomponentti.tuoUusiProsessi(prosessi);
    tuontiService.tuoJson(
        createAuditSession(true, httpServletRequestJaxRS),
        prosessi,
        new ErillishakuDTO(
            tyyppi,
            hakuOid,
            hakukohdeOid,
            tarjoajaOid,
            Optional.ofNullable(trimToNull(valintatapajonoOid))
                .orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid))),
        json.getRivit(),
        false);
    return prosessi.toProsessiId();
  }

  private String findTarjoajaOid(@QueryParam("hakukohdeOid") String hakukohdeOid) {
    try {
      return tarjontaResource
          .haeHakukohde(hakukohdeOid)
          .get(30, SECONDS)
          .tarjoajaOids
          .iterator()
          .next();
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
