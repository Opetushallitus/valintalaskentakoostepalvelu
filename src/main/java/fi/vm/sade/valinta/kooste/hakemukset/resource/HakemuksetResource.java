package fi.vm.sade.valinta.kooste.hakemukset.resource;

import static java.util.Arrays.asList;

import com.google.common.base.Preconditions;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.hakemukset.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.hakemukset.service.ValinnanvaiheenValintakoekutsutService;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import io.reactivex.Observable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController("HakemuksetResource")
@RequestMapping("/resources/hakemukset")
@PreAuthorize("isAuthenticated()")
@Tag(name = "/hakemukset", description = "Hakemusten hakeminen")
public class HakemuksetResource {
  private static final Logger LOG = LoggerFactory.getLogger(HakemuksetResource.class);

  @Autowired private AuthorityCheckService authorityCheckService;

  @Autowired private ValinnanvaiheenValintakoekutsutService valinnanvaiheenValintakoekutsutService;

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GetMapping(value = "/valinnanvaihe", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Valinnanvaiheen hakemusten listaus",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = HakemusDTO.class)))
      })
  public DeferredResult<Collection<HakemusDTO>> hakemuksetValinnanvaiheelle(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "valinnanvaiheOid", required = false) String valinnanvaiheOid,
      HttpServletRequest request) {
    Preconditions.checkNotNull(hakuOid);
    Preconditions.checkNotNull(valinnanvaiheOid);

    DeferredResult<Collection<HakemusDTO>> deferredResult = new DeferredResult<>(3600000l);

    Map<String, String> additionalAuditInfo = new HashMap<>();
    additionalAuditInfo.put("hakuOid", hakuOid);
    additionalAuditInfo.put("ValinnanvaiheOid", valinnanvaiheOid);

    AuditLog.log(
        KoosteAudit.AUDIT,
        AuthorizationUtil.createAuditSession(request).asAuditUser(),
        ValintaperusteetOperation.VALINNANVAIHEEN_HAKEMUKSET_HAKU,
        ValintaResource.HAKEMUKSET,
        valinnanvaiheOid,
        Changes.EMPTY,
        additionalAuditInfo);

    LOG.warn(
        "Aloitetaan hakemusten listaaminen valinnanvaiheelle {} haussa {}",
        valinnanvaiheOid,
        hakuOid);
    Long started = System.currentTimeMillis();

    Observable.fromFuture(
            authorityCheckService.getAuthorityCheckForRoles(
                asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_READ",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD")))
        .subscribe(
            authCheck -> {
              valinnanvaiheenValintakoekutsutService.hae(
                  valinnanvaiheOid,
                  hakuOid,
                  authCheck,
                  hakemusDTOs -> {
                    long duration = (System.currentTimeMillis() - started) / 1000;
                    LOG.info(
                        "hakemusten listaaminen valinnanvaiheelle {} haussa {} kesti {} sekuntia",
                        valinnanvaiheOid,
                        hakuOid,
                        duration);
                    deferredResult.setResult(hakemusDTOs);
                  },
                  exception -> {
                    long duration = (System.currentTimeMillis() - started) / 1000;
                    if (exception
                        instanceof
                        ValinnanvaiheenValintakoekutsutService
                            .ValinnanvaiheelleEiLoydyValintaryhmiaException) {
                      LOG.error(
                          String.format(
                              "%s : kesto %d sekuntia", exception.getMessage(), duration));
                      deferredResult.setErrorResult(
                          ResponseEntity.status(HttpStatus.BAD_REQUEST)
                              .body(exception.getMessage()));
                    } else {
                      LOG.error(
                          String.format(
                              "hakemusten listaaminen epäonnistui (valinnanvaihe %s, haku %s, kesto %d sekuntia",
                              valinnanvaiheOid, hakuOid, duration),
                          exception);
                      deferredResult.setErrorResult(
                          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                              .body(exception.getMessage()));
                    }
                  });
            },
            exception -> {
              String errorMessage = "hakemusten listaaminen epäonnistui, authCheck failed";
              LOG.error(
                  HttpExceptionWithResponse.appendWrappedResponse(errorMessage, exception),
                  exception);
              deferredResult.setErrorResult(errorMessage);
            });

    return deferredResult;
  }
}
