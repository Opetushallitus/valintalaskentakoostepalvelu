package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import static com.google.common.collect.ImmutableMap.of;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Lukuvuosimaksu;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.LukuvuosimaksuMuutos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Muutoshistoria;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValinnantulosRequest;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.TilaHakijalleDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ValintaTulosServiceAsyncResourceImpl extends UrlConfiguredResource implements ValintaTulosServiceAsyncResource {
    public ValintaTulosServiceAsyncResourceImpl() {
        super(TimeUnit.MINUTES.toMillis(30));
    }

    @Override
    protected Gson createGson() {
        return DateDeserializer.gsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeJsonSerializer())
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeJsonDeserializer())
                .registerTypeAdapter(DateTime.class, new VtsDateTimeJsonDeserializer())
                .registerTypeAdapter(DateTime.class, new VtsDateTimeJsonSerializer())
                .create();
    }

    @Override
    public Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid) {
        return getAsObservableLazily(getUrl("valinta-tulos-service.haku.hakuoid", hakuOid),
                new GenericType<List<ValintaTulosServiceDto>>() {}.getType());
    }
    @Override
    public Observable<List<Muutoshistoria>> getMuutoshistoria(String hakemusOid, String valintatapajonoOid) {
        return getAsObservableLazily(
                getUrl("valinta-tulos-service.muutoshistoria", hakemusOid, valintatapajonoOid),
                new GenericType<List<Muutoshistoria>>(){}.getType());
    }

    @Override
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return getStringAsObservableLazily(
                getUrl("valinta-tulos-service.haku.hakuoid.hakemus", hakuOid, hakemusOid));
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikalliset(String hakuOid, String hakukohdeOid) {
        return getAsObservableLazily(getUrl("valinta-tulos-service.haku.hakukohde.hyvaksytyt", hakuOid, hakukohdeOid),
                new GenericType<HakijaPaginationObject>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikalliset(String hakuOid) {
        return getAsObservableLazilyWithInputStream(getUrl("valinta-tulos-service.haku.hyvaksytyt", hakuOid),
                new GenericType<HakijaPaginationObject>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakijaDTO> getHakijaByHakemus(String hakuOid, String hakemusOid) {
        return getAsObservableLazily(
                getUrl("valinta-tulos-service.haku.sijoitteluajo.latest.hakemus", hakuOid, hakemusOid),
                new TypeToken<fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid) {
        return getAsObservableLazily(
                getUrl("valinta-tulos-service.haku.hakukohde.hakijat", hakuOid, hakukohdeOid),
                new GenericType<HakijaPaginationObject>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid) {
        return getAsObservableLazilyWithInputStream(
                getUrl("valinta-tulos-service.haku.ilmanhyvaksyntaa", hakuOid),
                new GenericType<HakijaPaginationObject>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<List<Valintatulos>> findValintatulokset(String hakuOid, String hakukohdeOid) {
        return getAsObservableLazily(getUrl("valinta-tulos-service.virkailija.valintatulos.haku.hakukohde", hakuOid, hakukohdeOid), new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<Lukuvuosimaksu>> fetchLukuvuosimaksut(String hakukohdeOid, AuditSession session) {
        return postAsObservableLazily(getUrl("valinta-tulos-service.virkailija.valintatulos.lukuvuosimaksu", "read", hakukohdeOid), new GenericType<List<Lukuvuosimaksu>>() {}.getType(),
                Entity.json(of("auditSession", session)));
    }

    @Override
    public Observable<String> saveLukuvuosimaksut(String hakukohdeOid, AuditSession session, List<LukuvuosimaksuMuutos> muutokset) {
        return postAsObservableLazily(getUrl("valinta-tulos-service.virkailija.valintatulos.lukuvuosimaksu", "write", hakukohdeOid),
                Void.class,
                Entity.json(of("lukuvuosimaksuMuutokset", muutokset, "auditSession", session)));
    }

    @Override
    public Observable<List<Valintatulos>> findValintatuloksetIlmanHakijanTilaa(String hakuOid, String hakukohdeOid) {
        return getAsObservableLazily(
                getUrl("valinta-tulos-service.virkailija.valintatulos.ilmanhakijantilaa.haku.hakukohde", hakuOid, hakukohdeOid),
                new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<Valintatulos>> findValintatuloksetByHakemus(String hakuOid, String hakemusOid) {
        return getAsObservableLazily(
                getUrl("valinta-tulos-service.virkailija.valintatulos.haku.hakemus", hakuOid, hakemusOid),
                new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<VastaanottoAikarajaMennytDTO>> findVastaanottoAikarajaMennyt(String hakuOid, String hakukohdeOid, Set<String> hakemusOids) {
        return postAsObservableLazily(
                getUrl("valinta-tulos-service.virkailija.myohastyneet.haku.hakukohde", hakuOid, hakukohdeOid),
                new GenericType<List<VastaanottoAikarajaMennytDTO>>() {}.getType(),
                Entity.json(hakemusOids));
    }

    @Override
    public Observable<List<TilaHakijalleDto>> findTilahakijalle(String hakuOid, String hakukohdeOid, String valintatapajonoOid, Set<String> hakemusOids) {
        return postAsObservableLazily(
                getUrl("valinta-tulos-service.virkailija.tilahakijalle.haku.hakukohde.valintatapajono", hakuOid, hakukohdeOid, valintatapajonoOid),
                new GenericType<List<TilaHakijalleDto>>() {}.getType(),
                Entity.json(hakemusOids));
    }

    @Override
    public Observable<List<ValintatulosUpdateStatus>> postErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid, List<Valinnantulos> valinnantulokset) {
        return postAsObservableLazily(
                getUrl("valinta-tulos-service.erillishaku.valinnan-tulos", valintatapajonoOid),
                new TypeToken<List<ValintatulosUpdateStatus>>() {}.getType(),
                Entity.entity(gson().toJson(new ValinnantulosRequest(auditSession, valinnantulokset)), MediaType.APPLICATION_JSON),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    if(auditSession.getIfUnmodifiedSince().isPresent()) {
                        client.header("If-Unmodified-Since", auditSession.getIfUnmodifiedSince().get());
                    }
                    return client;
                });
    }

    @Override
    public Observable<List<Valinnantulos>> getErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid) {
        return getAsObservableLazily(
          getUrl("valinta-tulos-service.erillishaku.valinnan-tulos", valintatapajonoOid) ,
          new GenericType<List<Valinnantulos>>() {}.getType(),
            client -> {
                client.accept(MediaType.APPLICATION_JSON_TYPE);
                client.query("sessionId", auditSession.getSessionId());
                client.query("uid", auditSession.getUid());
                client.query("inetAddress", auditSession.getInetAddress());
                client.query("userAgent", auditSession.getUserAgent());
                client.query("hyvaksymiskirjeet", "true");
                return client;
            });
    }

    @Override
    public Observable<HakukohdeDTO> getHakukohdeBySijoitteluajoPlainDTO(String hakuOid, String hakukohdeOid) {
        return getAsObservableLazily(
                getUrl("valinta-tulos-service.sijoittelu.sijoitteluajo.hakukohde", hakuOid, "latest", hakukohdeOid),
                new TypeToken<HakukohdeDTO>() {}.getType(),
                client -> {
                    client.accept(MediaType.WILDCARD_TYPE);
                    return client;
                }
        );
    }

    private static class OffsetDateTimeJsonSerializer implements JsonSerializer<OffsetDateTime> {
        @Override
        public JsonElement serialize(OffsetDateTime dateTime, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.atZoneSameInstant(ZoneId.of("Europe/Helsinki"))));
        }
    }

    private static class OffsetDateTimeJsonDeserializer implements JsonDeserializer<OffsetDateTime> {
        @Override
        public OffsetDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(json.getAsString()));
        }
    }

    private static class VtsDateTimeJsonDeserializer implements JsonDeserializer<DateTime> {
        @Override
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String dateAsString = json.getAsString();
            try {
                return DateTime.parse(dateAsString, valintaTulosServiceCompatibleFormatter);
            } catch (IllegalArgumentException iae) {
                return DateTime.parse(dateAsString, ISODateTimeFormat.dateTime());
            }
        }
    }

    private static class VtsDateTimeJsonSerializer implements JsonSerializer<DateTime> {
        @Override
        public JsonElement serialize(DateTime dateTime, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(ISODateTimeFormat.dateTime().print(dateTime));
        }
    }
}
