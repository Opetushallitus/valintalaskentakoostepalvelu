package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.Observable.zip;
import com.google.common.collect.Sets;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KoekutsukirjeetImpl implements KoekutsukirjeetService {
    private static final Logger LOG = LoggerFactory
            .getLogger(KoekutsukirjeetImpl.class);
    private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final ValintaperusteetAsyncResource valintakoeResource;
    private final ValintalaskentaValintakoeAsyncResource osallistumisetResource;

    @Autowired
    public KoekutsukirjeetImpl(
            KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetValintakoeAsyncResource,
            ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.valintakoeResource = valintaperusteetValintakoeAsyncResource;
        this.osallistumisetResource = valintalaskentaValintakoeAsyncResource;
    }

    @Override
    public void koekutsukirjeetHakemuksille(KirjeProsessi prosessi, KoekutsuDTO koekutsu, Collection<String> hakemusOids) {
        applicationAsyncResource.getApplicationsByOids(hakemusOids)
                .subscribeOn(Schedulers.newThread())
                .subscribe(koekutsukirjeiksi(prosessi, koekutsu),
                        t1 -> {
                            LOG.error("Hakemuksien haussa hakutoiveelle " + koekutsu.getHakukohdeOid(), t1);
                            prosessi.keskeyta();
                        }
                );
    }

    @Override
    public void koekutsukirjeetOsallistujille(KirjeProsessi prosessi, KoekutsuDTO koekutsu, List<String> valintakoeTunnisteet) {
        final Observable<List<ValintakoeOsallistuminenDTO>> osallistumiset = osallistumisetResource.haeHakutoiveelle(koekutsu.getHakukohdeOid());
        final Observable<List<ValintakoeDTO>> valintakokeetObservable = valintakoeResource.haeValintakokeetHakukohteelle(koekutsu.getHakukohdeOid());
        final Observable<List<Hakemus>> hakemuksetObservable = applicationAsyncResource.getApplicationsByOid(koekutsu.getHakuOid(), koekutsu.getHakukohdeOid());

        zip(valintakokeetObservable, hakemuksetObservable,
                (valintakoes, hakemukset) -> {
                    try {
                        boolean haetaankoKaikkiHakutoiveenHakijatValintakokeeseen = valintakoes
                                .stream()
                                .filter(Objects::nonNull)
                                .filter(v -> valintakoeTunnisteet.contains(v.getSelvitettyTunniste()))
                                .anyMatch(vk -> Boolean.TRUE.equals(vk.getKutsutaankoKaikki()));
                        if (haetaankoKaikkiHakutoiveenHakijatValintakokeeseen) {
                            LOG.info("Kaikki hakutoiveen {} hakijat osallistuu!", koekutsu.getHakukohdeOid());
                            return Observable.just(hakemukset);
                        }
                    } catch (Exception e) {
                        LOG.error("Kutsutaanko kaikki kokeeseen tarkistus epaonnistui!", e);
                        throw e;
                    }
                    try {
                        return osallistumiset.map(osallistuminenDTOS -> {
                            Set<String> osallistujienHakemusOidit = osallistuminenDTOS
                                .stream()
                                .filter(Objects::nonNull)
                                .filter(OsallistujatPredicate.osallistujat(valintakoeTunnisteet, koekutsu.getHakukohdeOid()))
                                .map(ValintakoeOsallistuminenDTO::getHakemusOid)
                                .collect(Collectors.toSet());
                            Stream<Hakemus> hakukohteenUlkopuolisetHakemukset = getHakukohteenUlkopuolisetHakemukset(hakemukset, osallistujienHakemusOidit);
                            // vain hakukohteen osallistujat
                            List<Hakemus> lopullinenHakemusJoukko = Stream.concat(hakukohteenUlkopuolisetHakemukset,
                                hakemukset.stream().filter(h -> osallistujienHakemusOidit.contains(h.getOid())))
                                .collect(Collectors.toList());
                            LOG.info("{}", lopullinenHakemusJoukko.size());
                            return lopullinenHakemusJoukko;
                        });
                    } catch (Exception e) {
                        LOG.error("Osallistumisia ei saatu valintalaskennasta! Valintakokeita oli " + valintakoeTunnisteet.size(), e);
                        throw new RuntimeException("Osallistumisia ei saatu valintalaskennasta! Valintakokeita oli " + valintakoeTunnisteet.size(), e);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .flatMap(x -> x)
                .subscribe(koekutsukirjeiksi(prosessi, koekutsu),
                        t1 -> {
                            LOG.error("Osallistumistietojen haussa hakutoiveelle " + koekutsu.getHakukohdeOid(), t1);
                            prosessi.keskeyta();
                        }
                );
    }

    Stream<Hakemus> getHakukohteenUlkopuolisetHakemukset(List<Hakemus> hakemukset, Set<String> osallistujienHakemusOidit) {
        Stream<Hakemus> hakukohteenUlkopuolisetHakemukset;
        {
            Set<String> hakemusOids = hakemukset.stream().map(Hakemus::getOid).collect(Collectors.toSet());
            Set<String> hakukohteenUlkopuolisetKoekutsuttavat = Sets.newHashSet(osallistujienHakemusOidit);
            hakukohteenUlkopuolisetKoekutsuttavat.removeIf(hakemusOids::contains);
            if (!hakukohteenUlkopuolisetKoekutsuttavat.isEmpty()) {
                hakukohteenUlkopuolisetHakemukset = applicationAsyncResource.getApplicationsByOids(hakukohteenUlkopuolisetKoekutsuttavat)
                    .timeout(30, SECONDS)
                    .toBlocking()
                    .first()
                    .stream();
            } else {
                hakukohteenUlkopuolisetHakemukset = Stream.empty();
            }
        }
        return hakukohteenUlkopuolisetHakemukset;
    }

    private Action1<List<Hakemus>> koekutsukirjeiksi(final KirjeProsessi prosessi, final KoekutsuDTO koekutsu) {
        return hakemukset -> {
            if (hakemukset.isEmpty()) {
                LOG.error("Hakutoiveeseen {} ei ole hakijoita. Yritettiin muodostaa koekutsukirjetta!", koekutsu.getHakukohdeOid());
                throw new RuntimeException("Koekutsuja ei voida muodostaa ilman valintakoetta johon osallistuu edes joku.");
            }
            // // Puuttuvat hakemukset //
            try {
                LOG.info("Haetaan valintakokeet hakutoiveille!");
                final Map<String, HakukohdeJaValintakoeDTO> valintakoeOidsHakutoiveille;
                try {
                    Set<String> hakutoiveetKaikistaHakemuksista = Sets.newHashSet(hakemukset.stream()
                            .flatMap(h -> new HakuappHakemusWrapper(h).getHakutoiveOids().stream()).collect(Collectors.toSet()));
                    hakutoiveetKaikistaHakemuksista.add(koekutsu.getHakukohdeOid());
                    LOG.info("Hakutoiveet hakemuksista:\r\n{}", Arrays.toString(hakutoiveetKaikistaHakemuksista.toArray()));
                    valintakoeOidsHakutoiveille = valintakoeResource
                            .haeValintakokeetHakukohteille(hakutoiveetKaikistaHakemuksista)
                            .timeout(1, HOURS)
                            .toBlocking()
                            .first()
                            .stream()
                            .filter(h -> h.getValintakoeDTO() != null && !h.getValintakoeDTO().isEmpty())
                            .collect(Collectors.toMap(HakukohdeJaValintakoeDTO::getHakukohdeOid, h -> h));
                    if (valintakoeOidsHakutoiveille.isEmpty()) {
                        throw new RuntimeException("Yhdellekaan hakutoiveelle ei loytynyt valintakokeita!");
                    }
                } catch (Exception e) {
                    LOG.error("Valintakokeiden haku hakutoiveille epaonnistui!", e);
                    throw e;
                }
                final Map<String, Collection<String>> hakemusOidJaHakijanMuutHakutoiveOids;
                final Set<String> kohdeHakukohteenTunnisteet;
                try {
                    LOG.info("Haetaan tunnisteet kohde valintakokeille: Onko valintakoeOid {}", valintakoeOidsHakutoiveille.containsKey(koekutsu.getHakukohdeOid()));
                    kohdeHakukohteenTunnisteet = valintakoeOidsHakutoiveille
                            .get(koekutsu.getHakukohdeOid())
                            .getValintakoeDTO().stream()
                            .filter(Objects::nonNull)
                            .filter(v -> Boolean.TRUE.equals(v.getAktiivinen()))
                            .map(ValintakoeDTO::getSelvitettyTunniste)
                            .collect(Collectors.toSet());
                    LOG.info("Mapataan muut hakukohteet");
                    hakemusOidJaHakijanMuutHakutoiveOids = hakemukset
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            Hakemus::getOid,
                                            h -> new HakuappHakemusWrapper(h)
                                                    .getHakutoiveOids().stream()
                                                    .filter(valintakoeOidsHakutoiveille::containsKey)
                                                    .filter(hakutoive -> valintakoeOidsHakutoiveille
                                                            .get(hakutoive)
                                                            .getValintakoeDTO()
                                                            .stream()
                                                            .filter(Objects::nonNull)
                                                            .filter(v -> Boolean.TRUE.equals(v.getAktiivinen()))
                                                            .filter(v -> null != v.getSelvitettyTunniste())
                                                            .anyMatch(v -> kohdeHakukohteenTunnisteet.contains(v.getSelvitettyTunniste())))
                                                    .collect(Collectors.toList())));
                } catch (Exception e) {
                    LOG.error("Muiden hakukohteiden mappauksessa tapahtui odottamaton virhe", e);
                    throw e;
                }
                LOG.info("Luodaan kirje.");
                LetterBatch letterBatch = koekutsukirjeetKomponentti.valmistaKoekutsukirjeet(hakemukset, koekutsu.getHakuOid(),
                        koekutsu.getHakukohdeOid(), hakemusOidJaHakijanMuutHakutoiveOids, koekutsu.getLetterBodyText(),
                        koekutsu.getTarjoajaOid(), koekutsu.getTag(), koekutsu.getTemplateName());
                LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
                LetterResponse batchId = viestintapalveluAsyncResource
                    .viePdfJaOdotaReferenssiObservable(letterBatch)
                    .timeout(1, MINUTES)
                    .toBlocking()
                    .toFuture()
                    .get(35L, SECONDS);
                LOG.info("### BATCHID: {} {} {} ###", batchId.getBatchId(), batchId.getStatus(), batchId.getErrors());
                LOG.info("Saatiin kirjeen seurantaId {}", batchId.getBatchId());
                prosessi.vaiheValmistui();
                if (batchId.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                    PublishSubject<String> stop = PublishSubject.create();
                    Observable
                            .interval(1, SECONDS)
                            .take((int) (ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis() / 1000))
                            .takeUntil(stop)
                            .subscribe(
                                    pulse -> {
                                        try {
                                            LOG.warn("Tehdaan status kutsu seurantaId:lle {}", batchId);
                                            LetterBatchStatusDto status = viestintapalveluAsyncResource.haeStatusObservable(batchId.getBatchId())
                                                .timeout(899, MILLISECONDS)
                                                .toBlocking().toFuture().get(900L, TimeUnit.MILLISECONDS);
                                            if ("error".equals(status.getStatus())) {
                                                LOG.error("Koekutsukirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
                                                prosessi.keskeyta();
                                                stop.onNext(null);
                                            }
                                            if ("ready".equals(status.getStatus())) {
                                                prosessi.vaiheValmistui();
                                                LOG.info("Koekutsukirjeet valmistui!");
                                                prosessi.valmistui(batchId.getBatchId());
                                                stop.onNext(null);
                                            }
                                        } catch (Exception e) {
                                            LOG.error("Statuksen haku epaonnistui", e);
                                        }
                                    },
                                    throwable -> prosessi.keskeyta(),
                                    prosessi::keskeyta);
                } else {
                    prosessi.keskeyta("Hakemuksissa oli virheitä", batchId.getErrors());
                }
            } catch (Exception e) {
                LOG.error("Virhe hakutoiveelle " + koekutsu.getHakukohdeOid(), e);
                prosessi.keskeyta();
            }
        };
    }
}
