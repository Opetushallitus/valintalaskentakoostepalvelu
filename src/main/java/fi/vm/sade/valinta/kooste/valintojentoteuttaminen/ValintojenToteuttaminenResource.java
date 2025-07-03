package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/resources/valintojen-toteuttaminen")
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "/valintojen-toteuttaminen",
    description = "Rajapintoja valintojen toteuttamisen käyttöliittymää varten")
public class ValintojenToteuttaminenResource {
  private static final String VALINTAKAYTTAJA_ROLE =
      "hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ',"
          + "'ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ_UPDATE','ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD')";
  private final ValintojenToteuttaminenService service;
  private final Logger LOG = LoggerFactory.getLogger(ValintojenToteuttaminenResource.class);

  @Autowired
  public ValintojenToteuttaminenResource(ValintojenToteuttaminenService service) {
    this.service = service;
  }

  @GetMapping(
      value = "/haku/{hakuOid}/valintatiedot-hakukohteittain",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(VALINTAKAYTTAJA_ROLE)
  @Operation(
      summary =
          "Hae haun hakukohteiden valintatietoja, joita käytetään hakukohteiden suodatuksessa",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = List.class)))
      })
  public DeferredResult<ResponseEntity<Map<String, HakukohteenValintatiedot>>> searchHakukohteet(
      @PathVariable("hakuOid") String hakuOid) {
    DeferredResult<ResponseEntity<Map<String, HakukohteenValintatiedot>>> result =
        new DeferredResult<>(60 * 1000L);
    try {
      result.onTimeout(
          () -> {
            LOG.error("valintatiedot hakukohteittain -kutsu aikakatkaistiin haulle {}", hakuOid);
            result.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Valintatiedot hakukohteittan -kutsu aikakatkaistiin"));
          });

      service
          .valintatiedotHakukohteittain(hakuOid)
          .whenComplete(
              (hakukohteet, error) -> {
                if (error != null) {
                  LOG.error(
                      "Valintatiedot hakukohteittain -kutsu epäonnistui haulle " + hakuOid, error);
                  result.setErrorResult(
                      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body(
                              "Valintatiedot hakukohteittain -kutsu epäonnistui "
                                  + error.getMessage()));
                } else {
                  result.setResult(ResponseEntity.status(HttpStatus.OK).body(hakukohteet));
                }
              });
    } catch (Exception e) {
      String msg = "Odottamaton virhe valintatietojen hakemisessa haulle " + hakuOid;
      LOG.error(msg, e);
      result.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg));
    }

    return result;
  }
}
