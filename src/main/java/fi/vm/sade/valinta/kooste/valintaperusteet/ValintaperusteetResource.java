package fi.vm.sade.valinta.kooste.valintaperusteet;

import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import io.reactivex.Observable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.stream.Collectors;
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
@RequestMapping("/valintaperusteet")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintaperusteet", description = "Valintaperusteet")
public class ValintaperusteetResource {
  private final ValintaperusteetAsyncResource resource;
  private final Logger LOG = LoggerFactory.getLogger(ValintaperusteetResource.class);

  @Autowired
  public ValintaperusteetResource(ValintaperusteetAsyncResource resource) {
    this.resource = resource;
  }

  @GetMapping(
      value = "/hakukohde/{hakukohdeOid}/kayttaaValintalaskentaa",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTAPERUSTEET_READ', 'ROLE_APP_VALINTAPERUSTEET_CRUD')")
  @ApiOperation(
      value = "Käyttääkö hakukohde valintalaskentaa",
      response = ValintaperusteetResourceResult.class)
  public DeferredResult<ResponseEntity<ValintaperusteetResourceResult>> kayttaaValintalaskentaa(
      @PathVariable("hakukohdeOid") String hakukohdeOid) {
    DeferredResult<ResponseEntity<ValintaperusteetResourceResult>> result =
        new DeferredResult<>(1 * 60 * 1000l);
    try {
      result.onTimeout(
          () -> {
            LOG.error("Valintaperusteet -kutsu aikakatkaistiin hakukohteelle {}", hakukohdeOid);
            result.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Valintaperusteet -kutsu aikakatkaistiin"));
          });

      Observable.fromFuture(resource.haeValintaperusteet(hakukohdeOid, null))
          .subscribe(
              valintaperusteetDTOs -> {
                boolean kayttaaValintalaskentaa =
                    !valintaperusteetDTOs.stream()
                        .filter(
                            v ->
                                v.getViimeinenValinnanvaihe()
                                    == v.getValinnanVaihe().getValinnanVaiheJarjestysluku())
                        .filter(
                            v ->
                                v.getValinnanVaihe().getValintatapajono().stream()
                                    .anyMatch(
                                        ValintatapajonoJarjestyskriteereillaDTO
                                            ::getKaytetaanValintalaskentaa))
                        .collect(Collectors.toList())
                        .isEmpty();

                result.setResult(
                    ResponseEntity.status(HttpStatus.OK)
                        .body(new ValintaperusteetResourceResult(kayttaaValintalaskentaa)));
              },
              e -> {
                LOG.error("Valintaperusteet -kutsu epäonnistui hakukohteelle " + hakukohdeOid, e);
                result.setErrorResult(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Valintaperusteet -kutsu epäonnistui" + e.getMessage()));
              });
    } catch (Exception e) {
      String msg = "Odottamaton virhe valintalaskentapäättelyssä hakukohteelle " + hakukohdeOid;
      LOG.error(msg, e);
      result.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg));
    }

    return result;
  }

  public static class ValintaperusteetResourceResult {
    public boolean kayttaaValintalaskentaa;

    public ValintaperusteetResourceResult() {}

    public ValintaperusteetResourceResult(boolean kayttaaValintalaskentaa) {
      this.kayttaaValintalaskentaa = kayttaaValintalaskentaa;
    }
  }
}
