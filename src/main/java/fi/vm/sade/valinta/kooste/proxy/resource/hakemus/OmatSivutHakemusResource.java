package fi.vm.sade.valinta.kooste.proxy.resource.hakemus;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/proxy/valintatulos")
public class OmatSivutHakemusResource {

  private static final Logger LOG = LoggerFactory.getLogger(OmatSivutHakemusResource.class);

  @Autowired private ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;

  @GetMapping(
      value = "/haku/{hakuOid}/hakemusOid/{hakemusOid:.+}",
      produces = MediaType.APPLICATION_JSON_VALUE + ";charset:utf-8")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
  public DeferredResult<ResponseEntity<String>> getValintatulos(
      @PathVariable("hakuOid") String hakuOid, @PathVariable("hakemusOid") String hakemusOid) {

    DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(30000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "getValintatulos proxy -palvelukutsu on aikakatkaistu: /valintatulos/haku/{hakuOid}/hakemusOid/{hakemusOid}",
              hakuOid,
              hakemusOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Valintatulokset proxy -palvelukutsu on aikakatkaistu"));
        });
    valintaTulosServiceAsyncResource
        .getHakemuksenValintatulosAsString(hakuOid, hakemusOid)
        .subscribe(
            toiveenValintaTulokset ->
                result.setResult(ResponseEntity.status(HttpStatus.OK).body(toiveenValintaTulokset)),
            error -> {
              LOG.error("getHakemuksenValintatulosAsString throws", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
            });

    return result;
  }
}
