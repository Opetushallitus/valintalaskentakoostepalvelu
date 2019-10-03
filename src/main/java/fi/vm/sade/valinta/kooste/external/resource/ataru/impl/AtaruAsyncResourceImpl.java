package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KansalaisuusDto;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.AtaruHakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AtaruAsyncResourceImpl implements AtaruAsyncResource {
    private static final int SUITABLE_ATARU_HAKEMUS_CHUNK_SIZE = 1000;
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final HttpClient client;
    private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
    private final UrlConfiguration urlConfiguration;

    @Autowired
    public AtaruAsyncResourceImpl(
            @Qualifier("AtaruHttpClient") HttpClient client,
            OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
            KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.client = client;
        this.urlConfiguration = UrlConfiguration.getInstance();
        this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    private CompletableFuture<List<HakemusWrapper>> getApplications(String hakukohdeOid, List<String> hakemusOids) {
        if (hakukohdeOid == null && hakemusOids.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return getApplicationsInChunks(hakukohdeOid, hakemusOids).thenCompose(hakemukset -> {
            if (hakemukset.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return getHenkilotObservable(hakemukset).thenCompose(henkilot -> {
                            ensureKansalaisuus(henkilot);
                            Stream<String> asuinmaaKoodit = hakemukset.stream().map(h -> h.getKeyValues().get("country-of-residence"));
                            Stream<String> kansalaisuusKoodit = henkilot.values().stream().flatMap(h -> h.getKansalaisuus().stream().map(KansalaisuusDto::getKansalaisuusKoodi));
                            return getMaakoodit(asuinmaaKoodit, kansalaisuusKoodit).thenApply(maakoodit ->
                                    hakemukset.stream()
                                            .map(hakemusToHakemusWrapper(henkilot, maakoodit))
                                            .collect(Collectors.toList()));
                        });
            }
        });
    }

    private CompletableFuture<List<AtaruHakemus>> getApplicationChunk(String hakukohdeOid, List<String> hakemusOids) {
        Map<String, String> query = new HashMap<>();
        if (hakukohdeOid != null) {
            query.put("hakukohdeOid", hakukohdeOid);
        }
        return this.client.postJson(
                this.urlConfiguration.url("ataru.applications.by-hakukohde", query),
                Duration.ofMinutes(1),
                hakemusOids,
                new TypeToken<List<String>>() {}.getType(),
                new TypeToken<List<AtaruHakemus>>() {}.getType()
        );
    }

    private CompletableFuture<List<AtaruHakemus>> getApplicationsInChunks(String hakukohdeOid, List<String> hakemusOids) {
        if (hakemusOids.isEmpty()) {
            return getApplicationChunk(hakukohdeOid, hakemusOids);
        } else {
            List<List<String>> chunks = Lists.partition(hakemusOids, SUITABLE_ATARU_HAKEMUS_CHUNK_SIZE);
            return chunks.stream().reduce(
                    CompletableFuture.completedFuture(new LinkedList<>()),
                    (f, chunk) -> f.thenCompose(l -> getApplicationChunk(hakukohdeOid, chunk).thenApply(ll -> { l.addAll(ll); return l; })),
                    (f, ff) -> f.thenCompose(l -> ff.thenApply(ll -> { l.addAll(ll); return l; }))
            );
        }
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

    private CompletableFuture<Map<String, HenkiloPerustietoDto>> getHenkilotObservable(List<AtaruHakemus> hakemukset) {
        List<String> personOids = hakemukset.stream().map(AtaruHakemus::getPersonOid).distinct().collect(Collectors.toList());
        CompletableFuture<Map<String, HenkiloPerustietoDto>> f = new CompletableFuture<>();
        oppijanumerorekisteriAsyncResource.haeHenkilot(personOids).subscribe(f::complete, f::completeExceptionally);
        return f;
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
                    hakemus.getMaksuvelvollisuus(),
                    hakemus.getAsiointikieli(),
                    newKeyValues
            );
            AtaruHakemusWrapper wrapper = new AtaruHakemusWrapper(h, henkilo);
            wrapper.setKansalaisuus(kansalaisuudet);
            return wrapper;
        };
    }

    private CompletableFuture<Map<String, Koodi>> getMaakoodit(Stream<String> asuinmaaKoodit, Stream<String> kansalaisuusKoodit) {
        return Stream.concat(asuinmaaKoodit, kansalaisuusKoodit)
                .distinct()
                .reduce(
                        CompletableFuture.completedFuture(new HashMap<>()),
                        (f, koodiArvo) -> f.thenCompose(acc -> koodistoCachedAsyncResource.maatjavaltiot2ToMaatjavaltiot1("maatjavaltiot2_" + koodiArvo).thenApply(koodi -> { acc.put(koodiArvo, koodi); return acc; })),
                        (f, ff) -> f.thenCompose(acc -> ff.thenApply(acc2 -> { acc.putAll(acc2); return acc; }))
                );
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByHakukohde(String hakukohdeOid) {
        return getApplications(hakukohdeOid, Lists.newArrayList());
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByOids(List<String> oids) {
        return getApplications(null, oids);
    }
}
