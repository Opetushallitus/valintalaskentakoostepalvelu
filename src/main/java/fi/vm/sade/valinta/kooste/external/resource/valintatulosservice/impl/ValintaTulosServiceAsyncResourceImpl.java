package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakemuksenVastaanottotila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.PoistaVastaanottoDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.TilaHakijalleDto;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoRecordDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoResultDTO;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ValintaTulosServiceAsyncResourceImpl extends UrlConfiguredResource implements ValintaTulosServiceAsyncResource {

    @Autowired
    public ValintaTulosServiceAsyncResourceImpl(UrlConfiguration urlConfiguration) {
        super(urlConfiguration, TimeUnit.MINUTES.toMillis(30));
    }

    @Override
    protected Gson createGson() {
        return DateDeserializer.gsonBuilder()
                .registerTypeAdapter(DateTime.class, new VtsDateTimeJsonDeserializer())
                .create();
    }

    @Override
    public Observable<List<ValintaTulosServiceDto>> getHaunValintatulokset(String hakuOid) {
        return getAsObservable(getUrl("valinta-tulos-service.haku", hakuOid),
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
        String url = getUrl("valinta-tulos-service.virkailija.haku.hakukohde", hakuOid, hakukohdeOid);
        return getAsObservable(url, new GenericType<List<HakemuksenVastaanottotila>>(){}.getType());
    }

    @Override
    public Observable<String> getHakemuksenValintatulosAsString(String hakuOid, String hakemusOid) {
        return getStringAsObservable(
                getUrl("valinta-tulos-service.haku.hakuoid.hakemus", hakuOid, hakemusOid));
    }

    @Override
    public Observable<List<Valintatulos>> findValintatulokset(String hakuOid, String hakukohdeOid) {
        return getAsObservable(getUrl("valinta-tulos-service.virkailija.haku.hakukohde", hakuOid, hakukohdeOid), new GenericType<List<Valintatulos>>() {}.getType());
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

    private static class VtsDateTimeJsonDeserializer implements JsonDeserializer<DateTime> {
        @Override
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String dateAsString = json.getAsString();
            return DateTime.parse(dateAsString, valintaTulosServiceCompatibleFormatter);
        }
    }
}
