package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakemuksenVastaanottotila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller("ValintaTulosServiceProxyResource")
@Path("/proxy/valintatulosservice")
public class ValintaTulosServiceProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaTulosServiceProxyResource.class);

    @Autowired
    private TilaAsyncResource tilaResource;

    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    @GET
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    public void sijoittelunTulokset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler(asyncResponse1 -> {
                LOG.error("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
                asyncResponse1.resume(Response.serverError().entity("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu").build());
            });
            Observable<List<Valintatulos>> valintatuloksetObs = tilaResource.getValintatuloksetHakukohteelle(hakukohdeOid);
            Observable<List<HakemuksenVastaanottotila>> valinnantilatObs = valintaTulosServiceResource.getVastaanottotilatByHakemus(hakuOid, hakukohdeOid);
            Observable.zip(valintatuloksetObs, valinnantilatObs, (valintatulokset, valinnantilat) -> {
                    Map<String, HakemuksenVastaanottotila> vastaanottotilaByHakemus = valinnantilat.stream()
                            .collect(Collectors.toMap(HakemuksenVastaanottotila::getHakemusOid, Function.identity()));
                return valintatulokset.stream()
                        .filter(v ->
                            Optional.ofNullable(vastaanottotilaByHakemus.get(v.getHakemusOid())).filter(vj -> v.getValintatapajonoOid().equals(vj.getValintatapajonoOid())).isPresent()
                        )
                        .map(v -> {
                            v.setTila(vastaanottotilaByHakemus.get(v.getHakemusOid()).getVastaanottotila(),"");
                            return v;
                        }).collect(Collectors.toList());
            }).subscribe(done -> {
                asyncResponse.resume(Response
                        .ok(done)
                        .header("Content-Type", "application/json").build());
            }, error -> {
                LOG.error("Resurssien haku epäonnistui!", error);
                asyncResponse
                        .resume(Response.serverError()
                                .header("Content-Type","plain/text;charset=UTF8")
                                .entity("ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage())
                                .build());
            });
        } catch (Throwable t) {
            LOG.error("Virhe!", t);
            asyncResponse.register(Response.ok(t).build());
        }

        /*, h -> {

        }, t -> {

        }*/

    }

}
