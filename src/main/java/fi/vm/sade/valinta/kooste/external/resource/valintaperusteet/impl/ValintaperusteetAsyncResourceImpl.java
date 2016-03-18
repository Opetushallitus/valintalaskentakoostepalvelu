package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import rx.Observable;

@Service
public class ValintaperusteetAsyncResourceImpl extends HttpResource implements ValintaperusteetAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintaperusteetAsyncResourceImpl.class);

    @Autowired
    public ValintaperusteetAsyncResourceImpl(@Value("${host.ilb}") String address) {
        super(address);
    }
    // /valintaperusteet/hakijaryhmä/{hakukohdeoid}
    public Peruutettava haeHakijaryhmat(String hakukohdeOid, Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/hakijaryhma/" + hakukohdeOid;
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .accept(MediaType.APPLICATION_JSON_TYPE)
                            .async()
                            .get(new GsonResponseCallback<>(address, url, callback, failureCallback, new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Peruutettava haeValinnanvaiheetHakukohteelle(String hakukohdeOid, Consumer<List<ValinnanVaiheJonoillaDTO>> callback, Consumer<Throwable> failureCallback) {
        LOG.info("Valinnanvaiheiden haku...");
        // /valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
        try {
            String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/" + hakukohdeOid + "/valinnanvaihe";
            return new PeruutettavaImpl(
                    getWebClient()
                    .path(url)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<>(address, url, callback, failureCallback, new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid) {
        LOG.info("Valinnanvaiheiden haku...");
        String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/" + hakukohdeOid + "/ilmanlaskentaa";
        return getAsObservable(url, new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType());
    }

    public Observable<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(String hakukohdeOid) {
        return getAsObservable("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/hakijaryhma/" + hakukohdeOid,
                new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {}.getType(), client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    public Observable<List<ValintaperusteetDTO>> haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku) {
        return getAsObservable("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/" + hakukohdeOid,
                new TypeToken<List<ValintaperusteetDTO>>() {}.getType(), client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    if (valinnanVaiheJarjestysluku != null) {
                        client.query("vaihe", valinnanVaiheJarjestysluku);
                    }
                    return client;
                });
    }

    public Peruutettava haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku, Consumer<List<ValintaperusteetDTO>> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/" + hakukohdeOid;
            WebClient wc = getWebClient().path(url);
            if (valinnanVaiheJarjestysluku != null) {
                wc.query("vaihe", valinnanVaiheJarjestysluku);
            }
            return new PeruutettavaImpl(wc
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<>(address, url + "?vaihe=" + valinnanVaiheJarjestysluku, callback, failureCallback, new TypeToken<List<ValintaperusteetDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Peruutettava haunHakukohteet(String hakuOid, Consumer<List<HakukohdeViiteDTO>> callback, Consumer<Throwable> failureCallback) {
        try {
            String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/haku/" + hakuOid;
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<>(address, url, callback, failureCallback, new TypeToken<List<HakukohdeViiteDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
        return getWebClient()
                .path("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/tuoHakukohde/")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(hakukohde, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public Observable<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
        return getAsObservable("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/" + hakukohdeOid + "/", new TypeToken<List<ValintaperusteDTO>>() {}.getType());
    }

    @Override
    public Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids) {
        return getWebClient()
                .path("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/valintakoe")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<HakukohdeJaValintakoeDTO>>() {});
    }

    @Override
    public Peruutettava haeValintakokeetHakukohteille(Collection<String> hakukohdeOids, Consumer<List<HakukohdeJaValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/valintakoe";
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE),
                        new GsonResponseCallback<>(address, url, callback, failureCallback, new TypeToken<List<HakukohdeJaValintakoeDTO>>() {}.getType())));
    }

    @Override
    public Future<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
        return getWebClient()
                .path("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/" + hakukohdeOid + "/valintakoe/")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GenericType<List<ValintakoeDTO>>() {});
    }

    @Override
    public Peruutettava haeValintakokeetHakukohteelle(String hakukohdeOid, Consumer<List<ValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/" + hakukohdeOid + "/valintakoe/";
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GsonResponseCallback<>(address, url, callback, failureCallback, new TypeToken<List<ValintakoeDTO>>() {}.getType())));
    }

    @Override
    public Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid) {
        String url = "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valinnanvaihe/" + oid + "/hakukohteet";
        LOG.info("Calling url {}", url);
        return getAsObservable(url, new TypeToken<Set<String>>() {}.getType());
    }


}
