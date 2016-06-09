package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import static fi.vm.sade.valinta.kooste.KoosteAudit.username;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableMap;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.HakukohteenValintatulosUpdateStatuses;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller("ValintaTulosServiceProxyResource")
@Path("/proxy/valintatulosservice")
public class ValintaTulosServiceProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaTulosServiceProxyResource.class);

    @Autowired
    private SijoitteluAsyncResource sijoitteluResource;

    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosServiceResource;

    @GET
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/ilmanhakijantilaa/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    public void valintatuloksetIlmanTilaaHakijalle(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @QueryParam("valintatapajonoOid") String valintatapajonoOid,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
                String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /ilmanhakijantilaa/haku/%s/hakukohde/%s",
                        hakuOid, hakukohdeOid));
        valintaTulosServiceResource.findValintatuloksetIlmanHakijanTilaa(hakuOid, hakukohdeOid)
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
                            LOG.error("Valintatulosten haku ilman valintatuloksen tilaa valinta-tulos-servicestä epäonnistui", error);
                            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage());
                        }
                );
    }

    @POST
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/myohastyneet/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    public void tietoSiitaEttaVastaanottoAikarajaOnMennyt(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            Set<String> hakemusOids,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
            String.format("ValintatulosserviceProxy -palvelukutsu vastaanottoaikarajojen menneeksi on aikakatkaistu: /myohastyneet/haku/%s/hakukohde/%s",
                hakuOid, hakukohdeOid));

        valintaTulosServiceResource.findVastaanottoAikarajaMennyt(hakuOid, hakukohdeOid, hakemusOids)
            .map(aikarajaMennytDtos -> Response.ok(aikarajaMennytDtos).type(MediaType.APPLICATION_JSON_TYPE).build())
            .subscribe(
                asyncResponse::resume,
                error -> {
                    LOG.error("Vastaanottoaikarajojen haku valinta-tulos-servicestä haun " + hakuOid + " hakukohteelle " + hakukohdeOid + " epäonnistui", error);
                    respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu epäonnistui virheeseen: " + error.getMessage());
                }
            );
    }

    @POST
    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @Path("/tilahakijalle/haku/{hakuOid}/hakukohde/{hakukohdeOid}/valintatapajono/{valintatapajonoOid}")
    @Consumes("application/json")
    public void vastaanottotilaHakijalle(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @PathParam("valintatapajonoOid") String valintatapajonoOid,
            Set<String> hakemusOids,
            @Suspended AsyncResponse asyncResponse) {
        setAsyncTimeout(asyncResponse,
            String.format("ValintatulosserviceProxy -palvelukutsu tila hakijalle -tiedon hakemiseksi on aikakatkaistu: /tilahakijalle/haku/%s/hakukohde/%s/valintatapajono/%s",
                hakuOid, hakukohdeOid, valintatapajonoOid));

        valintaTulosServiceResource.findTilahakijalle(hakuOid, hakukohdeOid, valintatapajonoOid, hakemusOids)
            .map(tilaHakijalleDtos -> Response.ok(tilaHakijalleDtos).type(MediaType.APPLICATION_JSON_TYPE).build())
            .subscribe(
                asyncResponse::resume,
                error -> {
                    LOG.error("Hakijan tilojen haku valinta-tulos-servicestä haun " + hakuOid + " hakukohteen " + hakukohdeOid + " valintatapajonolle " + valintatapajonoOid + " epäonnistui", error);
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

        sijoitteluResource.tarkistaEtteivatValintatuloksetMuuttuneetHakemisenJalkeen(valintatulokset).flatMap(staleReadCheckResponse -> {
            if (staleReadCheckResponse.statuses.isEmpty()) {
                List<VastaanottoRecordDTO> tallennettavat = null;
                try {
                    tallennettavat = createVastaanottoRecordsFrom(valintatulokset, username(), selite);
                } catch (Exception e) {
                    return Observable.error(e);
                }
                Observable<List<VastaanottoResultDTO>> vastaanottoTilojenTallennus = valintaTulosServiceResource.tallenna(tallennettavat);
                vastaanottoTilojenTallennus.doOnError(throwable -> LOG.error("Async call to valinta-tulos-service failed", throwable));
                return vastaanottoTilojenTallennus;
            } else {
                return Observable.error(new StaleReadCheckFailureException(staleReadCheckResponse));
            }
        }).flatMap(vastaanottoResponse -> {
            Stream<VastaanottoResultDTO> epaonnistuneet = vastaanottoResponse.stream().filter(VastaanottoResultDTO::isFailed);
            List<ValintatulosUpdateStatus> failedUpdateStatuses = epaonnistuneet.map(v -> {
                LOG.warn(v.toString());
                return new ValintatulosUpdateStatus(Response.Status.FORBIDDEN.getStatusCode(), v.getResult().getMessage(), null, v.getHakemusOid());
            }).collect(Collectors.toList());
            if (failedUpdateStatuses.isEmpty()) {
                valintatulokset.forEach(valintatulos -> valintatulos.setRead(new Date()));
                return sijoitteluResource.muutaHakemuksenTilaa(hakuOid, hakukohdeOid, valintatulokset, selite)
                        .doOnError(throwable -> LOG.error("Async call to sijoittelu-service failed", throwable));
            } else {
                return Observable.error(new VastaanottoUpdateFailuresException(failedUpdateStatuses));
            }
        }).subscribe(
                updateStatuses -> asyncResponse.resume(Response.ok(updateStatuses).build()),
                poikkeus -> {
                    if (poikkeus instanceof  StaleReadCheckFailureException) {
                        asyncResponse.resume(Response.serverError().entity(((StaleReadCheckFailureException) poikkeus).updateStatuses).build());
                    } else {
                        List<ValintatulosUpdateStatus> failedUpdateStatuses = poikkeus instanceof VastaanottoUpdateFailuresException ?
                                ((VastaanottoUpdateFailuresException) poikkeus).failedUpdateStatuses : Collections.emptyList();
                        asyncResponse.resume(Response.serverError().entity(new HakukohteenValintatulosUpdateStatuses(poikkeus.getMessage(), failedUpdateStatuses)).build());
                    }
                });
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/erillishaku/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    public void tuoErillishaunHakijat(@QueryParam("selite") String selite,
                                      @PathParam("hakuOid") String hakuOid,
                                      @PathParam("hakukohdeOid") String hakukohdeOid,
                                      List<Valintatulos> valintatulokset,
                                      @Suspended AsyncResponse asyncResponse) throws UnsupportedEncodingException {
        setAsyncTimeout(asyncResponse,
                String.format("ValintatulosserviceProxy -palvelukutsu on aikakatkaistu: /erillishaku/haku/%s/hakukohde/%s?selite=%s",
                        hakuOid, hakukohdeOid, selite));

        sijoitteluResource.tarkistaEtteivatValintatuloksetMuuttuneetHakemisenJalkeen(valintatulokset).flatMap(staleReadCheckResponse -> {
            if (staleReadCheckResponse.statuses.isEmpty()) {
                List<VastaanottoRecordDTO> tallennettavat = null;
                try {
                    tallennettavat = createVastaanottoRecordsFrom(valintatulokset, username(), selite);
                } catch (Exception e) {
                    return Observable.error(e);
                }
                Observable<List<VastaanottoResultDTO>> vastaanottoTilojenTallennus = valintaTulosServiceResource.tallenna(tallennettavat);
                vastaanottoTilojenTallennus.doOnError(throwable -> LOG.error("Async call to valinta-tulos-service failed", throwable));
                return vastaanottoTilojenTallennus;
            } else {
                return Observable.error(new StaleReadCheckFailureException(staleReadCheckResponse));
            }
        }).flatMap(vastaanottoResponse -> {
            Stream<VastaanottoResultDTO> epaonnistuneet = vastaanottoResponse.stream().filter(VastaanottoResultDTO::isFailed);
            List<ValintatulosUpdateStatus> failedUpdateStatuses = epaonnistuneet.map(v -> {
                LOG.warn(v.toString());
                return new ValintatulosUpdateStatus(Response.Status.FORBIDDEN.getStatusCode(), v.getResult().getMessage(), null, v.getHakemusOid());
            }).collect(Collectors.toList());
            if (failedUpdateStatuses.isEmpty()) {
                valintatulokset.forEach(valintatulos -> valintatulos.setRead(new Date()));
                return sijoitteluResource.muutaErillishaunHakemuksenTilaa(hakuOid, hakukohdeOid, valintatulokset)
                        .doOnError(throwable -> LOG.error("Async call to sijoittelu-service failed", throwable));
            } else {
                return Observable.error(new VastaanottoUpdateFailuresException(failedUpdateStatuses));
            }
        }).subscribe(
                updateStatuses -> asyncResponse.resume(Response.ok(updateStatuses).build()),
                poikkeus -> {
                    if (poikkeus instanceof  StaleReadCheckFailureException) {
                        asyncResponse.resume(Response.serverError().entity(((StaleReadCheckFailureException) poikkeus).updateStatuses).build());
                    } else {
                        List<ValintatulosUpdateStatus> failedUpdateStatuses = poikkeus instanceof VastaanottoUpdateFailuresException ?
                            ((VastaanottoUpdateFailuresException) poikkeus).failedUpdateStatuses : Collections.emptyList();
                        asyncResponse.resume(Response.serverError().entity(new HakukohteenValintatulosUpdateStatuses(poikkeus.getMessage(), failedUpdateStatuses)).build());
                    }
                });
    }

    private List<VastaanottoRecordDTO> createVastaanottoRecordsFrom(List<Valintatulos> valintatulokset, String muokkaaja, String selite) {
        return valintatulokset.stream().map(v -> VastaanottoRecordDTO.of(v, muokkaaja, selite)).collect(Collectors.toList());
    }

    private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
        response.setTimeout(5L, TimeUnit.MINUTES);
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

    public static class ValintaTulosServiceSerializersModule extends SimpleModule {
        public ValintaTulosServiceSerializersModule() {
            super(ValintaTulosServiceSerializersModule.class.getSimpleName());
            addSerializer(DateTime.class, new DateTimeJsonSerializer());
        }
    }

    private static class DateTimeJsonSerializer extends JsonSerializer<DateTime> {
        @Override
        public void serialize(DateTime dateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            String timestampAsString = ValintaTulosServiceAsyncResource.valintaTulosServiceCompatibleFormatter.print(dateTime);
            jsonGenerator.writeString(timestampAsString);
        }
    }
}
