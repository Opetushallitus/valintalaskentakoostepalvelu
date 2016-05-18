package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static rx.Observable.from;
import com.google.common.collect.Sets;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
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
import rx.functions.Action4;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class JalkiohjauskirjeetServiceImpl implements JalkiohjauskirjeService {
    private final static Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetServiceImpl.class);
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final KirjeetHakukohdeCache kirjeetHakukohdeCache;

    @Autowired
    public JalkiohjauskirjeetServiceImpl(
            ViestintapalveluAsyncResource viestintapalveluAsyncResource,
            JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            KirjeetHakukohdeCache kirjeetHakukohdeCache) {
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.jalkiohjauskirjeetKomponentti = jalkiohjauskirjeetKomponentti;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
    }

    @Override
    public void jalkiohjauskirjeetHakemuksille(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids) {
        from(
                sijoitteluAsyncResource.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO.getHakuOid()))
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
                            muodostaKirjeet().call(hakijat, whitelistinJalkeen, prosessi, jalkiohjauskirjeDTO);
                        },
                        throwable -> handleKoulutuspaikattomienHakuError(prosessi, jalkiohjauskirjeDTO, throwable));
    }

    void handleKoulutuspaikattomienHakuError(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO, Throwable throwable) {
        LOG.error("Koulutuspaikattomien haku haulle " + jalkiohjauskirjeDTO.getHakuOid() + " epaonnistui!", throwable);
        prosessi.keskeyta();
    }

    @Override
    public void jalkiohjauskirjeetHaulle(KirjeProsessi prosessi, JalkiohjauskirjeDTO jalkiohjauskirjeDTO) {
        from(
                sijoitteluAsyncResource.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO.getHakuOid()))
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        hakijat -> {
                            //VIALLISET DATA POIS FILTTEROINTI
                            Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat.getResults());
                            muodostaKirjeet().call(hakijat, vainHakeneetJalkiohjattavat, prosessi, jalkiohjauskirjeDTO);
                        },
                        throwable -> handleKoulutuspaikattomienHakuError(prosessi, jalkiohjauskirjeDTO, throwable));
    }

    private Action4<HakijaPaginationObject, Collection<HakijaDTO>, KirjeProsessi, JalkiohjauskirjeDTO> muodostaKirjeet() {
        return (kaikkiHakijat, hakijat, prosessi, kirje) -> {
            if (hakijat.isEmpty()) {
                LOG.error("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
                throw new RuntimeException("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
            }

            List<Hakemus> hakemukset;
            Collection<String> hakemusOids = hakijat.stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList());
            try {
                LOG.info("Haetaan hakemukset!");
                hakemukset = applicationAsyncResource.getApplicationsByOids(hakemusOids).get(240L, TimeUnit.MINUTES);
            } catch (Throwable e) {
                LOG.error("Hakemusten haussa oideilla tapahtui virhe!", e);
                throw new RuntimeException("Hakemusten haussa oideilla tapahtui virhe!");
            }
            String kieli = KieliUtil.normalisoiKielikoodi(Optional.ofNullable(StringUtils.trimToNull(kirje.getKielikoodi())).orElse(KieliUtil.SUOMI));
            Collection<Hakemus> yksikielisetHakemukset = hakemukset.stream()
                    .filter(h -> kieli.equals(new HakemusWrapper(h).getAsiointikieli()))
                    .collect(Collectors.toList());
            Set<String> yksikielisetHakemusOids = yksikielisetHakemukset.stream().map(Hakemus::getOid).collect(Collectors.toSet());
            Collection<HakijaDTO> yksikielisetHakijat = hakijat.stream()
                    .filter(h -> yksikielisetHakemusOids.contains(h.getHakemusOid()))
                    .collect(Collectors.toList());
            final Map<String, MetaHakukohde> metaKohteet = getStringMetaHakukohdeMap(yksikielisetHakijat);
            LetterBatch letterBatch = jalkiohjauskirjeetKomponentti.teeJalkiohjauskirjeet(kirje.getKielikoodi(), yksikielisetHakijat,
                    yksikielisetHakemukset, metaKohteet, kirje.getHakuOid(), kirje.getTemplateName(), kirje.getSisalto(), kirje.getTag());
            try {
                if (prosessi.isKeskeytetty()) {
                    LOG.error("Jalkiohjauskirjeiden luonti on keskeytetty kayttajantoimesta! (Timeout 30min)");
                    return;
                }
                LOG.error("Aloitetaan jalkiohjauskirjeiden vienti! Kirjeita {}kpl", letterBatch.getLetters().size());
                LetterResponse batchId = viestintapalveluAsyncResource.viePdfJaOdotaReferenssi(letterBatch).get(240L, TimeUnit.MINUTES);
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
                                            LetterBatchStatusDto status = viestintapalveluAsyncResource.haeStatus(batchId.getBatchId()).get(10000L, TimeUnit.MILLISECONDS);
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
