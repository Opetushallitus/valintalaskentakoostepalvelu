package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import static fi.vm.sade.valinta.kooste.util.ResponseUtil.respondWithError;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("ValintaTulosServiceProxyResource")
@Path("/proxy/valintatulosservice")
public class ValintaTulosServiceProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaTulosServiceProxyResource.class);

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

    private void setAsyncTimeout(AsyncResponse response, String timeoutMessage) {
        response.setTimeout(5L, TimeUnit.MINUTES);
        response.setTimeoutHandler(asyncResponse -> {
            LOG.error(timeoutMessage);
            respondWithError(asyncResponse, "ValintatulosserviceProxy -palvelukutsu on aikakatkaistu");
        });
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
