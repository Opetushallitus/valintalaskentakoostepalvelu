package fi.vm.sade.valinta.kooste.proxy.resource.jonotsijoittelussa;

import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import com.google.gson.Gson;

@Controller("JonotSijoittelussaProxyResource")
@Path("/proxy/jonotsijoittelussa")
@PreAuthorize("isAuthenticated()")
@Api(value = "/proxy/jonotsijoittelussa", description = "Tarkistaa onko jonot sijoittelussa")
public class JonotSijoittelussaProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(JonotSijoittelussaProxyResource.class);

    @Autowired
    private TarjontaAsyncResource tarjontaAsyncResource;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private SijoitteluAsyncResource sijoitteluAsyncResource;
    @Autowired
    private SijoitteleAsyncResource sijoitteleAsyncResource;
    @Autowired
    private ValintalaskentaAsyncResource valintalaskentaAsyncResource;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/hakuOid/{hakuOid}")
    public void jonotSijoittelussa(
            @PathParam("hakuOid") String hakuOid,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(5L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(this::handleTimeout);


        Observable.combineLatest(
                valintalaskentaAsyncResource.jonotSijoitteluun(hakuOid),
                tarjontaAsyncResource.haeHaku(hakuOid)
                        .map(HakuV1RDTO::getHakukohdeOids)
                        .switchMap(valintaperusteetAsyncResource::haeValintatapajonotSijoittelulle),
                (jonotLaskennassa, jonotValintaperusteissa) -> {


                    List<String> laskennastaPuuttuvatHakukohdeOids = jonotValintaperusteissa.entrySet().stream().flatMap(entry -> {
                        final String hakukohdeOid = entry.getKey();
                        final Predicate<String> puuttuuLaskennasta = jonoOid ->
                                !jonotLaskennassa.getOrDefault(hakukohdeOid, emptyList()).contains(jonoOid);
                        final List<ValintatapajonoDTO> jonot = entry.getValue();
                        final List<String> puuttuvatJonotLaskennassa = jonot.stream().filter(ValintatapajonoDTO::getSiirretaanSijoitteluun)
                                .map(ValintatapajonoDTO::getOid)
                                .filter(puuttuuLaskennasta).collect(Collectors.toList());

                        return !puuttuvatJonotLaskennassa.isEmpty() ? Stream.of(hakukohdeOid) : Stream.empty();
                    }).collect(Collectors.toList());

                    return laskennastaPuuttuvatHakukohdeOids;
                }
        ).subscribe(
                laskennastaPuuttuvatHakukohdeOids ->
                        asyncResponse.resume(Response.ok(new Gson().toJson(laskennastaPuuttuvatHakukohdeOids),
                                MediaType.APPLICATION_JSON_TYPE).build())
                ,
                exception -> {
                    LOG.error("Jonojen tarkistus sijoittelussa epaonnistui haulle {}!", hakuOid, exception);
                    asyncResponse.resume(Response.serverError().entity(exception).build());
                });
    }

    private void handleTimeout(AsyncResponse handler) {
        String explanation =
                String.format("JonotSijoittelussaProxyResource -palvelukutsu on aikakatkaistu: /proxy/jonotsijoittelussa/hakuOid/{}");
        LOG.error(explanation);
        try {
            handler.resume(Response.serverError().entity(explanation).build());
        } catch (Throwable t) {
            // dont care! timeout callback is racing with real response. throws when real response is faster.
        }
    }
}
