package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class ValintaperusteetAsyncResourceImpl extends UrlConfiguredResource implements ValintaperusteetAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(ValintaperusteetAsyncResourceImpl.class);

    public ValintaperusteetAsyncResourceImpl() {
        super(TimeUnit.HOURS.toMillis(1L));
    }

    public Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid) {
        LOG.info("Valinnanvaiheiden haku...");
        return getAsObservableLazily(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.ilmanlaskentaa", hakukohdeOid),
                new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {}.getType());
    }

    public Observable<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(String hakukohdeOid) {
        return getAsObservableLazily(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.hakijaryhma", hakukohdeOid),
                new TypeToken<List<ValintaperusteetHakijaryhmaDTO>>() {}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    public Observable<List<ValintaperusteetDTO>> haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku) {
        return getAsObservableLazily(
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
        return getAsObservableLazily(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.avaimet.oid", hakukohdeOid),
                new TypeToken<List<ValintaperusteDTO>>() {}.getType());
    }

    @Override
    public Observable<List<HakukohdeJaValintaperusteDTO>> findAvaimet(Collection<String> hakukohdeOids) {
        return postAsObservableLazily(
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
        return getAsObservableLazily(
                getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valinnanvaihe.valintaperusteet", valinnanvaiheOid),
                new TypeToken<List<ValintaperusteetDTO>>() {}.getType());
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
        return postAsObservableLazily(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe"), new TypeToken<List<HakukohdeJaValintakoeDTO>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakukohdeOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
    }

    @Override
    public Observable<Map<String, List<ValintatapajonoDTO>>> haeValintatapajonotSijoittelulle (Collection<String> hakukohdeOids) {
        return postAsObservableLazily(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintatapajono"),
                new TypeToken<Map<String, List<ValintatapajonoDTO>>>() {}.getType(),
                Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE));
    }
    @Override
    public Future<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
        return getWebClient()
                .path(getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintakoe", hakukohdeOid))
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
        return getAsObservableLazily(url, new TypeToken<Set<String>>() {}.getType());
    }
}
