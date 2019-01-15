package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    public Observable<List<HakukohdeViiteDTO>> haunHakukohteet(String hakuOid) {
        return getAsObservableLazily(
            getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.haku", hakuOid),
            new TypeToken<List<HakukohdeViiteDTO>>() {}.getType(),
            ACCEPT_JSON
        );
    }

    @Override
    public Observable<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
        return postAsObservableLazily(
            getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaperusteet.tuohakukohde"),
            Entity.entity(hakukohde, MediaType.APPLICATION_JSON_TYPE),
            ACCEPT_JSON);
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
    public Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids) {
        return postAsObservableLazily(
            getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.hakukohde.valintakoe"),
            new GenericType<List<HakukohdeJaValintakoeDTO>>() {}.getType(),
            Entity.entity(hakukohdeOids, MediaType.APPLICATION_JSON_TYPE),
            ACCEPT_JSON);
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
    public Observable<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
        return getAsObservableLazily(
            getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintakoe", hakukohdeOid),
            new GenericType<List<ValintakoeDTO>>() {}.getType(),
            ACCEPT_JSON
        );
    }

    @Override
    public Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid) {
        String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valinnanvaihe.hakukohteet", oid);
        LOG.info("Calling url {}", url);
        return getAsObservableLazily(url, new TypeToken<Set<String>>() {}.getType());
    }

    @Override
    public Observable<String> haeValintaryhmaVastuuorganisaatio(String valintaryhmaOid) {
        String url = getUrl("valintaperusteet-service.valintalaskentakoostepalvelu.valintaryhma.vastuuorganisaatio", valintaryhmaOid);
        LOG.info("Calling url {}", url);
        return getAsObservableLazily(url, String.class, client -> {
            client.accept(MediaType.TEXT_PLAIN_TYPE);
            return client;
        });
    }
}
