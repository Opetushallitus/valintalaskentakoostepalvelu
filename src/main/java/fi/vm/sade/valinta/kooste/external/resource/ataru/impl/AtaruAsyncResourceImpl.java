package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KansalaisuusDto;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AtaruAsyncResourceImpl extends UrlConfiguredResource implements AtaruAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    public AtaruAsyncResourceImpl(
            @Qualifier("AtaruRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource1,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
        this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource1;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    private Observable<List<HakemusWrapper>> getApplications(String hakukohdeOid, List<String> hakemusOids) {
        if (hakukohdeOid == null && hakemusOids.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }
        return this.<String, List<AtaruHakemus>>postAsObservableLazily(
                getUrl("ataru.applications.by-hakukohde"),
                new TypeToken<List<AtaruHakemus>>() {
                }.getType(),
                Entity.entity(gson().toJson(hakemusOids), MediaType.APPLICATION_JSON),
                client -> {
                    if (hakukohdeOid != null) {
                        client.query("hakukohdeOid", hakukohdeOid);
                    }
                    LOG.info("Calling url {}", client.getCurrentURI());
                    return client;
                }).flatMap(hakemukset -> {
            if (hakemukset.isEmpty()) {
                return Observable.just(Collections.emptyList());
            } else {
                return getHenkilotObservable(hakemukset)
                        .flatMap(henkilot -> {
                            ensureKansalaisuus(henkilot);
                            Stream<String> asuinmaaKoodit = hakemukset.stream().map(h -> h.getKeyValues().get("country-of-residence"));
                            Stream<String> kansalaisuusKoodit = henkilot.values().stream().flatMap(h -> h.getKansalaisuus().stream().map(KansalaisuusDto::getKansalaisuusKoodi));
                            return getMaakooditObservable(asuinmaaKoodit, kansalaisuusKoodit)
                                    .map(maakoodit ->
                                            hakemukset.stream()
                                                    .map(hakemusToHakemusWrapper(henkilot, maakoodit))
                                                    .collect(Collectors.toList()));
                        });
            }
        });
    }

    private void ensureKansalaisuus(Map<String, HenkiloPerustietoDto> henkilot) {
        List<String> missingKansalaisuus = henkilot.entrySet().stream()
                .filter(e -> e.getValue().getKansalaisuus().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!missingKansalaisuus.isEmpty()) {
            LOG.warn(String.format("Kansalaisuus missing from henkilöt: %s",
                    String.join(", ", missingKansalaisuus)
            ));
        }
    }

    private Observable<Map<String, HenkiloPerustietoDto>> getHenkilotObservable(List<AtaruHakemus> hakemukset) {
        List<String> personOids = hakemukset.stream().map(AtaruHakemus::getPersonOid).distinct().collect(Collectors.toList());
        return oppijanumerorekisteriAsyncResource.haeHenkilot(Lists.newArrayList(personOids))
                .map(henkiloDtot -> henkiloDtot.stream().collect(Collectors.toMap(HenkiloPerustietoDto::getOidHenkilo, h -> h)));
    }

    private Function<AtaruHakemus, AtaruHakemusWrapper> hakemusToHakemusWrapper(Map<String, HenkiloPerustietoDto> henkilot, Map<String, Koodi> maakoodit) {
        return hakemus -> {
            String ISOmaakoodi = maakoodit.get(hakemus.getKeyValues().get("country-of-residence")).getKoodiArvo();
            HenkiloPerustietoDto henkilo = henkilot.get(hakemus.getPersonOid());
            List<String> kansalaisuudet = henkilo.getKansalaisuus().stream()
                    .map(k -> maakoodit.get(k.getKansalaisuusKoodi()).getKoodiArvo())
                    .collect(Collectors.toList());
            Map<String, String> newKeyValues = new HashMap<>(hakemus.getKeyValues());
            newKeyValues.replace("country-of-residence", ISOmaakoodi);
            AtaruHakemus h = new AtaruHakemus(
                    hakemus.getHakemusOid(),
                    hakemus.getPersonOid(),
                    hakemus.getHakuOid(),
                    hakemus.getHakutoiveet(),
                    newKeyValues
            );
            AtaruHakemusWrapper wrapper = new AtaruHakemusWrapper(h, henkilo);
            wrapper.setKansalaisuus(kansalaisuudet);
            return wrapper;
        };
    }

    private Observable<Map<String, Koodi>> getMaakooditObservable(Stream<String> asuinmaaKoodit, Stream<String> kansalaisuusKoodit) {
        return Observable.merge(
                Stream.concat(asuinmaaKoodit, kansalaisuusKoodit)
                        .distinct()
                        .map(koodiArvo -> koodistoCachedAsyncResource.maatjavaltiot2ToMaatjavaltiot1("maatjavaltiot2_" + koodiArvo)
                                .map(koodi -> Pair.of(koodiArvo, koodi)))
                        .collect(Collectors.toList())).toMap(Pair::getLeft, Pair::getRight);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid) {
        return getApplications(hakukohdeOid, Lists.newArrayList());
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(List<String> oids) {
        return getApplications(null, oids);
    }
}