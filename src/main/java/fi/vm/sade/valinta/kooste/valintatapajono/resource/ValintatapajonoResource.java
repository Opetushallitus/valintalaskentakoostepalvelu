package fi.vm.sade.valinta.kooste.valintatapajono.resource;

import static java.util.concurrent.TimeUnit.MINUTES;

import fi.vm.sade.auditlog.User;
import fi.vm.sade.javautils.opintopolku_spring_security.Authorizer;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.ValintatapajonoRivit;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.valintatapajono.service.ValintatapajonoTuontiService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.poi.util.IOUtils;
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
 * @Autowired(required = false) Camelin pois refaktorointi
 */
///
// valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
///
// haku-app/applications/listfull?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid={hakukohdeOid}&asId={hakuOid}
///
// valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
@RestController
@RequestMapping("/resources/valintatapajonolaskenta")
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "/valintatapajonolaskenta",
    description = "Valintatapajonon tuonti ja vienti taulukkolaskentaan")
public class ValintatapajonoResource {
  public static final String ROLE_TULOSTENTUONTI =
      "ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI";
  private final Logger LOG = LoggerFactory.getLogger(ValintatapajonoResource.class);

  @Autowired private Authorizer authorizer;
  @Autowired private ValintatapajonoTuontiService valintatapajonoTuontiService;

  @Autowired(required = false)
  private ValintatapajonoVientiRoute valintatapajonoVienti;

  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
  @Autowired private TarjontaAsyncResource tarjontaResource;

  @PostMapping(value = "/vienti", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @Operation(
      summary = "Valintatapajonon vienti taulukkolaskentaan",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = ProsessiId.class)))
      })
  public ProsessiId vienti(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "valintatapajonoOid", required = false) String valintatapajonoOid) {
    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
    DokumenttiProsessi prosessi =
        new DokumenttiProsessi("Valintatapajono", "vienti", hakuOid, Arrays.asList(hakukohdeOid));
    valintatapajonoVienti.vie(prosessi, hakuOid, hakukohdeOid, valintatapajonoOid);
    dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
    return prosessi.toProsessiId();
  }

  @PostMapping(
      value = "/tuonti",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @Operation(
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
      summary = "Valintatapajonon tuonti taulukkolaskennasta",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = String.class)))
      })
  public DeferredResult<ResponseEntity<String>> tuonti(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "valintatapajonoOid", required = false) String valintatapajonoOid,
      InputStream file,
      HttpServletRequest request) {
    final User user = AuditLog.getUser(request);
    DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(5 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "Valintatapajonon tuonti on aikakatkaistu: /haku/{}/hakukohde/{}",
              hakuOid,
              hakukohdeOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Valintatapajonon tuonti on aikakatkaistu"));
        });

    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
    final ByteArrayOutputStream bytes;
    try {
      IOUtils.copy(file, bytes = new ByteArrayOutputStream());
      IOUtils.closeQuietly(file);
      valintatapajonoTuontiService.tuo(
          (valinnanvaiheet, hakemukset) -> {
            ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
            try {
              ValintatapajonoExcel valintatapajonoExcel =
                  new ValintatapajonoExcel(
                      hakuOid,
                      hakukohdeOid,
                      valintatapajonoOid,
                      "",
                      "",
                      valinnanvaiheet,
                      hakemukset,
                      Arrays.asList(listaus));
              valintatapajonoExcel
                  .getExcel()
                  .tuoXlsx(new ByteArrayInputStream(bytes.toByteArray()));
            } catch (Throwable t) {
              throw new RuntimeException(t);
            }
            return listaus.getRivit();
          },
          hakuOid,
          hakukohdeOid,
          tarjoajaOid,
          valintatapajonoOid,
          result,
          user);
    } catch (Throwable t) {
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Valintatapajonon tuonti epäonnistui tiedoston lukemiseen"));
    }

    return result;
  }

  @PostMapping(
      value = "/tuonti/json",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
  @Operation(
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
      summary = "Valintatapajonon tuonti jsonista",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = List.class)))
      })
  public DeferredResult<ResponseEntity<String>> tuonti(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "valintatapajonoOid", required = false) String valintatapajonoOid,
      @RequestBody ValintatapajonoRivit rivit,
      HttpServletRequest request) {
    final User user = AuditLog.getUser(request);
    DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(5 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "Valintatapajonon tuonti on aikakatkaistu: /haku/{}/hakukohde/{}",
              hakuOid,
              hakukohdeOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Valintatapajonon tuonti on aikakatkaistu"));
        });

    String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
    authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
    valintatapajonoTuontiService.tuo(
        (valinnanvaiheet, hakemukset) -> rivit.getRivit(),
        hakuOid,
        hakukohdeOid,
        tarjoajaOid,
        valintatapajonoOid,
        result,
        user);

    return result;
  }

  private String findTarjoajaOid(String hakukohdeOid) {
    try {
      return tarjontaResource
          .haeHakukohde(hakukohdeOid)
          .get(1, MINUTES)
          .tarjoajaOids
          .iterator()
          .next();
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }
}
