package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa;

import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.HakukohdePair;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.Jono;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.util.JonoUtil;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import io.reactivex.Observable;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

@RestController("JonotSijoittelussaProxyResource")
@RequestMapping("/resources/proxy/jonotsijoittelussa")
@PreAuthorize("isAuthenticated()")
@Tag(name = "/proxy/jonotsijoittelussa", description = "Tarkistaa onko jonot sijoittelussa")
public class JonotSijoittelussaProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(JonotSijoittelussaProxyResource.class);

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;
  @Autowired private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  @Autowired private ValintalaskentaAsyncResource valintalaskentaAsyncResource;

  @GetMapping(value = "/hakuOid/{hakuOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Set<String>>> jonotSijoittelussa(
      @PathVariable("hakuOid") String hakuOid) {

    DeferredResult<ResponseEntity<Set<String>>> result = new DeferredResult<>(5 * 60 * 1000l);
    result.onTimeout(
        () -> {
          String explanation =
              String.format(
                  "JonotSijoittelussaProxyResource -palvelukutsu on aikakatkaistu: /proxy/jonotsijoittelussa/hakuOid/{}");
          LOG.error(explanation);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(explanation));
        });

    final Observable<List<JonoDto>> laskennanJonot =
        valintalaskentaAsyncResource.jonotSijoitteluun(hakuOid);
    final Observable<Map<String, List<ValintatapajonoDTO>>> valintaperusteidenJonot =
        Observable.fromFuture(tarjontaAsyncResource.haunHakukohteet(hakuOid))
            .switchMap(valintaperusteetAsyncResource::haeValintatapajonotSijoittelulle);
    Observable.combineLatest(
            laskennanJonot,
            valintaperusteidenJonot,
            (jonotLaskennassa, jonotValintaperusteissa) -> {
              final List<Jono> fromValintaperusteet =
                  jonotValintaperusteissa.entrySet().stream()
                      .flatMap(
                          e ->
                              e.getValue().stream()
                                  .map(
                                      jono ->
                                          new Jono(
                                              e.getKey(),
                                              jono.getOid(),
                                              Optional.empty(),
                                              jono.getSiirretaanSijoitteluun(),
                                              Optional.ofNullable(jono.getAktiivinen()))))
                      .collect(Collectors.toList());
              final List<Jono> fromLaskenta =
                  jonotLaskennassa.stream()
                      .map(
                          j ->
                              new Jono(
                                  j.getHakukohdeOid(),
                                  j.getValintatapajonoOid(),
                                  Optional.of(j.getValmisSijoiteltavaksi()),
                                  j.getSiirretaanSijoitteluun(),
                                  Optional.empty()))
                      .collect(Collectors.toList());
              List<HakukohdePair> hakukohdePairs =
                  JonoUtil.pairHakukohteet(fromLaskenta, fromValintaperusteet);
              return JonoUtil.puutteellisetHakukohteet(hakukohdePairs);
            })
        .subscribe(
            laskennastaPuuttuvatHakukohdeOids ->
                result.setResult(
                    ResponseEntity.status(HttpStatus.OK).body(laskennastaPuuttuvatHakukohdeOids)),
            exception -> {
              LOG.error(
                  "Jonojen tarkistus sijoittelussa epaonnistui haulle {}!", hakuOid, exception);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(exception.getMessage()));
            });

    return result;
  }
}
