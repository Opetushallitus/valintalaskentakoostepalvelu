package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import com.google.common.collect.ImmutableMap;

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakemuksenVastaanottotila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.KoosteAudit.username;

@Controller("ValintaTulosServiceProxyResource")
@Path("/proxy/valintatulosservice")
public class ValintaTulosServiceProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaTulosServiceProxyResource.class);

    @Autowired
    private TilaAsyncResource tilaResource;

    @Autowired
    private SijoitteluAsyncResource sijoitteluResource;

    @Autowired
    private VastaanottoService vastaanottoService;

    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    @GET
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    public void sijoittelunTulokset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintatapajonoOid") String valintatapajonoOid,
            @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler(asyncResponse1 -> {
                LOG.error("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
                asyncResponse1.resume(Response.serverError().entity("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu").build());
            });
            Observable<List<Valintatulos>> valintatuloksetObs = valintatapajonoOid == null ?
                tilaResource.getValintatuloksetHakukohteelle(hakukohdeOid) :
                tilaResource.getValintatuloksetValintatapajonolle(hakukohdeOid, valintatapajonoOid);
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

        setAsyncTimeout(asyncResponse, String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/%s/sijoitteluajo/%s/hakukohde/%s", hakuOid, sijoitteluAjo, hakukohdeOid));

        sijoitteluResource.getLatestHakukohdeBySijoittelu(hakuOid, sijoitteluAjo, hakukohdeOid, h -> {
            asyncResponse.resume(Response
                    .ok(h)
                    .header("Content-Type", "application/json").build());
        }, t -> {
            String message = String.format("Error getting sijoittelunTulokset for hakukohde %s", hakukohdeOid);
            LOG.error(message, t);
            respondWithError(asyncResponse, message);
        });

        }*/

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    public void muutaHakemustenTilaa(@PathParam("hakuOid") String hakuOid,
                                     @PathParam("hakukohdeOid") String hakukohdeOid,
                                     List<Valintatulos> valintatulokset,
                                     @QueryParam("selite") String selite,
                                     @Suspended AsyncResponse asyncResponse) throws UnsupportedEncodingException {
        setAsyncTimeout(asyncResponse, String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/%s/hakukohde/%s?selite=%s", hakuOid, hakukohdeOid, selite));

        AtomicInteger counter = new AtomicInteger(2);
        Supplier<Void> mergeSupplier = () -> {
            if (counter.decrementAndGet() == 0) {
                asyncResponse.resume(Response.ok().build());
            }
            return null;
        };

        String muokkaaja = username();
        vastaanottoService.tallenna(valintatulokset, muokkaaja).subscribe(integer -> mergeSupplier.get(), throwable -> {
            LOG.error("Async call to valinta-tulos-service failed", throwable);
            respondWithError(asyncResponse, throwable.getMessage());
        });
        sijoitteluResource.muutaHakemuksenTilaa(hakuOid, hakukohdeOid, valintatulokset, selite).subscribe(aVoid -> mergeSupplier.get(), throwable -> {
            LOG.error("Async call to sijoittelu-service failed", throwable);
            respondWithError(asyncResponse, throwable.getMessage());
        });
    }

    private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
        response.setTimeout(2L, TimeUnit.MINUTES);
        response.setTimeoutHandler(asyncResponse -> {
            LOG.error(timeoutMessage);
            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu on aikakatkaistu");
        });
    }

    private void respondWithError(AsyncResponse asyncResponse, String error) {
        asyncResponse.resume(Response.serverError().entity(ImmutableMap.of("error", error)).build());
    }

    @GET
    @Path("/hakemus/{hakemusOid}/haku/{hakuOid}/hakukohde/{hakukohdeOid}/valintatapajono/{valintatapajonoOid}")
    @Consumes("application/json")
    public void hakemuksenSijoittelunTulos(
            @PathParam("hakemusOid") String hakemusOid,
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @PathParam("valintatapajonoOid") String valintatapajonoOid,
            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(asyncResponse1 -> {
            LOG.error("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /hakemus/{}/haku/{}/hakukohde/{}/valintatapajono/{}",
                hakemusOid, hakuOid, hakukohdeOid, valintatapajonoOid);
            asyncResponse1.resume(Response.serverError().entity("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu").build());
        });
        Observable<Valintatulos> valintatulosObs = tilaResource.getHakemuksenSijoittelunTulos(hakemusOid, hakuOid, hakukohdeOid, valintatapajonoOid);
        Observable<ValintaTulosServiceDto> hakemuksenVastaanottotiedotObs = valintaTulosServiceResource.getHakemuksenValintatulos(hakuOid, hakemusOid);
        Observable.zip(valintatulosObs, hakemuksenVastaanottotiedotObs, (valintatulos, hakemuksenVastaanottotiedot) -> {
            hakemuksenVastaanottotiedot.getHakutoiveet().stream().filter(h -> h.getHakukohdeOid().equals(hakukohdeOid)).forEach(hakutoive ->
                valintatulos.setTila(ValintatuloksenTila.valueOf(hakutoive.getVastaanottotila().name()), ""));
            return valintatulos;
        }).subscribe(
            done -> asyncResponse.resume(Response.ok(done).header("Content-Type", "application/json").build()),
            error -> {
                LOG.error("Resurssien haku epäonnistui!", error);
                asyncResponse
                    .resume(Response.serverError()
                        .header("Content-Type", "plain/text;charset=UTF8")
                        .entity("ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage())
                        .build());
        });
    }

    @GET
    @Path("/hakemus/{hakemusOid}/haku/{hakuOid}")
    @Consumes("application/json")
    public void kaikkiHakemuksenSijoittelunTulokset(
            @PathParam("hakemusOid") String hakemusOid,
            @PathParam("hakuOid") String hakuOid,
            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(asyncResponse1 -> {
            LOG.error("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /hakemus/{}/haku/{}", hakemusOid, hakuOid);
            asyncResponse1.resume(Response.serverError().entity("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu").build());
        });
        Observable<List<Valintatulos>> valintatulosObs = tilaResource.getHakemuksenTulokset(hakemusOid);
        Observable<ValintaTulosServiceDto> hakemuksenVastaanottotiedotObs = valintaTulosServiceResource.getHakemuksenValintatulos(hakuOid, hakemusOid);
        Observable.zip(valintatulosObs, hakemuksenVastaanottotiedotObs, (valintatulokset, hakemuksenVastaanottotiedot) -> {
            valintatulokset.forEach(valintatulos ->
                hakemuksenVastaanottotiedot.getHakutoiveet().stream().filter(ht -> ht.getHakukohdeOid().equals(valintatulos.getHakukohdeOid())).forEach(hakutoive ->
                    valintatulos.setTila(ValintatuloksenTila.valueOf(hakutoive.getVastaanottotila().name()), "")));
            return valintatulokset;
        }).subscribe(
            done -> asyncResponse.resume(Response.ok(done).header("Content-Type", "application/json").build()),
            error -> {
                LOG.error("Resurssien haku epäonnistui!", error);
                asyncResponse
                    .resume(Response.serverError()
                        .header("Content-Type", "plain/text;charset=UTF8")
                        .entity("ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage())
                        .build());
        });
    }
}
