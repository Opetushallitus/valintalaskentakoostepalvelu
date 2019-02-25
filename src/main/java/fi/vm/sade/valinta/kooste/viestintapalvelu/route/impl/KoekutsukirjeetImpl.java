package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.functions.Consumer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.*;
import static io.reactivex.Observable.zip;

@Service
public class KoekutsukirjeetImpl implements KoekutsukirjeetService {
    private static final Logger LOG = LoggerFactory
            .getLogger(KoekutsukirjeetImpl.class);
    private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final ValintaperusteetAsyncResource valintakoeResource;
    private final ValintalaskentaValintakoeAsyncResource osallistumisetResource;

    @Autowired
    public KoekutsukirjeetImpl(
            KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            AtaruAsyncResource ataruAsyncResource, ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetValintakoeAsyncResource,
            ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.valintakoeResource = valintaperusteetValintakoeAsyncResource;
        this.osallistumisetResource = valintalaskentaValintakoeAsyncResource;
    }

    @Override
    public void koekutsukirjeetHakemuksille(KirjeProsessi prosessi, KoekutsuDTO koekutsu, Collection<String> hakemusOids) {
        ((StringUtils.isEmpty(koekutsu.getHaku().getAtaruLomakeAvain()))
                ? applicationAsyncResource.getApplicationsByHakemusOids(Lists.newArrayList(hakemusOids))
                : ataruAsyncResource.getApplicationsByOids(Lists.newArrayList(hakemusOids)))
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
        final Observable<List<HakemusWrapper>> hakemuksetObservable = ((StringUtils.isEmpty(koekutsu.getHaku().getAtaruLomakeAvain()))
                ? applicationAsyncResource.getApplicationsByOid(koekutsu.getHaku().getOid(), koekutsu.getHakukohdeOid())
                : ataruAsyncResource.getApplicationsByHakukohde(koekutsu.getHakukohdeOid()));

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
                            Stream<HakemusWrapper> hakukohteenUlkopuolisetHakemukset = getHakukohteenUlkopuolisetHakemukset(hakemukset, osallistujienHakemusOidit, koekutsu.getHaku());
                            // vain hakukohteen osallistujat
                            List<HakemusWrapper> lopullinenHakemusJoukko = Stream.concat(hakukohteenUlkopuolisetHakemukset,
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

    private Stream<HakemusWrapper> getHakukohteenUlkopuolisetHakemukset(List<HakemusWrapper> hakemukset, Set<String> osallistujienHakemusOidit, HakuV1RDTO haku) {
        Set<String> hakemusOids = hakemukset.stream().map(HakemusWrapper::getOid).collect(Collectors.toSet());
        Set<String> hakukohteenUlkopuolisetKoekutsuttavat = Sets.newHashSet(osallistujienHakemusOidit);
        hakukohteenUlkopuolisetKoekutsuttavat.removeIf(hakemusOids::contains);
        if (!hakukohteenUlkopuolisetKoekutsuttavat.isEmpty()) {
            return ((StringUtils.isEmpty(haku.getAtaruLomakeAvain()))
                    ? applicationAsyncResource.getApplicationsByHakemusOids(Lists.newArrayList(hakukohteenUlkopuolisetKoekutsuttavat))
                    : ataruAsyncResource.getApplicationsByOids(Lists.newArrayList(hakukohteenUlkopuolisetKoekutsuttavat)))
                    .timeout(30, SECONDS)
                    .blockingFirst()
                    .stream();
        } else {
            return Stream.empty();
        }
    }

    private Consumer<List<HakemusWrapper>> koekutsukirjeiksi(final KirjeProsessi prosessi, final KoekutsuDTO koekutsu) {
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
                            .flatMap(h -> h.getHakutoiveOids().stream()).collect(Collectors.toSet()));
                    hakutoiveetKaikistaHakemuksista.add(koekutsu.getHakukohdeOid());
                    LOG.info("Hakutoiveet hakemuksista:\r\n{}", Arrays.toString(hakutoiveetKaikistaHakemuksista.toArray()));
                    valintakoeOidsHakutoiveille = valintakoeResource
                            .haeValintakokeetHakukohteille(hakutoiveetKaikistaHakemuksista)
                            .timeout(1, HOURS)
                            .blockingFirst()
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
                                            HakemusWrapper::getOid,
                                            h -> h.getHakutoiveOids().stream()
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
                LetterBatch letterBatch = koekutsukirjeetKomponentti.valmistaKoekutsukirjeet(hakemukset, koekutsu.getHaku().getOid(),
                        koekutsu.getHakukohdeOid(), hakemusOidJaHakijanMuutHakutoiveOids, koekutsu.getLetterBodyText(),
                        koekutsu.getTarjoajaOid(), koekutsu.getTag(), koekutsu.getTemplateName());
                LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
                LetterResponse batchId = viestintapalveluAsyncResource
                    .viePdfJaOdotaReferenssiObservable(letterBatch)
                    .timeout(1, MINUTES)
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
                                                .toFuture().get(900L, TimeUnit.MILLISECONDS);
                                            if ("error".equals(status.getStatus())) {
                                                String msg = "Koekutsukirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!";
                                                LOG.error(msg);
                                                prosessi.keskeyta();
                                                stop.onNext(msg);
                                            }
                                            if ("ready".equals(status.getStatus())) {
                                                prosessi.vaiheValmistui();
                                                String msg = "Koekutsukirjeet valmistui!";
                                                LOG.info(msg);
                                                prosessi.valmistui(batchId.getBatchId());
                                                stop.onNext(msg);
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
