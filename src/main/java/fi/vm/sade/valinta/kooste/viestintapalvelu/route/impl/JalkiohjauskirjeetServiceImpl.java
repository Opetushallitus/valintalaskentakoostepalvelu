package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HaunResurssit;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.filtteroiAsiointikielella;
import static java.util.concurrent.TimeUnit.MINUTES;

@Service
public class JalkiohjauskirjeetServiceImpl implements JalkiohjauskirjeService {
    private final static Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetServiceImpl.class);
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private AtaruAsyncResource ataruAsyncResource;
    private TarjontaAsyncResource tarjontaAsyncResource;
    private final KirjeetHakukohdeCache kirjeetHakukohdeCache;
    private final int pollingIntervalMillis;
    private final int viePdfTimeoutMinutes;

    @Autowired
    public JalkiohjauskirjeetServiceImpl(
        ViestintapalveluAsyncResource viestintapalveluAsyncResource,
        JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti,
        ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
        ApplicationAsyncResource applicationAsyncResource,
        AtaruAsyncResource ataruAsyncResource,
        TarjontaAsyncResource tarjontaAsyncResource,
        KirjeetHakukohdeCache kirjeetHakukohdeCache,
        @Value("${valintalaskentakoostepalvelu.jalkiohjauskirjeet.polling.interval.millis:10000}") int pollingIntervalMillis,
        @Value("${valintalaskentakoostepalvelu.jalkiohjauskirjeet.viePdf.timeout.minutes:15}") int viePdfTimeoutMinutes) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.jalkiohjauskirjeetKomponentti = jalkiohjauskirjeetKomponentti;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
        this.pollingIntervalMillis = pollingIntervalMillis;
        this.viePdfTimeoutMinutes = viePdfTimeoutMinutes;
    }

    @Override
    public void jalkiohjauskirjeetHakemuksille(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids) {
        final boolean onlyOne = hakemusOids.size() == 1;
        Observable<List<HakijaDTO>> hakijatObs;
        if(onlyOne) {
            hakijatObs = valintaTulosServiceAsyncResource.getHakijaByHakemus(jalkiohjauskirjeDTO.getHakuOid(), hakemusOids.iterator().next()).map(Collections::singletonList);
        } else {
            hakijatObs = valintaTulosServiceAsyncResource.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO.getHakuOid()).map(h -> h.getResults());
        }
        hakijatObs.subscribeOn(Schedulers.newThread())
            .subscribe(
                    hakijat -> {
                        // VIALLISET DATA POIS FILTTEROINTI
                        Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat);
                        //WHITELIST FILTTEROINTI
                        Set<String> whitelist = Sets.newHashSet(hakemusOids);
                        Collection<HakijaDTO> whitelistinJalkeen = vainHakeneetJalkiohjattavat
                                .stream()
                                .filter(h -> whitelist.contains(h.getHakemusOid()))
                                .collect(Collectors.toList());
                        muodostaKirjeet(true).accept(new JalkiohjausKirjeProsessiTuple(
                            prosessi,
                            jalkiohjauskirjeDTO,
                            () -> haeHaunResurssit(jalkiohjauskirjeDTO.getHakuOid(), whitelistinJalkeen, jalkiohjauskirjeDTO)));
                    },
                    throwable -> handleKoulutuspaikattomienHakuError(prosessi, jalkiohjauskirjeDTO, throwable));
    }

    void handleKoulutuspaikattomienHakuError(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, Throwable throwable) {
        LOG.error("Koulutuspaikattomien haku haulle " + jalkiohjauskirjeDTO.getHakuOid() + " epaonnistui!", throwable);
        prosessi.keskeyta();
    }

    @Override
    public void jalkiohjauskirjeetHaulle(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO) {
        valintaTulosServiceAsyncResource.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO.getHakuOid())
            .subscribeOn(Schedulers.newThread())
            .subscribe(
                    hakijat -> {
                        //VIALLISET DATA POIS FILTTEROINTI
                        Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat.getResults());
                        muodostaKirjeet(false).accept(new JalkiohjausKirjeProsessiTuple(
                            prosessi,
                            jalkiohjauskirjeDTO,
                            () -> haeHaunResurssit(jalkiohjauskirjeDTO.getHakuOid(), vainHakeneetJalkiohjattavat, jalkiohjauskirjeDTO)));
                    },
                    throwable -> handleKoulutuspaikattomienHakuError(prosessi, jalkiohjauskirjeDTO, throwable));
    }

    private HaunResurssit haeHaunResurssit(String hakuOid, Collection<HakijaDTO> hakijat, JalkiohjauskirjeDTO kirje) {
        List<HakemusWrapper> hakemukset = haeHakemukset(hakuOid, hakijat);
        String asiointikieli = lueAsiointikieliKirjeesta(kirje);
        return filtteroiAsiointikielella(asiointikieli, new HaunResurssit(new ArrayList<>(hakijat), hakemukset));
    }

    private List<HakemusWrapper> haeHakemukset(String hakuOid, Collection<HakijaDTO> hakijat) {
        if (hakijat.isEmpty()) {
            LOG.error("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
            throw new RuntimeException("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
        }

        List<String> hakemusOids = hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
        try {
            return tarjontaAsyncResource.haeHaku(hakuOid)
                    .flatMap(haku -> (StringUtils.isEmpty(haku.getAtaruLomakeAvain())
                            ? applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS)
                            : ataruAsyncResource.getApplicationsByOids(hakemusOids)))
                    .timeout(5, MINUTES).blockingFirst();
        } catch (Throwable e) {
            LOG.error("Hakemusten haussa oideilla tapahtui virhe!", e);
            throw new RuntimeException("Hakemusten haussa oideilla tapahtui virhe!");
        }
    }


    private String lueAsiointikieliKirjeesta(JalkiohjauskirjeDTO kirje) {
        return KieliUtil.normalisoiKielikoodi(Optional.ofNullable(StringUtils.trimToNull(kirje.getKielikoodi())).orElse(KieliUtil.SUOMI));
    }

    private Consumer<JalkiohjausKirjeProsessiTuple> muodostaKirjeet(boolean tallennaTuloksetDokumenttiPalveluun) {
        return t -> {
            KirjeProsessi prosessi = t.prosessi;
            JalkiohjauskirjeDTO kirje = t.kirje;
            Supplier<HaunResurssit> haeHaunResurssit = t.haunResurssitSupplier;
            HaunResurssit haunResurssit = haeHaunResurssit.get();
            final Map<String, MetaHakukohde> metaKohteet = getStringMetaHakukohdeMap(haunResurssit.hakijat);
            LetterBatch letterBatch = jalkiohjauskirjeetKomponentti.teeJalkiohjauskirjeet(
                    kirje.getKielikoodi(), haunResurssit.hakijat, haunResurssit.hakemukset, metaKohteet, kirje.getHakuOid(),
                    kirje.getTemplateName(), kirje.getSisalto(), kirje.getTag(), !tallennaTuloksetDokumenttiPalveluun);
            try {
                if (prosessi.isKeskeytetty()) {
                    LOG.warn("Jalkiohjauskirjeiden luonti on keskeytetty kayttajantoimesta ennen siirto viestintapalveluun!");
                    return;
                }
                LOG.info("Aloitetaan jalkiohjauskirjeiden vienti viestintäpalveluun! Kirjeita {} kpl", letterBatch.getLetters().size());
                //TODO: Muuta aidosti asynkroniseksi
                LetterResponse batchId = viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(letterBatch).timeout(viePdfTimeoutMinutes, MINUTES).blockingFirst();

                int timesToPoll = (int) (VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis() / pollingIntervalMillis);
                LOG.info(String.format("Saatiin jalkiohjauskirjeen seurantaId %s ja aloitetaan valmistumisen pollaus! " +
                        "(Timeout %s, pollausväli %s ms, pollataan %s kertaa)",
                    batchId.getBatchId(), DurationFormatUtils.formatDurationHMS(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_AIKA.toMillis()), pollingIntervalMillis, timesToPoll));

                prosessi.vaiheValmistui();
                if (batchId.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                    PublishSubject<String> stop = PublishSubject.create();
                    Observable
                            .interval(pollingIntervalMillis, TimeUnit.MILLISECONDS)
                            .take(timesToPoll)
                            .takeUntil(stop)
                            .subscribe(
                                    pulse -> {
                                        try {
                                            LOG.info("Tehdaan status kutsu seurantaId:lle {}", batchId);
                                            viestintapalveluAsyncResource.haeStatusObservable(batchId.getBatchId()).subscribe(status -> {
                                                if (prosessi.isKeskeytetty()) {
                                                    String message = "Jalkiohjauskirjeiden muodstuksen seuranta on keskeytetty kayttajantoimesta";
                                                    LOG.warn(message);
                                                    stop.onNext(message);
                                                    return;
                                                }
                                                if ("error".equals(status.getStatus())) {
                                                    String message = String.format("Jalkiohjauskirjeiden muodstuksen seuranta paattyi viestintapalvelun sisaiseen virheeseen: %s", letterBatch);
                                                    LOG.error(message);
                                                    prosessi.keskeyta();
                                                    stop.onNext(message);
                                                }
                                                if ("ready".equals(status.getStatus())) {
                                                    prosessi.vaiheValmistui();
                                                    String message = "Jalkiohjauskirjeet valmistui!";
                                                    LOG.info(message);
                                                    prosessi.valmistui(batchId.getBatchId());
                                                    stop.onNext(message);
                                                }
                                            }, e -> LOG.error("Statuksen haku epaonnistui", e));
                                        } catch (Throwable e) {
                                            LOG.error("Statuksen haku epaonnistui", e);
                                        }
                                    }, throwable -> {
                                        prosessi.keskeyta();
                                    }, () -> {
                                        prosessi.keskeyta();
                                    });
                } else {
                    prosessi.keskeyta("Hakemuksissa oli virheitä", batchId.getErrors());
                }
            } catch (Throwable e) {
                LOG.error("Virhe haulle {}" + kirje.getHakuOid(), e);
                prosessi.keskeyta();
            }
        };
    }

    private Map<String, MetaHakukohde> getStringMetaHakukohdeMap(Collection<HakijaDTO> yksikielisetHakijat) {
        final Map<String, MetaHakukohde> metaKohteet = new HashMap<>();
        for (HakijaDTO hakija : yksikielisetHakijat) {
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                try {
                    metaKohteet.putIfAbsent(hakukohdeOid, kirjeetHakukohdeCache.haeHakukohde(hakukohdeOid));
                } catch (Exception e) {
                    LOG.error("Tarjonnasta ei saatu hakukohdetta " + hakukohdeOid, e);
                    metaKohteet.put(hakukohdeOid, new MetaHakukohde("",
                            new Teksti("Hakukohde " + hakukohdeOid + " ei löydy tarjonnasta!"),
                            new Teksti("Nimetön hakukohde")));
                }
            }
        }
        return metaKohteet;
    }

    private Collection<HakijaDTO> puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(Collection<HakijaDTO> hyvaksymattomatHakijat) {
        return hyvaksymattomatHakijat.stream()
                // Filtteröidään puutteellisilla tiedoilla olevat hakijat pois
                .filter(hakija -> {
                    if (hakija == null || hakija.getHakutoiveet() == null || hakija.getHakutoiveet().isEmpty()) {
                        LOG.error("Hakija ilman hakutoiveita!");
                        return false;
                    }
                    return true;
                })
                //
                // OVT-8553 Itse itsensa peruuttaneet pois
                //
                .filter(hakija -> hakija
                        .getHakutoiveet()
                        .stream()
                        .anyMatch(hakutoive -> hakutoive.getHakutoiveenValintatapajonot()
                                .stream().noneMatch(valintatapajono -> valintatapajono.getTila() == HakemuksenTila.PERUNUT)))
                .collect(Collectors.toList());
    }

    public static class JalkiohjausKirjeProsessiTuple {
        public final KirjeProsessi prosessi;
        public final JalkiohjauskirjeDTO kirje;
        public final Supplier<HaunResurssit> haunResurssitSupplier;

        public JalkiohjausKirjeProsessiTuple(KirjeProsessi prosessi, JalkiohjauskirjeDTO kirje, Supplier<HaunResurssit> haunResurssitSupplier) {
            this.prosessi = prosessi;
            this.kirje = kirje;
            this.haunResurssitSupplier = haunResurssitSupplier;
        }
    }
}
