package fi.vm.sade.valinta.kooste.hakukohteet;

import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
@RequestMapping("/resources/hakukohteet")
@PreAuthorize("isAuthenticated()")
@Tag(name = "/hakukohteet", description = "Hakukohteet")
public class HakukohteetResource {
  private final TarjontaAsyncResource resource;
  private final Logger LOG = LoggerFactory.getLogger(HakukohteetResource.class);

  @Autowired
  public HakukohteetResource(TarjontaAsyncResource resource) {
    this.resource = resource;
  }

  @GetMapping(value = "/search/{hakuOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTAPERUSTEET_READ', 'ROLE_APP_VALINTAPERUSTEET_CRUD')")
  @Operation(
      summary = "Hae kouta-hakukohteita valinnan tietojen perusteella",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = List.class)))
      })
  public DeferredResult<ResponseEntity<List<KoutaHakukohde>>> searchHakukohteet(
      @PathVariable("hakuOid") String hakuOid,
      @RequestParam("hasValintakoe") Boolean hasValintakoe) {
    DeferredResult<ResponseEntity<List<KoutaHakukohde>>> result =
        new DeferredResult<>((Long) (60 * 1000L));
    try {
      result.onTimeout(
          () -> {
            LOG.error("Hakukohteet -kutsu aikakatkaistiin haulle {}", hakuOid);
            result.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Hakukohteet -kutsu aikakatkaistiin"));
          });

      resource
          .searchKoutaHakukohteet(hakuOid, hasValintakoe)
          .whenCompleteAsync(
              (hakukohteet, e) -> {
                if (e != null) {
                  LOG.error("Hakukohteet-kutsu epäonnistui haulle " + hakuOid, e);
                  result.setErrorResult(
                      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Hakukohteet-kutsu epäonnistui" + e.getMessage()));
                } else {
                  result.setResult(ResponseEntity.status(HttpStatus.OK).body(hakukohteet));
                }
              });
    } catch (Exception e) {
      String msg = "Odottamaton virhe hakukohteiden hakemisessa haulle " + hakuOid;
      LOG.error(msg, e);
      result.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg));
    }

    return result;
  }
}
