package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    @GET
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    public void sijoittelunTulokset(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintatapajonoOid") String valintatapajonoOid,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/%s/hakukohde/%s",
                        hakuOid, hakukohdeOid));
        valintaTulosServiceResource.findValintatulokset(hakuOid, hakukohdeOid)
                .map(valintatulokset -> {
                    if (StringUtils.isBlank(valintatapajonoOid)) {
                        return valintatulokset;
                    } else {
                        return valintatulokset.stream()
                                .filter(v -> valintatapajonoOid.equals(v.getValintatapajonoOid()))
                                .collect(Collectors.toList());
                    }
                })
                .map(valintatulokset -> Response.ok(valintatulokset).type(MediaType.APPLICATION_JSON_TYPE).build())
                .subscribe(
                        asyncResponse::resume,
                        error -> {
                            LOG.error("Valintatulosten haku valinta-tulos-servicestä epäonnistui", error);
                            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage());
                        }
                );
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    public void muutaHakemustenTilaa(@PathParam("hakuOid") String hakuOid,
                                     @PathParam("hakukohdeOid") String hakukohdeOid,
                                     List<Valintatulos> valintatulokset,
                                     @QueryParam("selite") String selite,
                                     @Suspended AsyncResponse asyncResponse) throws UnsupportedEncodingException {
        setAsyncTimeout(asyncResponse,
                String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /haku/%s/hakukohde/%s?selite=%s",
                        hakuOid, hakukohdeOid, selite));

        Observable<List<VastaanottoResultDTO>> vastaanottoTilojenTallennus = valintaTulosServiceResource.tallenna(createVastaanottoRecordsFrom(valintatulokset, username(), selite));
        vastaanottoTilojenTallennus.doOnError(throwable -> LOG.error("Async call to valinta-tulos-service failed", throwable));
        Observable<HakukohteenValintatulosUpdateStatuses> tilojenTallennusSijoitteluun = sijoitteluResource.muutaHakemuksenTilaa(hakuOid, hakukohdeOid, valintatulokset, selite).doOnError(
            throwable -> LOG.error("Async call to sijoittelu-service failed", throwable));
        vastaanottoTilojenTallennus.flatMap(vastaanottoResponse -> {
            Stream<VastaanottoResultDTO> epaonnistuneet = vastaanottoResponse.stream().filter(VastaanottoResultDTO::isFailed);
            List<ValintatulosUpdateStatus> failedUpdateStatuses = epaonnistuneet.map(v -> {
                LOG.warn(v.toString());
                return new ValintatulosUpdateStatus(Response.Status.FORBIDDEN.getStatusCode(), v.getResult().getMessage(), null, v.getHakemusOid());
            }).collect(Collectors.toList());
            if (failedUpdateStatuses.isEmpty()) {
                return tilojenTallennusSijoitteluun;
            } else {
              return Observable.error(new VastaanottoUpdateFailuresException(failedUpdateStatuses));
            }
        }).subscribe(
            updateStatuses -> asyncResponse.resume(Response.ok(updateStatuses).build()),
            poikkeus -> {
                List<ValintatulosUpdateStatus> failedUpdateStatuses = poikkeus instanceof VastaanottoUpdateFailuresException ?
                    ((VastaanottoUpdateFailuresException) poikkeus).failedUpdateStatuses : Collections.emptyList();
                asyncResponse.resume(Response.serverError().entity(new HakukohteenValintatulosUpdateStatuses(poikkeus.getMessage(), failedUpdateStatuses)).build());
            });
    }

    private List<VastaanottoRecordDTO> createVastaanottoRecordsFrom(List<Valintatulos> valintatulokset, String muokkaaja, String selite) {
        return valintatulokset.stream().map(v -> {
            VastaanottoRecordDTO dto = new VastaanottoRecordDTO();
            dto.setHakemusOid(v.getHakemusOid());
            dto.setHakukohdeOid(v.getHakukohdeOid());
            dto.setHakuOid(v.getHakuOid());
            dto.setHenkiloOid(v.getHakijaOid());
            dto.setIlmoittaja(muokkaaja);
            dto.setSelite(selite);
            dto.setTila(v.getTila());
            return dto;
        }).collect(Collectors.toList());
    }

    private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
        response.setTimeout(2L, TimeUnit.MINUTES);
        response.setTimeoutHandler(asyncResponse -> {
            LOG.error(timeoutMessage);
            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu on aikakatkaistu");
        });
    }

    private void respondWithError(AsyncResponse asyncResponse, String error) {
        asyncResponse.resume(Response.serverError()
                .entity(ImmutableMap.of("error", error))
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build());
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @GET
    @Path("/hakemus/{hakemusOid}/haku/{hakuOid}/hakukohde/{hakukohdeOid}/valintatapajono/{valintatapajonoOid}")
    @Consumes("application/json")
    public void hakemuksenSijoittelunTulos(
            @PathParam("hakemusOid") String hakemusOid,
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @PathParam("valintatapajonoOid") String valintatapajonoOid,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /hakemus/%s/haku/%s/hakukohde/%s/valintatapajono/%s",
                        hakemusOid, hakuOid, hakukohdeOid, valintatapajonoOid));
        valintaTulosServiceResource.findValintatuloksetByHakemus(hakuOid, hakemusOid)
                .map(valintatulokset -> {
                    if (StringUtils.isBlank(valintatapajonoOid)) {
                        return valintatulokset;
                    } else {
                        return valintatulokset.stream()
                                .filter(v -> valintatapajonoOid.equals(v.getValintatapajonoOid()))
                                .collect(Collectors.toList());
                    }
                })
                .map(valintatulokset -> Response.ok(valintatulokset).type(MediaType.APPLICATION_JSON_TYPE).build())
                .subscribe(
                        asyncResponse::resume,
                        error -> {
                            LOG.error("Valintatulosten haku valinta-tulos-servicestä epäonnistui", error);
                            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage());
                        }
                );
    }
    
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @GET
    @Path("/hakemus/{hakemusOid}/haku/{hakuOid}")
    @Consumes("application/json")
    public void kaikkiHakemuksenSijoittelunTulokset(
            @PathParam("hakemusOid") String hakemusOid,
            @PathParam("hakuOid") String hakuOid,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /hakemus/%s/haku/%s",
                        hakemusOid, hakuOid));
        valintaTulosServiceResource.findValintatuloksetByHakemus(hakuOid, hakemusOid)
                .map(valintatulokset -> Response.ok(valintatulokset).type(MediaType.APPLICATION_JSON_TYPE).build())
                .subscribe(
                        asyncResponse::resume,
                        error -> {
                            LOG.error("Valintatulosten haku valinta-tulos-servicestä epäonnistui", error);
                            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage());
                        }
                );
    }
}
