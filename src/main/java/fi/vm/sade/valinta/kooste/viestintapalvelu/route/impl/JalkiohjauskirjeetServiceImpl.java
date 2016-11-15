package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Sets;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Action3;
import rx.functions.Action4;
import static rx.observables.BlockingObservable.from;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.HaunResurssit;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.ViestintapalveluObservables.filtteroiAsiointikielella;

@Service
public class JalkiohjauskirjeetServiceImpl implements JalkiohjauskirjeService {
    private final static Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetServiceImpl.class);
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final KirjeetHakukohdeCache kirjeetHakukohdeCache;
    private final TarjontaAsyncResource hakuV1AsyncResource;

    @Autowired
    public JalkiohjauskirjeetServiceImpl(
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            KirjeetHakukohdeCache kirjeetHakukohdeCache,
            TarjontaAsyncResource hakuV1AsyncResource) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.jalkiohjauskirjeetKomponentti = jalkiohjauskirjeetKomponentti;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
        this.hakuV1AsyncResource = hakuV1AsyncResource;
    }

    @Override
    public void jalkiohjauskirjeetHakemuksille(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids) {
            sijoitteluAsyncResource.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO.getHakuOid())
            .subscribeOn(Schedulers.newThread())
            .subscribe(
                    hakijat -> {
                        // VIALLISET DATA POIS FILTTEROINTI
                        Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat.getResults());
                        //WHITELIST FILTTEROINTI
                        Set<String> whitelist = Sets.newHashSet(hakemusOids);
                        Collection<HakijaDTO> whitelistinJalkeen = vainHakeneetJalkiohjattavat
                                .stream()
                                .filter(h -> whitelist.contains(h.getHakemusOid()))
                                .collect(Collectors.toList());
                        muodostaKirjeet(false).call(prosessi, jalkiohjauskirjeDTO, () -> haeHaunResurssit(jalkiohjauskirjeDTO.getHakuOid(), whitelistinJalkeen, jalkiohjauskirjeDTO));
                    },
                    throwable -> handleKoulutuspaikattomienHakuError(prosessi, jalkiohjauskirjeDTO, throwable));
    }

    void handleKoulutuspaikattomienHakuError(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, Throwable throwable) {
        LOG.error("Koulutuspaikattomien haku haulle " + jalkiohjauskirjeDTO.getHakuOid() + " epaonnistui!", throwable);
        prosessi.keskeyta();
    }

    @Override
    public void jalkiohjauskirjeetHaulle(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO) {
            sijoitteluAsyncResource.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO.getHakuOid())
            .subscribeOn(Schedulers.newThread())
            .subscribe(
                    hakijat -> {
                        //VIALLISET DATA POIS FILTTEROINTI
                        Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat.getResults());
                        muodostaKirjeet(isKorkeakouluhaku(jalkiohjauskirjeDTO.getHakuOid())).call(prosessi, jalkiohjauskirjeDTO, () -> haeHaunResurssit(jalkiohjauskirjeDTO.getHakuOid(), vainHakeneetJalkiohjattavat, jalkiohjauskirjeDTO));
                    },
                    throwable -> handleKoulutuspaikattomienHakuError(prosessi, jalkiohjauskirjeDTO, throwable));
    }

    private HaunResurssit haeHaunResurssit(String hakuOid, Collection<HakijaDTO> hakijat, JalkiohjauskirjeDTO kirje) {
        List<Hakemus> hakemukset = haeHakemukset(hakuOid, hakijat);
        String asiointikieli = lueAsiointikieliKirjeesta(kirje);
        return filtteroiAsiointikielella(asiointikieli, new HaunResurssit(new ArrayList<>(hakijat), hakemukset));
    }

    private List<Hakemus> haeHakemukset(String hakuOid, Collection<HakijaDTO> hakijat) {
        if (hakijat.isEmpty()) {
            LOG.error("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
            throw new RuntimeException("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
        }

        List<String> hakemusOids = hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
        try {
            return applicationAsyncResource.getApplicationsByhakemusOidsInParts(hakuOid, hakemusOids, ApplicationAsyncResource.DEFAULT_KEYS);
        } catch (Throwable e) {
            LOG.error("Hakemusten haussa oideilla tapahtui virhe!", e);
            throw new RuntimeException("Hakemusten haussa oideilla tapahtui virhe!");
        }
    }


    private String lueAsiointikieliKirjeesta(JalkiohjauskirjeDTO kirje) {
        return KieliUtil.normalisoiKielikoodi(Optional.ofNullable(StringUtils.trimToNull(kirje.getKielikoodi())).orElse(KieliUtil.SUOMI));
    }

    private boolean isKorkeakouluhaku(String hakuOid) {
        //TODO: tee aidosti asynkroniseksi
        String haunKohdejoukkoUri = from(hakuV1AsyncResource.haeHaku(hakuOid)).first().getKohdejoukkoUri();
        return haunKohdejoukkoUri.startsWith("haunkohdejoukko_12"); //"kohdejoukkoUri": "haunkohdejoukko_12#1"
    }

    private Action3<KirjeProsessi, JalkiohjauskirjeDTO, Supplier<HaunResurssit>> muodostaKirjeet(boolean sahkoinenKorkeakoulunMassaposti) {
        return (prosessi, kirje, haeHaunResurssit) -> {
            HaunResurssit haunResurssit = haeHaunResurssit.get();
            final Map<String, MetaHakukohde> metaKohteet = getStringMetaHakukohdeMap(haunResurssit.hakijat);
            LetterBatch letterBatch = jalkiohjauskirjeetKomponentti.teeJalkiohjauskirjeet(
                    kirje.getKielikoodi(), haunResurssit.hakijat, haunResurssit.hakemukset, metaKohteet, kirje.getHakuOid(),
                    kirje.getTemplateName(), kirje.getSisalto(), kirje.getTag(), sahkoinenKorkeakoulunMassaposti);
            try {
                if (prosessi.isKeskeytetty()) {
                    LOG.error("Jalkiohjauskirjeiden luonti on keskeytetty kayttajantoimesta! (Timeout 30min)");
                    return;
                }
                LOG.error("Aloitetaan jalkiohjauskirjeiden vienti! Kirjeita {}kpl", letterBatch.getLetters().size());
                //TODO: Muuta aidosti asynkroniseksi
                LetterResponse batchId = from(viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(letterBatch)).first();
                LOG.error("Saatiin jalkiohjauskirjeen seurantaId {} ja aloitetaan valmistumisen pollaus! (Timeout 60min)", batchId.getBatchId());
                prosessi.vaiheValmistui();
                if (batchId.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
                    LOG.error("############ Kirjeiden status ok #############");
                    PublishSubject<String> stop = PublishSubject.create();
                    Observable
                            .interval(1, TimeUnit.SECONDS)
                            .take(ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA)
                            .takeUntil(stop)
                            .subscribe(
                                    pulse -> {
                                        try {
                                            LOG.warn("Tehdaan status kutsu seurantaId:lle {}", batchId);
                                            //TODO: Muuta aidosti asynkroniseksi
                                            LetterBatchStatusDto status = from(viestintapalveluAsyncResource.haeStatusObservable(batchId.getBatchId())).first();
                                            if (prosessi.isKeskeytetty()) {
                                                LOG.error("Jalkiohjauskirjeiden luonti on keskeytetty kayttajantoimesta!");
                                                stop.onNext(null);
                                                return;
                                            }
                                            if ("error".equals(status.getStatus())) {
                                                LOG.error("Jalkiohjauskirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
                                                prosessi.keskeyta();
                                                stop.onNext(null);
                                            }
                                            if ("ready".equals(status.getStatus())) {
                                                prosessi.vaiheValmistui();
                                                LOG.error("Jalkiohjauskirjeet valmistui!");
                                                prosessi.valmistui(batchId.getBatchId());
                                                stop.onNext(null);
                                            }
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
}
