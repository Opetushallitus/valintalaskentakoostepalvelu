package fi.vm.sade.valinta.kooste.proxy.resource.viestintapalvelu;

import static io.reactivex.Observable.combineLatest;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.dto.LetterBatchCountDto;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController("ViestintapalveluProxyResource")
@RequestMapping("/resources/proxy/viestintapalvelu")
public class ViestintapalveluProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(ViestintapalveluProxyResource.class);

  private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
  private final RyhmasahkopostiAsyncResource ryhmasahkopostiAsyncResource;

  @Autowired
  public ViestintapalveluProxyResource(
      ViestintapalveluAsyncResource viestintapalveluAsyncResource,
      RyhmasahkopostiAsyncResource ryhmasahkopostiAsyncResource) {
    this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    this.ryhmasahkopostiAsyncResource = ryhmasahkopostiAsyncResource;
  }

  @PostMapping(
      value = "/publish/haku/{hakuOid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
  public DeferredResult<ResponseEntity<String>> julkaiseKirjeetOmillaSivuilla(
      @PathVariable("hakuOid") String hakuOid,
      @RequestParam(value = "asiointikieli", required = false) String asiointikieli,
      @RequestParam(value = "kirjeenTyyppi", required = false) String kirjeenTyyppi) {
    DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(10 * 60 * 1000l);

    viestintapalveluAsyncResource
        .haeKirjelahetysJulkaistavaksi(hakuOid, kirjeenTyyppi, asiointikieli)
        .flatMap(
            batchIdOptional -> {
              if (batchIdOptional.isPresent()) {
                return viestintapalveluAsyncResource.julkaiseKirjelahetys(batchIdOptional.get());
              } else {
                throw new RuntimeException("Kirjelähetyksen ID:tä ei löytynyt.");
              }
            })
        .subscribe(
            batchIdOptional ->
                result.setResult(
                    ResponseEntity.status(HttpStatus.OK).body(batchIdOptional.get().toString())),
            throwable ->
                result.setErrorResult(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(throwable.getMessage())));

    return result;
  }

  private LetterBatchCountDto haeRyhmasahkopostiId(LetterBatchCountDto countDto) {
    if (countDto.letterBatchId == null) {
      return countDto;
    }
    Optional<Long> groupEmailId =
        ryhmasahkopostiAsyncResource
            .haeRyhmasahkopostiIdByLetterObservable(countDto.letterBatchId)
            .timeout(5, MINUTES)
            .blockingFirst();
    return groupEmailId
        .map(
            aLong ->
                new LetterBatchCountDto(
                    countDto.letterBatchId,
                    countDto.letterTotalCount,
                    countDto.letterReadyCount,
                    countDto.letterErrorCount,
                    countDto.letterPublishedCount,
                    countDto.readyForPublish,
                    false,
                    aLong))
        .orElse(countDto);
  }

  @GetMapping(value = "/count/haku/{hakuOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
  public DeferredResult<ResponseEntity<ImmutableMap<String, ImmutableMap<String, Object>>>>
      countLettersForHaku(@PathVariable("hakuOid") String hakuOid) {
    DeferredResult<ResponseEntity<ImmutableMap<String, ImmutableMap<String, Object>>>> result =
        new DeferredResult<>(5 * 60 * 1000l);
    result.onTimeout(
        () -> {
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body(
                      String.format(
                          "ViestintapalveluProxyResource -palvelukutsu on aikakatkaistu: /viestintapalvelu/haku/%s/tyyppi/--/kieli/--",
                          hakuOid)));
        });

    Observable<LetterBatchCountDto> hyvaksymiskirjeFi =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "fi")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> hyvaksymiskirjeSv =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "sv")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> hyvaksymiskirjeEn =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje", "en")
            .map(this::haeRyhmasahkopostiId);

    Observable<LetterBatchCountDto> hyvaksymiskirjeHuoltajilleFi =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje_huoltajille", "fi")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> hyvaksymiskirjeHuoltajilleSv =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje_huoltajille", "sv")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> hyvaksymiskirjeHuoltajilleEn =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "hyvaksymiskirje_huoltajille", "en")
            .map(this::haeRyhmasahkopostiId);

    Observable<LetterBatchCountDto> jalkiohjauskirjeFi =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "fi")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> jalkiohjauskirjeSv =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "sv")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> jalkiohjauskirjeEn =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje", "en")
            .map(this::haeRyhmasahkopostiId);

    Observable<LetterBatchCountDto> jalkiohjauskirjeHuoltajilleFi =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje_huoltajille", "fi")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> jalkiohjauskirjeHuoltajilleSv =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje_huoltajille", "sv")
            .map(this::haeRyhmasahkopostiId);
    Observable<LetterBatchCountDto> jalkiohjauskirjeHuoltajilleEn =
        viestintapalveluAsyncResource
            .haeTuloskirjeenMuodostuksenTilanne(hakuOid, "jalkiohjauskirje_huoltajille", "en")
            .map(this::haeRyhmasahkopostiId);

    List<Observable<LetterBatchCountDto>> observables =
        Arrays.asList(
            hyvaksymiskirjeFi,
            hyvaksymiskirjeSv,
            hyvaksymiskirjeEn,
            hyvaksymiskirjeHuoltajilleFi,
            hyvaksymiskirjeHuoltajilleSv,
            hyvaksymiskirjeHuoltajilleEn,
            jalkiohjauskirjeFi,
            jalkiohjauskirjeSv,
            jalkiohjauskirjeEn,
            jalkiohjauskirjeHuoltajilleFi,
            jalkiohjauskirjeHuoltajilleSv,
            jalkiohjauskirjeHuoltajilleEn);
    combineLatest(
            observables,
            (args) ->
                ImmutableMap.of(
                    "hyvaksymiskirje",
                    ImmutableMap.of("fi", args[0], "sv", args[1], "en", args[2]),
                    "hyvaksymiskirje_huoltajille",
                    ImmutableMap.of("fi", args[3], "sv", args[4], "en", args[5]),
                    "jalkiohjauskirje",
                    ImmutableMap.of("fi", args[6], "sv", args[7], "en", args[8]),
                    "jalkiohjauskirje_huoltajille",
                    ImmutableMap.of("fi", args[9], "sv", args[10], "en", args[11])))
        .subscribe(
            letterCount -> result.setResult(ResponseEntity.status(HttpStatus.OK).body(letterCount)),
            error -> {
              LOG.error("Viestintäpalvelukutsu epäonnistui!", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
            });

    return result;
  }
}
