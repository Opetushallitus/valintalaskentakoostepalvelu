package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static java.util.Arrays.asList;

import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaResurssiProvider;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController("ValintalaskentaKerrallaResource")
@RequestMapping("/resources/valintalaskentakerralla")
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "/valintalaskentakerralla",
    description = "Valintalaskenta kaikille valinnanvaiheille kerralla")
public class ValintalaskentaKerrallaResource {
  private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaResource.class);
  private static final List<String> valintalaskentaAllowedRoles =
      asList(
          "ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD",
          "ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ_UPDATE",
          "ROLE_APP_VALINTOJENTOTEUTTAMINENKK_CRUD",
          "ROLE_APP_VALINTOJENTOTEUTTAMINENKK_READ_UPDATE");

  @Autowired private AuthorityCheckService authorityCheckService;
  @Autowired private LaskentaResurssiProvider laskentaResurssiProvider;

  private static AuditSession koosteAuditSession() {
    final String userOID = AuthorizationUtil.getCurrentUser();
    final String userAgent = "-";
    final String inetAddress = "127.0.0.1";
    AuditSession auditSession =
        new AuditSession(userOID, Collections.emptyList(), userAgent, inetAddress);
    auditSession.setSessionId("");
    auditSession.setPersonOid(userOID);
    return auditSession;
  }

  @GetMapping(
      value = "/haku/{hakuOid}/hakukohde/{hakukohdeOid}/lahtotiedot",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeferredResult<ResponseEntity<LaskeDTO>> valintalaskennanLahtotiedot(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("hakukohdeOid") String hakukohdeOid,
      @RequestParam(value = "uuid", defaultValue = "123") String uuid,
      @RequestParam(value = "erillishaku", required = false, defaultValue = "false")
          Boolean erillishaku,
      @RequestParam(value = "valinnanvaihe", required = false, defaultValue = "-1")
          Integer valinnanvaihe,
      @RequestParam(value = "valintakoelaskenta", required = false, defaultValue = "false")
          Boolean valintakoelaskenta,
      @RequestParam(value = "retryHakemuksetAndOppijat", required = false, defaultValue = "false")
          Boolean retryHakemuksetAndOppijat,
      @RequestParam(value = "withHakijaRyhmat", required = false, defaultValue = "false")
          Boolean withHakijaRyhmat) {
    authorityCheckService.checkAuthorizationForHaku(hakuOid, valintalaskentaAllowedRoles);
    DeferredResult<ResponseEntity<LaskeDTO>> result = new DeferredResult<>(5 * 60 * 1000l);

    Date nyt = new Date();

    try {
      result.onTimeout(
          () -> {
            LOG.error(
                "Lähtötietojen haku timeuottasi kutsulle /haku/{}/hakukohde/{}/lahtotiedot",
                hakuOid,
                hakukohdeOid);
            result.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Lähtötietojen haku hakukohteelle aikakatkaistu!"));
          });

      this.laskentaResurssiProvider
          .fetchResourcesForOneLaskenta(
              uuid,
              hakuOid,
              hakukohdeOid,
              valinnanvaihe == -1 ? null : valinnanvaihe,
              koosteAuditSession(),
              erillishaku,
              retryHakemuksetAndOppijat,
              withHakijaRyhmat,
              nyt)
          .thenApply(laskeDTO -> result.setResult(new ResponseEntity<>(laskeDTO, HttpStatus.OK)));
    } catch (Throwable e) {
      LOG.error(
          "Hakukohteen " + hakukohdeOid + " tietojen hakemisessa tapahtui odottamaton virhe!", e);
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Odottamaton virhe laskennan tietojen hakemisessa! " + e.getMessage()));
    }

    return result;
  }
}
