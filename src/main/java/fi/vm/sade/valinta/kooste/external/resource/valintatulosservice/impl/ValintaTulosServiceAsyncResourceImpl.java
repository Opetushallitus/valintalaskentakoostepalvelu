package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import com.google.common.collect.ImmutableMap;
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
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.*;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.PoistaVastaanottoDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.TilaHakijalleDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rx.Observable;

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

import static com.google.common.collect.ImmutableMap.of;

@Service
public class ValintaTulosServiceAsyncResourceImpl extends UrlConfiguredResource implements ValintaTulosServiceAsyncResource {
    public ValintaTulosServiceAsyncResourceImpl() {
        super(TimeUnit.MINUTES.toMillis(30));
    }

    @Override
    protected Gson createGson() {
        return DateDeserializer.gsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeJsonSerializer())
                .registerTypeAdapter(DateTime.class, new VtsDateTimeJsonDeserializer())
                .registerTypeAdapter(DateTime.class, new VtsDateTimeJsonSerializer())
                .create();
    }

    @Override
    public Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid) {
        return getAsObservable(getUrl("valinta-tulos-service.haku.hakuoid", hakuOid),
                new GenericType<List<ValintaTulosServiceDto>>() {}.getType());
    }

    @Override
    public Observable<ValintaTulosServiceDto> getHakemuksenValintatulos(String hakuOid, String hakemusOid) {
        return getAsObservable(
                getUrl("valinta-tulos-service.haku.hakuoid.hakemus", hakuOid, hakemusOid),
                ValintaTulosServiceDto.class);
    }

    @Override
    public Observable<List<HakemuksenVastaanottotila>> getVastaanottotilatByHakemus(String hakuOid, String hakukohdeOid) {
        return getAsObservable(getUrl("valinta-tulos-service.virkailija.haku.hakukohde", hakuOid, hakukohdeOid),
                new GenericType<List<HakemuksenVastaanottotila>>(){}.getType());
    }

    @Override
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return getStringAsObservable(
                getUrl("valinta-tulos-service.haku.hakuoid.hakemus", hakuOid, hakemusOid));
    }

    @Override
    public Observable<List<Valintatulos>> findValintatulokset(String hakuOid, String hakukohdeOid) {
        return getAsObservable(getUrl("valinta-tulos-service.virkailija.valintatulos.haku.hakukohde", hakuOid, hakukohdeOid), new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<Lukuvuosimaksu>> fetchLukuvuosimaksut(String hakukohdeOid, AuditSession session) {
        return getAsObservable(getUrl("valinta-tulos-service.virkailija.valintatulos.lukuvuosimaksu", hakukohdeOid), new GenericType<List<Lukuvuosimaksu>>() {}.getType(),
                Entity.json(of("auditSession", session)));
    }

    @Override
    public Observable<Void> saveLukuvuosimaksut(String hakukohdeOid, AuditSession session, List<LukuvuosimaksuMuutos> muutokset) {
        return postAsObservable(getUrl("valinta-tulos-service.virkailija.valintatulos.lukuvuosimaksu", hakukohdeOid),
                Void.class,
                Entity.json(of("lukuvuosimaksuMuutokset", muutokset, "auditSession", session)));
    }

    @Override
    public Observable<List<Valintatulos>> findValintatuloksetIlmanHakijanTilaa(String hakuOid, String hakukohdeOid) {
        return getAsObservable(
                getUrl("valinta-tulos-service.virkailija.valintatulos.ilmanhakijantilaa.haku.hakukohde", hakuOid, hakukohdeOid),
                new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<Valintatulos>> findValintatuloksetByHakemus(String hakuOid, String hakemusOid) {
        return getAsObservable(
                getUrl("valinta-tulos-service.virkailija.valintatulos.haku.hakemus", hakuOid, hakemusOid),
                new GenericType<List<Valintatulos>>() {}.getType());
    }

    @Override
    public Observable<List<VastaanottoAikarajaMennytDTO>> findVastaanottoAikarajaMennyt(String hakuOid, String hakukohdeOid, Set<String> hakemusOids) {
        return postAsObservable(
                getUrl("valinta-tulos-service.virkailija.myohastyneet.haku.hakukohde", hakuOid, hakukohdeOid),
                new GenericType<List<VastaanottoAikarajaMennytDTO>>() {}.getType(),
                Entity.json(hakemusOids));
    }

    @Override
    public Observable<List<TilaHakijalleDto>> findTilahakijalle(String hakuOid, String hakukohdeOid, String valintatapajonoOid, Set<String> hakemusOids) {
        return postAsObservable(
                getUrl("valinta-tulos-service.virkailija.tilahakijalle.haku.hakukohde.valintatapajono", hakuOid, hakukohdeOid, valintatapajonoOid),
                new GenericType<List<TilaHakijalleDto>>() {}.getType(),
                Entity.json(hakemusOids));
    }

    @Override
    public Observable<List<VastaanottoRecordDTO>> hakukohteenVastaanotot(String hakukohdeOid) {
        return getAsObservable(
                getUrl("valinta-tulos-service.virkailija.vastaanotto.hakukohde", hakukohdeOid),
                new GenericType<List<VastaanottoRecordDTO>>() {}.getType());
    }

    @Override
    public Observable<Void> poista(PoistaVastaanottoDTO poistaVastaanottoDTO) {
        return postAsObservable(
                getUrl("valinta-tulos-service.virkailija.vastaanotto.poista"),
                Void.class,
                Entity.json(poistaVastaanottoDTO));
    }

    @Override
    public Observable<List<VastaanottoResultDTO>> tallenna(List<VastaanottoRecordDTO> tallennettavat) {
        return postAsObservable(
                getUrl("valinta-tulos-service.virkailija.vastaanotto"),
                new GenericType<List<VastaanottoResultDTO>>() {}.getType(),
                Entity.json(tallennettavat));
    }

    @Override
    public Observable<List<ValintatulosUpdateStatus>> postErillishaunValinnantulokset(AuditSession auditSession, String valintatapajonoOid, List<Valinnantulos> valinnantulokset) {
        return postAsObservable(
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
        return getAsObservable(
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

    private static class OffsetDateTimeJsonSerializer implements JsonSerializer<OffsetDateTime> {
        @Override
        public JsonElement serialize(OffsetDateTime dateTime, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.atZoneSameInstant(ZoneId.of("Europe/Helsinki"))));
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
