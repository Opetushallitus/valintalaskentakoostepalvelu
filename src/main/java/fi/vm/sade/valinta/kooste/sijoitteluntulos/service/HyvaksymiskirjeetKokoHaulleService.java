package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.SijoittelussaHyvaksyttyHakijaBiPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeetServiceImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func3;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    private final Observable<Long> pulse;

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
        this.pulse = Observable.interval(500L, TimeUnit.MILLISECONDS);
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

    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, SijoittelunTulosProsessi prosessi) {
        //LOG.error("### Aloitetaan hyväksymiskirjeiden massaluonti");
        tarjontaAsyncResource.haeHaku(hakuOid).subscribe(
                haku -> {
                    muodostaHyvaksymiskirjeetKokoHaulle(
                            hakuOid,
                            haku.getHakukohdeOids()
                            //.stream().limit(115).collect(Collectors.toList())
                            , prosessi
                    );
                },
                error -> {
                    LOG.error("Ei saatu hakua tarjonnalta", error);
                }
        );
    }

    private void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, List<String> hakukohdeOids, SijoittelunTulosProsessi prosessi) {
        final ConcurrentLinkedQueue<String> hakukohdeQueue = new ConcurrentLinkedQueue<>(hakukohdeOids);
        Action0 aloitaAsynkroninenSuoritusHakukohdeJonolle =
                () -> {
                    hakukohdeKerralla(hakuOid, hakukohdeQueue, prosessi);
                };
        final boolean onkoTarveSplitata = hakukohdeOids.size() > 20;
        IntStream.range(0, onkoTarveSplitata ? 1 : 1).forEach(i -> aloitaAsynkroninenSuoritusHakukohdeJonolle.call());
    }

    private void hakukohdeKerralla(String hakuOid, ConcurrentLinkedQueue<String> hakukohdeQueue, SijoittelunTulosProsessi prosessi) {
        Optional<String> hakukohdeOid = Optional.ofNullable(hakukohdeQueue.poll());
        hakukohdeOid.ifPresent(
                hakukohde -> {
                    LOG.error("Aloitetaan hakukohteen {} hyväksymiskirjeiden luonti jäljellä {} hakukohdetta", hakukohde, hakukohdeQueue.size());
                    Observable.amb(
                            tarjontaAsyncResource.haeHakukohde(hakukohde).switchMap(
                                    h -> {
                                        try {
                                            String tarjoaja = h.getTarjoajaOids().iterator().next();
                                            String kieli = KirjeetHakukohdeCache.getOpetuskieli(h.getOpetusKielet());
                                            return wrapAsRunOnlyOnceObservable(Observable.combineLatest(
                                                    sijoitteluAsyncResource.getKoulutuspaikkalliset(hakuOid, hakukohde),
                                                    viestintapalveluAsyncResource.haeKirjepohja(hakuOid, tarjoaja, "hyvaksymiskirje", kieli, hakukohde),
                                                    (s, t) -> {
                                                        try {
                                                            List<HakijaDTO> hyvaksytytHakijat =
                                                                    s.getResults().stream().filter(
                                                                            hakija -> new SijoittelussaHyvaksyttyHakijaBiPredicate().test(hakija, hakukohde)
                                                                    ).collect(Collectors.toList());
                                                            if (hyvaksytytHakijat.isEmpty()) {
                                                                return Observable.error(new RuntimeException("Ei hyväksyttyjä hakijoita hakukohteelle " + hakukohde));
                                                            }
                                                            // TARKISTA JOS TYHJA NIIN DONE
                                                            Optional<TemplateDetail> td = etsiVakioDetail(t);
                                                            if (!td.isPresent()) {

                                                                return Observable.error(new RuntimeException("Ei " + VAKIOTEMPLATE + " tai " + VAKIODETAIL + " templateDetailia hakukohteelle " + hakukohde));
                                                            } else {
                                                                return applicationAsyncResource.getApplicationsByHakemusOids(hyvaksytytHakijat.stream().map(hh -> hh.getHakemusOid()).collect(Collectors.toList()))
                                                                        .switchMap(
                                                                                hakemukset -> {
                                                                                    try {
                                                                                        LOG.info("##### Saatiin hakemukset hakukohteelle {}", hakukohde);
                                                                                        Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti.haeKiinnostavatHakukohteet(hyvaksytytHakijat);
                                                                                        MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohde);
                                                                                        Future<Response> organisaatioFuture = organisaatioAsyncResource.haeOrganisaatio(tarjoaja);
                                                                                        String tag = nimiUriToTag(h.getHakukohteenNimiUri(), hakukohde);

                                                                                        LetterBatch l = hyvaksymiskirjeetKomponentti
                                                                                                .teeHyvaksymiskirjeet(
                                                                                                        HyvaksymiskirjeetServiceImpl.todellisenJonosijanRatkaisin(hyvaksytytHakijat),
                                                                                                        HyvaksymiskirjeetServiceImpl.organisaatioResponseToHakijapalveluidenOsoite(
                                                                                                                haeOsoiteKomponentti, organisaatioAsyncResource,
                                                                                                                newArrayList(Arrays.asList(tarjoaja)),
                                                                                                                kohdeHakukohde.getHakukohteenKieli(), organisaatioFuture.get()),
                                                                                                        hyvaksymiskirjeessaKaytetytHakukohteet,
                                                                                                        hyvaksytytHakijat,
                                                                                                        hakemukset,
                                                                                                        hakukohde,
                                                                                                        hakuOid,
                                                                                                        tarjoaja,
                                                                                                        //
                                                                                                        td.get().getDefaultValue(),
                                                                                                        tag,
                                                                                                        "hyvaksymiskirje",
                                                                                                        null,
                                                                                                        null);


                                                                                        LOG.info("##### Tehdään viestintäpalvelukutsu {}", hakukohde);
                                                                                        //LOG.error("{}", new Gson().toJson(l));
                                                                                        return viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(l)
                                                                                                .switchMap(
                                                                                                        letterResponse -> {
                                                                                                            LOG.info("##### Viestintäpalvelukutsu onnistui {}", hakukohde);
                                                                                                            final String batchId = letterResponse.getBatchId();
                                                                                                            LOG.error("##### Odotetaan statusta... BatchId={}", batchId);
                                                                                                            AtomicReference<Subscription> pulseRef = new AtomicReference<>();
                                                                                                            final Observable<String> plainObservable = Observable.create(subscriber -> {
                                                                                                                pulseRef.set(pulse.subscribe(
                                                                                                                        aikaTehdaJotain -> {
                                                                                                                            LOG.error("Status PING... {}", batchId);
                                                                                                                            viestintapalveluAsyncResource.haeStatusObservable(batchId)
                                                                                                                                    .subscribe(
                                                                                                                                            b -> {
                                                                                                                                                if (VALMIS_STATUS.equals(b.getStatus())) {
                                                                                                                                                    LOG.error("##### Dokumentti {} valmistui hakukohteelle {} joten uudelleen nimetään se", batchId, hakukohde);
                                                                                                                                                    try {
                                                                                                                                                        dokumenttiAsyncResource.uudelleenNimea(batchId, "hyvaksymiskirje_" + hakukohde + ".pdf")
                                                                                                                                                                .subscribe(
                                                                                                                                                                        success -> {
                                                                                                                                                                            LOG.error("Uudelleen nimeäminen onnistui hakukohteelle {}", hakukohde);
                                                                                                                                                                        },
                                                                                                                                                                        error -> {
                                                                                                                                                                            LOG.error("Uudelleen nimeäminen epäonnistui hakukohteelle {}", hakukohde, error);
                                                                                                                                                                        }
                                                                                                                                                                );
                                                                                                                                                    } catch (Throwable ttt) {
                                                                                                                                                        LOG.error("", ttt);
                                                                                                                                                    }
                                                                                                                                                    subscriber.onNext(batchId);
                                                                                                                                                }
                                                                                                                                                if (KESKEYTETTY_STATUS.equals(b.getStatus())) {
                                                                                                                                                    subscriber.onError(new RuntimeException("Viestintäpalvelu palautti error statuksen hakukohteelle " + hakukohde));
                                                                                                                                                }
                                                                                                                                            }
                                                                                                                                    );
                                                                                                                        }
                                                                                                                ));

                                                                                                            });
                                                                                                            plainObservable.subscribe(subscribeWithFinally(() -> pulseRef.get().unsubscribe()));

                                                                                                            return plainObservable;
                                                                                                        }
                                                                                                );
                                                                                    } catch (Throwable error) {
                                                                                        LOG.error("Viestintäpalveluviestin muodostus epäonnistui hakukohteelle {}", hakukohde, error);
                                                                                        return Observable.error(error);

                                                                                    }
                                                                                }
                                                                        );
                                                            }
                                                        } catch (Throwable error) {
                                                            LOG.error("Spluush", error);
                                                            return Observable.error(error);

                                                        }
                                                    })).flatMap(o -> o);
                                        } catch (Throwable e) {
                                            return Observable.error(e);
                                        }
                                    }),
                            Observable.timer(3L, TimeUnit.MINUTES)
                    ).subscribe(
                            s -> {
                                LOG.error("Hakukohde {} valmis", hakukohde);
                                hakukohdeKerralla(hakuOid, hakukohdeQueue, prosessi);
                            },
                            e -> {
                                LOG.error("Hakukohde {} ohitettu", hakukohde, e);
                                hakukohdeKerralla(hakuOid, hakukohdeQueue, prosessi);
                            },
                            () -> {

                            }
                    );
                });
        if (!hakukohdeOid.isPresent()) {
            LOG.error("### Hyväksymiskirjeiden generointi haulle {} on valmis", hakuOid);
        }
    }

    private <T> Observable<T> wrapAsRunOnlyOnceObservable(Observable<T> o) {
        final ConnectableObservable<T> replayingObservable = o.replay(1);
        replayingObservable.connect();
        return replayingObservable;
    }
}
