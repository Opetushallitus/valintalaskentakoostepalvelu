package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa;

import com.google.gson.Gson;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.HakukohdePair;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.dto.Jono;
import fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa.util.JonoUtil;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import io.reactivex.Observable;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller("JonotSijoittelussaProxyResource")
@Path("/proxy/jonotsijoittelussa")
@PreAuthorize("isAuthenticated()")
@Api(value = "/proxy/jonotsijoittelussa", description = "Tarkistaa onko jonot sijoittelussa")
public class JonotSijoittelussaProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(JonotSijoittelussaProxyResource.class);

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;
  @Autowired private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  @Autowired private ValintalaskentaAsyncResource valintalaskentaAsyncResource;

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GET
  @Path("/hakuOid/{hakuOid}")
  public void jonotSijoittelussa(
      @PathParam("hakuOid") String hakuOid, @Suspended final AsyncResponse asyncResponse) {
    asyncResponse.setTimeout(5L, TimeUnit.MINUTES);
    asyncResponse.setTimeoutHandler(this::handleTimeout);
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
                asyncResponse.resume(
                    Response.ok(
                            new Gson().toJson(laskennastaPuuttuvatHakukohdeOids),
                            MediaType.APPLICATION_JSON_TYPE)
                        .build()),
            exception -> {
              LOG.error(
                  "Jonojen tarkistus sijoittelussa epaonnistui haulle {}!", hakuOid, exception);
              asyncResponse.resume(Response.serverError().entity(exception).build());
            });
  }

  private void handleTimeout(AsyncResponse handler) {
    String explanation =
        String.format(
            "JonotSijoittelussaProxyResource -palvelukutsu on aikakatkaistu: /proxy/jonotsijoittelussa/hakuOid/{}");
    LOG.error(explanation);
    try {
      handler.resume(Response.serverError().entity(explanation).build());
    } catch (Throwable t) {
      // dont care! timeout callback is racing with real response. throws when real response is
      // faster.
    }
  }
}
