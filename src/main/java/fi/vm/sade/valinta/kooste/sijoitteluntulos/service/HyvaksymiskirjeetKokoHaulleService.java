package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.Hakijapalvelu;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.*;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";
    private static final String VALMIS_STATUS = "ready";
    private static final String KESKEYTETTY_STATUS = "error";
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);

    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @Autowired
    private HyvaksymiskirjeetKokoHaulleService(
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource) {
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    }

    private static Optional<TemplateDetail> etsiVakioDetail(List<TemplateHistory> t) {
        return t.stream()
                .filter(th -> VAKIOTEMPLATE.equals(th.getName()))
                .flatMap(td -> td.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                .findAny();
    }

    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti asiointikielelle {} hakemalla hyväksytyt koko haulle", hakuOid, prosessi.getAsiointikieli());

        Observable<HakijaPaginationObject> koulutuspaikallisetObs = sijoitteluAsyncResource
                .getKoulutuspaikkalliset(hakuOid)
                .doOnNext(hakijat -> LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getTotalCount()));

        Observable<List<Hakemus>> hakijatObs = koulutuspaikallisetObs
                .flatMap(hakijat -> applicationAsyncResource.getApplicationsByHakemusOids(hakijat.getResults().stream().map(HakijaDTO::getHakemusOid).collect(Collectors.toList())));

        Observable<List<HakukohdeJaResurssit>> hakukohdeJaResurssitObs = hakijatObs
                .zipWith(koulutuspaikallisetObs, (hakemukset, hakijat) -> {
                    if (!prosessi.getAsiointikieli().isPresent()) {
                        return hakukohteetOpetuskielella(hakijat, hakemukset);
                    } else {
                        return filtteroiAsiointikielella(prosessi.getAsiointikieli().get(), hakijat, hakemukset);
                    }
                })
                .take(1)
                .doOnNext(list -> prosessi.setKokonaistyo(list.size()));

        hakukohdeJaResurssitObs.subscribe(
                list -> {
                    LOG.info("Hyväksyttyjä asiointikielellä {} on yhteensä {} hakukohteessa", prosessi.getAsiointikieli(), list.size());
                    final ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue = new ConcurrentLinkedQueue<>(list);
                    final boolean onkoTarveSplitata = list.size() > 20;
                    IntStream.range(0, onkoTarveSplitata ? 2 : 1).forEach(i -> hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue));
                },
                error -> {
                    LOG.error("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun {}", hakuOid, error);
                    throw new RuntimeException(error);
                });
    }

    private List<HakukohdeJaResurssit> hakukohteetOpetuskielella(HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        return getHakukohteenResurssitHakemuksistaJaHakijoista(
                hakemukset.stream().collect(Collectors.toMap(Hakemus::getOid, h0 -> h0)),
                hakijat.getResults().stream().collect(Collectors.groupingBy(this::hakutoiveMissaHakijaOnHyvaksyttyna)));
    }

    private List<HakukohdeJaResurssit> filtteroiAsiointikielella(String asiointkieli, HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        final Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna = hakemukset
                .stream()
                .filter(h -> asiointkieli.equals(new HakemusWrapper(h).getAsiointikieli()))
                .collect(Collectors.toMap(Hakemus::getOid, h0 -> h0));

        final Set<String> oidit = hakemuksetAsiointikielellaFiltteroituna.keySet();
        List<HakijaDTO> hakijatAsiointikielellaFiltteroituna = hakijat.getResults()
                .stream()
                .filter(h -> oidit.contains(h.getHakemusOid())).collect(Collectors.toList());
        LOG.info("Saatiin haun hakemukset {} kpl ja asiointkielellä filtteröinnin jälkeen {} kpl", hakemukset.size(), hakemuksetAsiointikielellaFiltteroituna.size());

        return ImmutableList.of(new HakukohdeJaResurssit(hakijatAsiointikielellaFiltteroituna, hakemuksetAsiointikielellaFiltteroituna.values()));
    }

    private List<HakukohdeJaResurssit> getHakukohteenResurssitHakemuksistaJaHakijoista(Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna, Map<String, List<HakijaDTO>> hyvaksytytHakutoiveittain) {
        return hyvaksytytHakutoiveittain.entrySet()
                .stream()
                .map(e -> new HakukohdeJaResurssit(
                                e.getKey(),
                                e.getValue(),
                                e.getValue().stream().map(v -> hakemuksetAsiointikielellaFiltteroituna.get(v.getHakemusOid())).collect(Collectors.toList()))
                ).collect(Collectors.toList());
    }

    private String hakutoiveMissaHakijaOnHyvaksyttyna(HakijaDTO hakija) {
        return hakija.getHakutoiveet().stream()
                .flatMap(h -> h.getHakutoiveenValintatapajonot().stream().map(j -> new HakutoiveJaJono(h.getHakukohdeOid(), j)))
                .filter(hjj -> hjj.jono.getTila() != null && hjj.jono.getTila().isHyvaksytty())
                .findAny()
                .map(hjj -> hjj.hakukohdeOid)
                .get(); // jos heittää npe:n niin sijoittelu palauttaa hyväksymättömiä rajapinnan läpi
    }

    private void hakukohdeKerralla(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue, ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue) {
        Optional<HakukohdeJaResurssit> hakukohdeJaResurssit = Optional.ofNullable(hakukohdeQueue.poll());
        hakukohdeJaResurssit.ifPresent(
                resurssit -> {
                    LOG.info("Aloitetaan hakukohteen {} hyväksymiskirjeiden luonti, jäljellä {} hakukohdetta", resurssit.hakukohdeOid, hakukohdeQueue.size());

                    Observable.amb(
                            getHakukohteenHyvaksymiskirjeObservable(
                                    hakuOid,
                                    resurssit.hakukohdeOid,
                                    defaultValue,
                                    prosessi.getAsiointikieli(),
                                    resurssit.hakijat,
                                    resurssit.hakemukset, prosessi),
                            Observable.timer(getDelay(hakukohdeJaResurssit.get().hakukohdeOid), TimeUnit.MINUTES)
                    ).subscribe(
                            s -> {
                                LOG.info("Hakukohde {} valmis", resurssit.hakukohdeOid);
                                prosessi.inkrementoi();
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            },
                            e -> {
                                LOG.info("Hakukohde {} ohitettu", resurssit.hakukohdeOid, e);
                                prosessi.inkrementoi();
                                prosessi.getVaroitukset().add(new Varoitus(resurssit.hakukohdeOid.orElse(hakuOid), e.getMessage()));
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            },
                            () -> {

                            }
                    );
                });
        if (!hakukohdeJaResurssit.isPresent()) {
            LOG.info("### Hyväksymiskirjeiden generointi haulle {} on valmis", hakuOid);

        }
    }

    private Long getDelay(Optional<String> hakukohdeOid) {
        return hakukohdeOid.map(h -> 3L).orElse(780L);
    }

    private Observable<?> getHakukohteenHyvaksymiskirjeObservable(String hakuOid, Optional<String> hakukohdeOid, Optional<String> defaultValue, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat, Collection<Hakemus> hakemukset,
                                                                  SijoittelunTulosProsessi prosessi) {
        if (!hakukohdeOid.isPresent()) {
            return luoKirjeJaLahetaMuodostettavaksi(hakuOid, Optional.empty(), Optional.empty(), asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue.get(), prosessi);
        } else {
            return
                    tarjontaAsyncResource.haeHakukohde(hakukohdeOid.get()).switchMap(
                            h -> {
                                try {
                                    String tarjoajaOid = h.getTarjoajaOids().iterator().next();
                                    String kieli;
                                    if (asiointikieli.isPresent()) {
                                        kieli = asiointikieli.get();
                                    } else {
                                        kieli = KirjeetHakukohdeCache.getOpetuskieli(h.getOpetusKielet());
                                    }
                                    if (defaultValue.isPresent()) {
                                        return luoKirjeJaLahetaMuodostettavaksi(hakuOid, hakukohdeOid, Optional.of(tarjoajaOid),
                                                asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue.get(), prosessi);
                                    } else {
                                        return viestintapalveluAsyncResource.haeKirjepohja(hakuOid, tarjoajaOid, "hyvaksymiskirje", kieli, hakukohdeOid.get()).switchMap(
                                                (t) -> {
                                                    Optional<TemplateDetail> td = etsiVakioDetail(t);
                                                    if (!td.isPresent()) {
                                                        return Observable.error(new RuntimeException("Ei " + VAKIOTEMPLATE + " tai " + VAKIODETAIL + " templateDetailia hakukohteelle " + hakukohdeOid));
                                                    } else {
                                                        return luoKirjeJaLahetaMuodostettavaksi(hakuOid, hakukohdeOid, Optional.of(tarjoajaOid),
                                                                asiointikieli, hyvaksytytHakijat, hakemukset, td.get().getDefaultValue(), prosessi);
                                                    }
                                                });
                                    }
                                } catch (Throwable e) {
                                    return Observable.error(e);
                                }
                            });
        }
    }

    private Observable<?> luoKirjeJaLahetaMuodostettavaksi(String hakuOid, Optional<String> hakukohdeOid, Optional<String> tarjoajaOid, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat, Collection<Hakemus> hakemukset, String defaultValue,
                                                           SijoittelunTulosProsessi prosessi) {

        try {
            LOG.info("##### Saatiin hakemukset hakukohteelle {}", hakukohdeOid);
            Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hyvaksytytHakijat);

            Observable<Map<String, Optional<Osoite>>> addresses = addresses(hakukohdeOid, tarjoajaOid, hyvaksymiskirjeessaKaytetytHakukohteet);

            Observable<LetterBatch> hyvaksymiskirje = addresses.map(hakijapalveluidenOsoite -> hyvaksymiskirjeetKomponentti
                    .teeHyvaksymiskirjeet(
                            HyvaksymiskirjeetServiceImpl.todellisenJonosijanRatkaisin(hyvaksytytHakijat),
                            hakijapalveluidenOsoite,
                            hyvaksymiskirjeessaKaytetytHakukohteet,
                            hyvaksytytHakijat,
                            hakemukset,
                            hakuOid,
                            asiointikieli,
                            //
                            defaultValue,
                            hakuOid, // nimiUriToTag(h.getHakukohteenNimiUri(), hakukohdeOid.get());
                            "hyvaksymiskirje",
                            null,
                            null,
                            asiointikieli.isPresent()));

            LOG.info("##### Tehdään viestintäpalvelukutsu {}", hakukohdeOid);

            Observable<ResponseWithBatchId> valmisOrKeskeytettyObs = hyvaksymiskirje.flatMap(l -> pollDocumentStatus(hakukohdeOid, viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(l)));
            Observable<String> keskeytettyObs = processInterruptedDocument(hakukohdeOid, valmisOrKeskeytettyObs);
            Observable<String> valmisBatchIdObs = processReadyDocument(hakukohdeOid, prosessi, valmisOrKeskeytettyObs);

            return valmisBatchIdObs.mergeWith(keskeytettyObs);

        } catch (Throwable error) {
            LOG.error("Viestintäpalveluviestin muodostus epäonnistui hakukohteelle {}", hakukohdeOid, error);
            return Observable.error(error);

        }
    }

    private Observable<Map<String, Optional<Osoite>>> addresses(Optional<String> hakukohdeOid, Optional<String> tarjoajaOid, Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet) {
        if (hakukohdeOid.isPresent()) {
            MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid.get());
            return organisaatioAsyncResource.haeHakutoimisto(tarjoajaOid.get()).map(hakutoimistoDTO -> ImmutableMap.of(
                    tarjoajaOid.get(), Hakijapalvelu.osoite(hakutoimistoDTO, kohdeHakukohde.getHakukohteenKieli())
            ));
        } else { // koko haun kiinnostaville hakukohteille kerralla
            return Observable.from(hyvaksymiskirjeessaKaytetytHakukohteet.values())
                    .flatMap(meta -> organisaatioAsyncResource.haeHakutoimisto(meta.getTarjoajaOid())
                            .map(hakutoimisto -> new TarjoajaWithOsoite(meta.getTarjoajaOid(), Hakijapalvelu.osoite(hakutoimisto, meta.getHakukohteenKieli()))))
                    .collect(HashMap::new, (map, pair) -> map.put(pair.tarjoajaOid, pair.hakutoimisto));
        }
    }

    private Observable<ResponseWithBatchId> pollDocumentStatus(Optional<String> hakukohdeOid, Observable<LetterResponse> letterResponseObs) {
        return letterResponseObs
                .doOnNext(letterResponse -> {
                    LOG.info("##### Viestintäpalvelukutsu onnistui {}", hakukohdeOid);
                    LOG.info("##### Odotetaan statusta, batchid={}", letterResponse.getBatchId());
                })
                .flatMap(letterResponse -> Observable.interval(1, TimeUnit.SECONDS)
                        .take((int) TimeUnit.MINUTES.toSeconds(getDelay(hakukohdeOid)))
                        .flatMap(i -> viestintapalveluAsyncResource.haeStatusObservable(letterResponse.getBatchId())
                                .zipWith(Observable.just(letterResponse.getBatchId()), ResponseWithBatchId::new))
                        .skipWhile(status -> !VALMIS_STATUS.equals(status.resp.getStatus()) && !KESKEYTETTY_STATUS.equals(status.resp.getStatus())))
                .take(1);
    }

    private Observable<String> processInterruptedDocument(Optional<String> hakukohdeOid, Observable<ResponseWithBatchId> valmisOrKeskeytettyObs) {
        return valmisOrKeskeytettyObs
                .filter(status -> KESKEYTETTY_STATUS.equals(status.resp.getStatus()))
                .map(status -> status.batchId)
                .flatMap(s -> Observable.error(new RuntimeException("Viestintäpalvelu palautti virheen hakukohteelle " + hakukohdeOid.get())));
    }

    private Observable<String> processReadyDocument(Optional<String> hakukohdeOid, SijoittelunTulosProsessi prosessi, Observable<ResponseWithBatchId> valmisOrKeskeytettyObs) {
        Observable<ResponseWithBatchId> valmisObs = valmisOrKeskeytettyObs.filter(status -> VALMIS_STATUS.equals(status.resp.getStatus()));
        if (hakukohdeOid.isPresent()) {
            return valmisObs
                    .doOnNext(s -> LOG.info("##### Dokumentti {} valmistui hakukohteelle {} joten uudelleen nimetään se", s.batchId, hakukohdeOid.get()))
                    .flatMap(s -> dokumenttiAsyncResource.uudelleenNimea(s.batchId, "hyvaksymiskirje_" + hakukohdeOid.get() + ".pdf")
                            .doOnNext(str -> LOG.info("Uudelleen nimeäminen onnistui hakukohteelle {}", hakukohdeOid.get()))
                            .doOnError(error -> LOG.error("Uudelleen nimeäminen epäonnistui hakukohteelle {}", hakukohdeOid.get(), error))
                            .onErrorReturn(error -> s.batchId)
                            .map(name -> s.batchId));
        } else {
            return valmisObs.doOnNext(s -> prosessi.setDokumenttiId(s.batchId)).map(s -> s.batchId);
        }
    }

    private static class HakutoiveJaJono {
        public final String hakukohdeOid;
        public final HakutoiveenValintatapajonoDTO jono;

        public HakutoiveJaJono(String hakukohdeOid, HakutoiveenValintatapajonoDTO jono) {
            this.hakukohdeOid = hakukohdeOid;
            this.jono = jono;
        }
    }

    private static class HakukohdeJaResurssit {
        public final Optional<String> hakukohdeOid;
        public final List<HakijaDTO> hakijat;
        public final Collection<Hakemus> hakemukset;

        public HakukohdeJaResurssit(String hakukohdeOid, List<HakijaDTO> hakijat, Collection<Hakemus> hakemukset) {
            this.hakukohdeOid = Optional.of(hakukohdeOid);
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }

        public HakukohdeJaResurssit(List<HakijaDTO> hakijat, Collection<Hakemus> hakemukset) {
            this.hakukohdeOid = Optional.empty();
            this.hakijat = hakijat;
            this.hakemukset = hakemukset;
        }
    }

    private class ResponseWithBatchId {
        public final LetterBatchStatusDto resp;
        public final String batchId;
        public ResponseWithBatchId(LetterBatchStatusDto resp, String batchId) {
            this.resp = resp;
            this.batchId = batchId;
        }
    }

    private class TarjoajaWithOsoite {

        public final String tarjoajaOid;
        public final Optional<Osoite> hakutoimisto;

        public TarjoajaWithOsoite(String tarjoajaOid, Optional<Osoite> hakutoimisto) {
            this.tarjoajaOid = tarjoajaOid;
            this.hakutoimisto = hakutoimisto;
        }
    }
}
