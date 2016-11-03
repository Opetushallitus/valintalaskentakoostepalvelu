package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.dto.*;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import rx.Observable;

@Service
public class ValintaperusteetAsyncResourceImpl extends UrlConfiguredResource implements ValintaperusteetAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintaperusteetAsyncResourceImpl.class);

    @Autowired
    public ValintaperusteetAsyncResourceImpl(UrlConfiguration urlConfiguration) {
        super(urlConfiguration, TimeUnit.HOURS.toMillis(1L));
    }

    public Peruutettava haeHakijaryhmat(String hakukohdeOid, Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.hakijaryhma", hakukohdeOid);
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .accept(MediaType.APPLICATION_JSON_TYPE)
                            .async()
                            .get(new GsonResponseCallback<>(gson(), url, callback, failureCallback, new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Peruutettava haeValinnanvaiheetHakukohteelle(String hakukohdeOid, Consumer<List<ValinnanVaiheJonoillaDTO>> callback, Consumer<Throwable> failureCallback) {
        LOG.info("Valinnanvaiheiden haku...");
        try {
            String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valinnanvaihe", hakukohdeOid);
            return new PeruutettavaImpl(
                    getWebClient()
                    .path(url)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<>(gson(), url, callback, failureCallback, new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid) {
        LOG.info("Valinnanvaiheiden haku...");
        String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.ilmanlaskentaa", hakukohdeOid);
        return getAsObservable(url, new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType());
    }

    public Observable<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(String hakukohdeOid) {
        return getAsObservable(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.hakijaryhma", hakukohdeOid),
                new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    public Observable<List<ValintaperusteetDTO>> haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku) {
        return getAsObservable(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet", hakukohdeOid),
                new TypeToken<List<ValintaperusteetDTO>>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    if (valinnanVaiheJarjestysluku != null) {
                        client.query("vaihe", valinnanVaiheJarjestysluku);
                    }
                    return client;
                });
    }

    public Peruutettava haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku, Consumer<List<ValintaperusteetDTO>> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet", hakukohdeOid);
            WebClient wc = getWebClient().path(url);
            if (valinnanVaiheJarjestysluku != null) {
                wc.query("vaihe", valinnanVaiheJarjestysluku);
            }
            return new PeruutettavaImpl(wc
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<>(gson(), url + "?vaihe=" + valinnanVaiheJarjestysluku, callback, failureCallback, new TypeToken<List<ValintaperusteetDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Peruutettava haunHakukohteet(String hakuOid, Consumer<List<HakukohdeViiteDTO>> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.haku", hakuOid);
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<>(gson(), url, callback, failureCallback, new TypeToken<List<HakukohdeViiteDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
        return getWebClient()
                .path(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.tuohakukohde"))
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(hakukohde, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public Observable<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
        return getAsObservable(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.avaimet.oid", hakukohdeOid),
                new TypeToken<List<ValintaperusteDTO>>() {}.getType());
    }

    @Override
    public Observable<List<HakukohdeJaValintaperusteDTO>> findAvaimet(Collection<String> hakukohdeOids) {
        return postAsObservable(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.avaimet"),
                new TypeToken<List<HakukohdeJaValintaperusteDTO>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    @Override
    public Observable<List<ValintaperusteetDTO>> valintaperusteet(String valinnanvaiheOid) {
        return getAsObservable(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valinnanvaihe.valintaperusteet", valinnanvaiheOid),
                new TypeToken<List<ValintaperusteetDTO>>() {}.getType());
    }

    @Override
    public Observable<List<ValintakoeDTO>> readByTunnisteet(Collection<String> tunnisteet) {
        return postAsObservable(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.tunniste"), new TypeToken<List<ValintakoeDTO>>() {}.getType(),
                Entity.entity(Lists.newArrayList(tunnisteet), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    @Override
    public Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids) {
        return getWebClient()
                .path(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe"))
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<HakukohdeJaValintakoeDTO>>() {});
    }

    @Override
    public Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakutoiveille(Collection<String> hakukohdeOids) {
        return postAsObservable(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe"), new TypeToken<List<HakukohdeJaValintakoeDTO>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    @Override
    public Peruutettava haeValintakokeetHakukohteille(Collection<String> hakukohdeOids, Consumer<List<HakukohdeJaValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe");
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE),
                        new GsonResponseCallback<>(gson(), url, callback, failureCallback, new TypeToken<List<HakukohdeJaValintakoeDTO>>() {}.getType())));
    }
    @Override
    public Observable<Map<String, List<ValintatapajonoDTO>>> haeValintatapajonotSijoittelulle (Collection<String> hakukohdeOids) {
        String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintatapajono";
        return postAsObservable(url,
                new TypeToken<Map<String, List<ValintatapajonoDTO>>>() {}.getType(),
                Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE));
    }
    @Override
    public Future<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
        return getWebClient()
                .path(getUrl("", hakukohdeOid))
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GenericType<List<ValintakoeDTO>>() {});
    }

    @Override
    public Peruutettava haeValintakokeetHakukohteelle(String hakukohdeOid, Consumer<List<ValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintakoe", hakukohdeOid);
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GsonResponseCallback<>(gson(), url, callback, failureCallback, new TypeToken<List<ValintakoeDTO>>() {}.getType())));
    }

    @Override
    public Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid) {
        String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valinnanvaihe.hakukohteet", oid);
        LOG.info("Calling url {}", url);
        return getAsObservable(url, new TypeToken<Set<String>>() {}.getType());
    }
}
