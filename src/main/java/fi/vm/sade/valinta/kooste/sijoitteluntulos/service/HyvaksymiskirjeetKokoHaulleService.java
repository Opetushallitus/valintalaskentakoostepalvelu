package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import com.google.common.collect.Maps;
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
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.OsoiteHaku;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;

@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";
    private static final String VALMIS_STATUS = "ready";
    private static final String KESKEYTETTY_STATUS = "error";
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);

    private final HaeOsoiteKomponentti haeOsoiteKomponentti;
    private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final DokumenttiAsyncResource dokumenttiAsyncResource;
    private final OrganisaatioAsyncResource organisaatioAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @Autowired
    private HyvaksymiskirjeetKokoHaulleService(
            HaeOsoiteKomponentti haeOsoiteKomponentti,
            HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            DokumenttiAsyncResource dokumenttiAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ViestintapalveluAsyncResource viestintapalveluAsyncResource) {
        this.haeOsoiteKomponentti = haeOsoiteKomponentti;
        this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
        this.applicationAsyncResource = applicationAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
    }

    private static String nimiUriToTag(String nimiUri, String deflt) {
        return Optional.ofNullable(nimiUri).map(
                n -> n.split("#")[0]//.split("#")[0]
        ).orElse(deflt);
    }

    private static Optional<TemplateDetail> etsiVakioDetail(List<TemplateHistory> t) {
        Stream<TemplateHistory> vainVakioHistoriat = t.stream().filter(th -> VAKIOTEMPLATE.equals(th.getName()));
        Optional<TemplateDetail> o =
                vainVakioHistoriat
                        .flatMap(td -> td.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                        .findAny();
        return o;
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action0 doFinally) {
        return subscribeWithFinally(f -> {
        }, f -> {
        }, () -> {
        }, doFinally);
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action1<T> doNext, Action1<Throwable> doError, Action0 doFinally) {
        return subscribeWithFinally(doNext, doError, () -> {
        }, doFinally);
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action1<T> doNext, Action1<Throwable> doError, Action0 doComplete, Action0 doFinally) {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                try {
                    doComplete.call();
                } finally {
                    try {
                        doFinally.call();
                    } catch (Throwable t) {
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    doError.call(e);
                } finally {
                    try {
                        doFinally.call();
                    } catch (Throwable t) {
                    }
                }
            }

            @Override
            public void onNext(T t) {
                try {
                    doNext.call(t);
                } finally {
                    try {
                        doFinally.call();
                    } catch (Throwable tt) {
                    }
                }
            }
        };
    }

    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue) {
        LOG.info("Aloitetaan haun {} hyväksymiskirjeiden luonti asiointikielelle {} hakemalla hyväksytyt koko haulle", hakuOid, prosessi.getAsiointikieli());
        sijoitteluAsyncResource.getKoulutuspaikkalliset(hakuOid)
                .switchMap(
                        hakijat -> {
                            LOG.info("Saatiin haulle hyväksyttyjä {} kpl", hakijat.getTotalCount());
                            Observable<List<Hakemus>> hakemuksetObs = applicationAsyncResource.getApplicationsByHakemusOids(hakijat.getResults().stream().map(h -> h.getHakemusOid()).collect(Collectors.toList()));
                            if (!prosessi.getAsiointikieli().isPresent()) {
                                return hakemuksetObs.switchMap(
                                        hakemukset -> hakukohteetOpetuskielella(hakijat, hakemukset)
                                );
                            }
                            return hakemuksetObs.switchMap(hakemukset -> filtteroiAsiointikielella(prosessi.getAsiointikieli().get(), hakijat, hakemukset));
                        }
                ).subscribe(
                success -> {
                    LOG.info("Hyväksyttyjä asiointikielellä {} on yhteensä {} hakukohteessa", prosessi.getAsiointikieli(), success.size());
                    prosessi.setKokonaistyo(success.size());
                    final ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue = new ConcurrentLinkedQueue<>(success);
                    final boolean onkoTarveSplitata = success.size() > 20;
                    IntStream.range(0, onkoTarveSplitata ? 2 : 1).forEach(i -> hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue));
                },
                error -> {
                    LOG.error("Ei saatu hakukohteen resursseja massahyväksymiskirjeitä varten hakuun {}", hakuOid, error);
                }
        );
    }

    private Observable<List<HakukohdeJaResurssit>> hakukohteetOpetuskielella(HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        return wrapAsRunOnlyOnceObservable(Observable.create(
                onSub -> {
                    onSub.onNext(getHakukohteenResurssitHakemuksistaJaHakijoista(
                            hakemukset.stream().collect(Collectors.toMap(Hakemus::getOid, h0 -> h0)),
                            hakijat.getResults().stream().collect(Collectors.groupingBy(this::hakutoiveMissaHakijaOnHyvaksyttyna))));
                }
        ));
    }

    private Observable<List<HakukohdeJaResurssit>> filtteroiAsiointikielella(String asiointkieli, HakijaPaginationObject hakijat, List<Hakemus> hakemukset) {
        final Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna
                = hakemukset.stream().filter(
                h -> asiointkieli.equals(new HakemusWrapper(h).getAsiointikieli()))
                .collect(Collectors.toMap(Hakemus::getOid, h0 -> h0));

        List<HakijaDTO> hakijatAsiointikielellaFiltteroituna;
        {
            final Set<String> oidit =  hakemuksetAsiointikielellaFiltteroituna.keySet();
            hakijatAsiointikielellaFiltteroituna=hakijat.getResults().stream().filter(
                h -> oidit.contains(((HakijaDTO)h).getHakemusOid())).collect(Collectors.toList());
        }
        LOG.info("Saatiin haun hakemukset {} kpl ja asiointkielellä filtteröinnin jälkeen {} kpl", hakemukset.size(), hakemuksetAsiointikielellaFiltteroituna.size());


        return wrapAsRunOnlyOnceObservable(Observable.create(
                onSub -> {
                    onSub.onNext(
                            Arrays.asList(new HakukohdeJaResurssit(hakijatAsiointikielellaFiltteroituna, hakemuksetAsiointikielellaFiltteroituna.values())
                            ));
                }
        ));
    }

    private List<HakukohdeJaResurssit> getHakukohteenResurssitHakemuksistaJaHakijoista(Map<String, Hakemus> hakemuksetAsiointikielellaFiltteroituna, Map<String, List<HakijaDTO>> hyvaksytytHakutoiveittain) {
        return hyvaksytytHakutoiveittain.entrySet().stream()
                .map(e -> new HakukohdeJaResurssit(e.getKey(), e.getValue(),
                        e.getValue().stream().map(v -> hakemuksetAsiointikielellaFiltteroituna.get(v.getHakemusOid())).collect(Collectors.toList())
                )).collect(Collectors.toList());
    }

    private String hakutoiveMissaHakijaOnHyvaksyttyna(HakijaDTO hakija) {
        return hakija.getHakutoiveet().stream()
                .flatMap(h -> h.getHakutoiveenValintatapajonot().stream().map(j -> new HakutoiveJaJono(h.getHakukohdeOid(),j)))
                .filter(hjj -> hjj.jono.getTila() != null && hjj.jono.getTila().isHyvaksytty())
                .findAny()
                .map(hjj -> hjj.hakukohdeOid)
                .get(); // jos heittää npe:n niin sijoittelu palauttaa hyväksymättömiä rajapinnan läpi
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

    private void hakukohdeKerralla(String hakuOid, SijoittelunTulosProsessi prosessi, Optional<String> defaultValue, ConcurrentLinkedQueue<HakukohdeJaResurssit> hakukohdeQueue) {
        Optional<HakukohdeJaResurssit> hakukohdeJaResurssit = Optional.ofNullable(hakukohdeQueue.poll());
        hakukohdeJaResurssit.ifPresent(
                resurssit -> {
                    LOG.info("Aloitetaan hakukohteen {} hyväksymiskirjeiden luonti jäljellä {} hakukohdetta", resurssit.hakukohdeOid, hakukohdeQueue.size());

                    Observable.amb(
                            getHakukohteenHyvaksymiskirjeObservable(
                                    hakuOid,
                                    resurssit.hakukohdeOid,
                                    defaultValue,
                                    prosessi.getAsiointikieli(),
                                    resurssit.hakijat,
                                    resurssit.hakemukset,prosessi),
                            Observable.timer(getDelay(hakukohdeJaResurssit.get().hakukohdeOid), TimeUnit.MINUTES)
                    ).subscribe(
                            s -> {
                                LOG.error("Hakukohde {} valmis", resurssit.hakukohdeOid);
                                prosessi.inkrementoi();
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            },
                            e -> {
                                LOG.error("Hakukohde {} ohitettu", resurssit.hakukohdeOid, e);
                                prosessi.inkrementoi();
                                prosessi.getVaroitukset().add(new Varoitus(resurssit.hakukohdeOid.orElse(hakuOid), e.getMessage()));
                                hakukohdeKerralla(hakuOid, prosessi, defaultValue, hakukohdeQueue);
                            },
                            () -> {

                            }
                    );
                });
        if (!hakukohdeJaResurssit.isPresent()) {
            LOG.error("### Hyväksymiskirjeiden generointi haulle {} on valmis", hakuOid);

        }
    }

    private Long getDelay(Optional<String> hakukohdeOid) {
        return hakukohdeOid.map(h -> 3L).orElse(780L);
    }

    private Observable<?> getHakukohteenHyvaksymiskirjeObservable(String hakuOid, Optional<String> hakukohdeOid, Optional<String> defaultValue, Optional<String> asiointikieli, List<HakijaDTO> hyvaksytytHakijat, Collection<Hakemus> hakemukset,
                                                                  SijoittelunTulosProsessi prosessi) {
        if(!hakukohdeOid.isPresent()) {
            return luoKirjeJaLahetaMuodostettavaksi(hakuOid, Optional.empty(), Optional.empty(),
                    asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue.get(),prosessi);
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
                                                asiointikieli, hyvaksytytHakijat, hakemukset, defaultValue.get(),prosessi);
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
            Map<String, Optional<Osoite>> hakijapalveluidenOsoite = Maps.newHashMap();
            if(hakukohdeOid.isPresent()) {
                MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid.get());
                Future<Response> organisaatioFuture = organisaatioAsyncResource.haeOrganisaatio(tarjoajaOid.get());
                hakijapalveluidenOsoite.put(tarjoajaOid.get(), Optional.ofNullable(OsoiteHaku.organisaatioResponseToHakijapalveluidenOsoite(
                        haeOsoiteKomponentti, organisaatioAsyncResource,
                        newArrayList(Arrays.asList(tarjoajaOid.get())),
                        kohdeHakukohde.getHakukohteenKieli(),
                        organisaatioFuture.get())));
            } else { // koko haun kiinnostaville hakukohteille kerralla
                hakijapalveluidenOsoite =
                hyvaksymiskirjeessaKaytetytHakukohteet.values()
                        .stream().map(m -> m.getTarjoajaOid())
                        .collect(Collectors.toSet()) // koska sama tarjoaja voi olla useammalla hakukohteella
                        .stream()
                        .collect(Collectors.toMap(t -> t, t -> {
                            try {
                                return Optional.ofNullable(OsoiteHaku.organisaatioResponseToHakijapalveluidenOsoite(
                                        haeOsoiteKomponentti, organisaatioAsyncResource,
                                        newArrayList(Arrays.asList(t)),
                                        asiointikieli.get(),
                                        organisaatioAsyncResource.haeOrganisaatio(t).get()));
                            } catch (Throwable t0) {
                                LOG.error("Tarjoajalle {} ei saatu hakijapalveluiden osoitetta", t, t0);
                                return Optional.empty();
                            }
                        }));

            }
            LetterBatch l = hyvaksymiskirjeetKomponentti
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
                            asiointikieli.isPresent());


            LOG.info("##### Tehdään viestintäpalvelukutsu {}", hakukohdeOid);
            //LOG.error("{}", new Gson().toJson(l));
            return viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(l)
                    .switchMap(
                            letterResponse -> {
                                LOG.info("##### Viestintäpalvelukutsu onnistui {}", hakukohdeOid);
                                final String batchId = letterResponse.getBatchId();
                                LOG.info("##### Odotetaan statusta... BatchId={}", batchId);
                                PublishSubject<String> stop = PublishSubject.create();

                                final Observable<String> plainObservable = wrapAsRunOnlyOnceObservable(Observable.create(subscriber -> {
                                    Observable
                                            .interval(1, TimeUnit.SECONDS)
                                            .take((int) TimeUnit.MINUTES.toSeconds(getDelay(hakukohdeOid)))
                                            .takeUntil(stop)
                                            .subscribe(
                                                    aikaTehdaJotain -> {

                                                        LOG.error("Status PING... {}", batchId);
                                                        viestintapalveluAsyncResource.haeStatusObservable(batchId)
                                                                .subscribe(
                                                                        b -> {
                                                                            if (VALMIS_STATUS.equals(b.getStatus())) {
                                                                                try {
                                                                                    if (hakukohdeOid.isPresent()) {
                                                                                        LOG.info("##### Dokumentti {} valmistui hakukohteelle {} joten uudelleen nimetään se", batchId, hakukohdeOid.get());
                                                                                        dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohdeOid.get() + ".pdf")
                                                                                                .subscribe(
                                                                                                        success -> {
                                                                                                            LOG.info("Uudelleen nimeäminen onnistui hakukohteelle {}", hakukohdeOid.get());
                                                                                                        },
                                                                                                        error -> {
                                                                                                            LOG.error("Uudelleen nimeäminen epäonnistui hakukohteelle {}", hakukohdeOid.get(), error);
                                                                                                        }
                                                                                                );
                                                                                    } else {
                                                                                        prosessi.setDokumenttiId(batchId);
                                                                                    }
                                                                                } catch (Throwable ttt) {
                                                                                    LOG.error("", ttt);
                                                                                }
                                                                                subscriber.onNext(batchId);
                                                                                stop.onNext("LOPETUS");
                                                                            }
                                                                            if (KESKEYTETTY_STATUS.equals(b.getStatus())) {
                                                                                subscriber.onError(new RuntimeException("Viestintäpalvelu palautti error statuksen hakukohteelle " + hakukohdeOid.get()));
                                                                                stop.onNext("LOPETUS");
                                                                            }
                                                                        }
                                                                );
                                                    }
                                            );

                                }));

                                return plainObservable;
                            }
                    );
        } catch (Throwable error) {
            LOG.error("Viestintäpalveluviestin muodostus epäonnistui hakukohteelle {}", hakukohdeOid, error);
            return Observable.error(error);

        }
    }

    private <T> Observable<T> wrapAsRunOnlyOnceObservable(Observable<T> o) {
        final ConnectableObservable<T> replayingObservable = o.replay(1);
        replayingObservable.connect();
        return replayingObservable;
    }
}
