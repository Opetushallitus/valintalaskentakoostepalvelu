package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import rx.Observable;

@Service
public class SijoitteluAsyncResourceImpl extends AsyncResourceWithCas implements SijoitteluAsyncResource {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new JsonDeserializer() {
                @Override
                public Object deserialize(JsonElement json, Type typeOfT,
                                          JsonDeserializationContext context)
                        throws JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            })
            .create();

    @Autowired
    public SijoitteluAsyncResourceImpl(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.sijoittelu-service}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.sijoittelu}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.sijoittelu}") String appClientPassword,
            @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String address,
            ApplicationContext context) {
        super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.MINUTES.toMillis(50));
    }

    public void getLatestHakukohdeBySijoitteluAjoId(String hakuOid, String hakukohdeOid, Long sijoitteluAjoId
            , Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus) {
        ///erillissijoittelu/{hakuOid}/sijoitteluajo/{sijoitteluAjoId}/hakukohde/{hakukodeOid}
        String url = "/erillissijoittelu/" + hakuOid + "/sijoitteluajo/" + sijoitteluAjoId + "/hakukohde/" + hakukohdeOid;
        getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async().get(new GsonResponseCallback<HakukohdeDTO>(GSON,
                address, url, hakukohde, poikkeus,
                new TypeToken<HakukohdeDTO>() {
                }.getType()));
    }

    public void getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid
            , Consumer<HakukohdeDTO> hakukohde, Consumer<Throwable> poikkeus) {
        ///sijoittelu/{hakuOid}/sijoitteluajo/latest/hakukohde/{hakukohdeOid}
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakukohde/" + hakukohdeOid;
        getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async().get(new GsonResponseCallback<HakukohdeDTO>(GSON,
                address, url, hakukohde, poikkeus,
                new TypeToken<HakukohdeDTO>() {
                }.getType()));
    }

    @Override
    public Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid) {
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakemukset";
        LOG.info("Asynkroninen kutsu: {}{}?ilmanHyvaksyntaa=true", address, url);
        return getWebClient()
                .path(url)
                .query("ilmanHyvaksyntaa", true)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async().get(new GenericType<HakijaPaginationObject>() {
                });
    }

    public Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid) {
        String url = "/tila/hakukohde/" + hakukohdeOid + "/" + valintatapajonoOid;
        return getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async().get(new GenericType<List<Valintatulos>>() {
                });
    }

    public Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid) {
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakukohde/" + hakukohdeOid;
        return getWebClient()
                .path(url)
                .accept(MediaType.WILDCARD)
                .async().get(new GenericType<HakukohdeDTO>() {
                });
    }

    @Override
    public Future<HakijaPaginationObject> getKaikkiHakijat(String hakuOid, String hakukohdeOid) {
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakemukset";
        LOG.info("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}", address, url, hakukohdeOid);
        return getWebClient()
                .path(url)
                .query("hakukohdeOid", hakukohdeOid)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async().get(new GenericType<HakijaPaginationObject>() {
                });
    }

    @Override
    public Future<HakijaPaginationObject> getKoulutuspaikkallisetHakijat(String hakuOid, String hakukohdeOid) {
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakemukset";
        LOG.info("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}", address, url, hakukohdeOid);
        return getWebClient()
                .path(url)
                .query("hyvaksytyt", true)
                .query("hakukohdeOid", hakukohdeOid)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async().get(new GenericType<HakijaPaginationObject>() {
                });
    }

    @Override
    public Observable<HakijaPaginationObject> getKoulutuspaikkalliset(
            String hakuOid, String hakukohdeOid) {
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakemukset";
        return getAsObservable(
                url,
                new TypeToken<HakijaPaginationObject>() {
                }.getType(),
                client -> {
                    client.query("hyvaksytyt", true);
                    client.query("hakukohdeOid", hakukohdeOid);
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Peruutettava getKoulutuspaikkallisetHakijat(
            String hakuOid, String hakukohdeOid, Consumer<HakijaPaginationObject> callback, Consumer<Throwable> failureCallback) {
        String url = "/sijoittelu/" + hakuOid + "/sijoitteluajo/" + SijoitteluResource.LATEST + "/hakemukset";
        LOG.info("Asynkroninen kutsu: {}{}?hyvaksytyt=true&hakukohdeOid={}", address, url, hakukohdeOid);
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .query("hyvaksytyt", true)
                .query("hakukohdeOid", hakukohdeOid)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async().get(new GsonResponseCallback<HakijaPaginationObject>(GSON,
                        address, url, callback, failureCallback,
                        new TypeToken<HakijaPaginationObject>() {
                        }.getType())));
    }
}
