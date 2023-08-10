package fi.vm.sade.valinta.kooste.valintakokeet;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.Observable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/valintakoe")
@Api(value = "/valintakoe", description = "Resurssi valintakoeosallistumistulosten hakemiseen.")
public class AktiivistenHakemustenValintakoeResource {
  private static final String VALINTAKAYTTAJA_ROLE =
      "hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ',"
          + "'ROLE_APP_VALINTOJENTOTEUTTAMINEN_READ_UPDATE','ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD')";
  private static final Logger LOG =
      LoggerFactory.getLogger(AktiivistenHakemustenValintakoeResource.class);

  private final ValintalaskentaValintakoeAsyncResource valintakoeAsyncResource;
  private final ApplicationAsyncResource applicationAsyncResource;
  private AtaruAsyncResource ataruAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;

  @Autowired
  public AktiivistenHakemustenValintakoeResource(
      ValintalaskentaValintakoeAsyncResource valintakoeAsyncResource,
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource) {
    this.valintakoeAsyncResource = valintakoeAsyncResource;
    this.applicationAsyncResource = applicationAsyncResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
  }

  @GetMapping(value = "hakutoive/{hakukohdeOid:.+}", produces = MediaType.APPLICATION_JSON)
  @PreAuthorize(VALINTAKAYTTAJA_ROLE)
  @ApiOperation(
      value =
          "Hakee valintakoeosallistumiset hakukohteelle OID:n perusteella, "
              + "filtteröiden pois passiiviset hakemukset",
      response = ValintakoeOsallistuminenDTO.class)
  public DeferredResult<ResponseEntity<List<ValintakoeOsallistuminenDTO>>>
      osallistumisetByHakutoive(
          @ApiParam(value = "Hakukohde OID", required = true) @PathVariable("hakukohdeOid")
              String hakukohdeOid) {

    DeferredResult<ResponseEntity<List<ValintakoeOsallistuminenDTO>>> result =
        new DeferredResult<>(30 * 1000l);

    Observable.fromFuture(valintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
        .flatMap(
            osallistumiset ->
                filtteroiPoisPassiivistenHakemustenOsallistumistiedot(osallistumiset, hakukohdeOid))
        .subscribe(
            osallistumiset ->
                result.setResult(ResponseEntity.status(HttpStatus.OK).body(osallistumiset)),
            exception -> {
              String message =
                  String.format(
                      "Virhe haettaessa valintakoeosallistumisia hakukohteelle %s", hakukohdeOid);
              LOG.error(message, exception);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(String.format("%s : %s", message, exception.getMessage())));
            });

    return result;
  }

  private Observable<List<ValintakoeOsallistuminenDTO>>
      filtteroiPoisPassiivistenHakemustenOsallistumistiedot(
          List<ValintakoeOsallistuminenDTO> osallistumiset, String hakukohdeOid) {
    List<String> kaikkiOsallistumistenHakemusOidit =
        osallistumiset.stream()
            .map(ValintakoeOsallistuminenDTO::getHakemusOid)
            .distinct()
            .collect(Collectors.toList());

    return Observable.fromFuture(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
        .flatMap(
            hakukohde -> Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakukohde.hakuOid)))
        .flatMap(
            haku -> {
              if (haku.isHakemuspalvelu()) {
                return Observable.fromFuture(
                        ataruAsyncResource.getApplicationsByOids(kaikkiOsallistumistenHakemusOidit))
                    .map(
                        hakemukset ->
                            hakemukset.stream()
                                .map(HakemusWrapper::getOid)
                                .collect(Collectors.toSet()));
              } else {
                return applicationAsyncResource
                    .getApplicationsByHakemusOids(kaikkiOsallistumistenHakemusOidit)
                    .map(
                        hakemukset ->
                            hakemukset.stream()
                                .map(HakemusWrapper::getOid)
                                .collect(Collectors.toSet()));
              }
            })
        .map(
            aktiivistenHakemusOidit ->
                osallistumiset.stream()
                    .filter(
                        o -> {
                          boolean onAktiivinen =
                              aktiivistenHakemusOidit.contains(o.getHakemusOid());
                          if (!onAktiivinen) {
                            LOG.warn(
                                String.format(
                                    "Hakemuksen %s valintakoeosallistuminen filtteröidään pois "
                                        + "haettaessa hakukohteen %s osallistumistietoja, "
                                        + "koska hakemusnumerolla ei löydy aktiivista hakemusta.",
                                    o.getHakemusOid(), hakukohdeOid));
                          }
                          return onAktiivinen;
                        })
                    .collect(Collectors.toList()));
  }
}
