package fi.vm.sade.valinta.kooste.sijoitteluntulos.service;

import com.google.common.base.FinalizableWeakReference;
import com.google.gson.Gson;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Valmis;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
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
import rx.functions.*;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.ref.Reference;
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

/**
 * @author Jussi Jartamo
 */
@Service
public class HyvaksymiskirjeetKokoHaulleService {

    private static final String VAKIOTEMPLATE = "default";
    private static final String VAKIODETAIL = "sisalto";
    private static final String VALMIS_STATUS = "ready";
    private static final String KESKEYTETTY_STATUS = "error";
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKokoHaulleService.class);
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final Func3<String,String,SijoittelunTulosProsessi, ? extends Observable<?>> hakukohdeHandler;
    private static String nimiUriToTag(String nimiUri, String deflt) {
        return Optional.ofNullable(nimiUri).map(
                n -> n.split("#")[0]//.split("#")[0]
        ).orElse(deflt);
    }
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
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;

        final Observable<Long> pulse = Observable.interval(500L, TimeUnit.MILLISECONDS);

        this.hakukohdeHandler = (haku,hakukohde, prosessi) -> {
            //LOG.error("#### Aloitetaan hakukohteen {} kanssa", hakukohde);
            return wrapAsRunOnlyOnceObservable(tarjontaAsyncResource.haeHakukohde(hakukohde).flatMap(
                    h -> {
                        try {
                            //LOG.error("Saatiin hakukohde {}", hakukohde);
                            Teksti hakukohdeNimi = new Teksti(h.getHakukohteenNimet());
                            String tarjoaja = h.getTarjoajaOids().iterator().next();
                            String kieli = KirjeetHakukohdeCache.getOpetuskieli(h.getOpetusKielet());
                            return wrapAsRunOnlyOnceObservable(Observable.combineLatest(
                                    sijoitteluAsyncResource.getKoulutuspaikkalliset(haku, hakukohde),
                                    viestintapalveluAsyncResource.haeKirjepohja(haku, tarjoaja, "hyvaksymiskirje",
                                            kieli, hakukohde),

                                    (s, t) -> {
                                        try {
                                            //LOG.info("Saatiin sijoittelun tulokset ja templatet hakukohteelle {}", hakukohde);
                                            List<HakijaDTO> hyvaksytytHakijat =
                                                    s.getResults().stream().filter(
                                                            hakija -> new SijoittelussaHyvaksyttyHakijaBiPredicate().test(hakija, hakukohde)
                                                    ).collect(Collectors.toList());
                                            if (hyvaksytytHakijat.isEmpty()) {
                                                return Observable.error(new RuntimeException("Ei hyväksyttyjä hakijoita hakukohteelle " + hakukohde));
                                            }
                                            // TARKISTA JOS TYHJA NIIN DONE
                                            Optional<TemplateDetail> td = etsiVakioDetail(t);
                                            if (td.isPresent()) {
                                                return wrapAsRunOnlyOnceObservable(applicationAsyncResource.getApplicationsByHakemusOids(hyvaksytytHakijat.stream().map(hh -> hh.getHakemusOid()).collect(Collectors.toList()))
                                                        .map(
                                                                hakemukset -> {
                                                                    try {
                                                                        LOG.info("##### Saatiin hakemukset hakukohteelle {}", hakukohde);
                                                                        Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti
                                                                                .haeKiinnostavatHakukohteet(hyvaksytytHakijat);
                                                                        MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet
                                                                                .get(hakukohde);
                                                                        Future<Response> organisaatioFuture = organisaatioAsyncResource
                                                                                .haeOrganisaatio(tarjoaja);
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
                                                                                        haku,
                                                                                        tarjoaja,
                                                                                        //
                                                                                        td.get().getDefaultValue(),
                                                                                        tag,
                                                                                        "hyvaksymiskirje",
                                                                                        null,
                                                                                        null);


                                                                        LOG.info("##### Tehdään viestintäpalvelukutsu {}", hakukohde);
                                                                        //LOG.error("{}", new Gson().toJson(l));
                                                                        return wrapAsRunOnlyOnceObservable(viestintapalveluAsyncResource.viePdfJaOdotaReferenssiObservable(l)
                                                                                .map(
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


                                                                                            return wrapAsRunOnlyOnceObservable(plainObservable).subscribe(subscribeWithFinally(() -> pulseRef.get().unsubscribe()));
                                                                                        }
                                                                                ));
                                                                    } catch (Throwable error) {
                                                                        LOG.error("Viestintäpalveluviestin muodostus epäonnistui hakukohteelle {}", hakukohde, error);
                                                                        return wrapAsRunOnlyOnceObservable(Observable.error(error));

                                                                    }
                                                                }
                                                        ));
                                            } else {
                                                return wrapAsRunOnlyOnceObservable(Observable.error(
                                                        new RuntimeException("Ei " +
                                                                VAKIOTEMPLATE + " tai " +
                                                                VAKIODETAIL + " templateDetailia hakukohteelle " + hakukohde)));
                                            }
                                        } catch (Throwable error) {
                                            LOG.error("Muodostus epäonnistui hakukohteelle {}", hakukohde, error);
                                            return wrapAsRunOnlyOnceObservable(Observable.error(error));

                                        }
                                    }));
                        } catch (Throwable tttt) {
                            LOG.error("Muodostus epäonnistui hakukohteelle {}", hakukohde, tttt);
                            return wrapAsRunOnlyOnceObservable(Observable.error(tttt));
                        }
                    }
            ));
        };
    }

    private static Optional<TemplateDetail> etsiVakioDetail(List<TemplateHistory> t) {
        Stream<TemplateHistory> vainVakioHistoriat = t.stream().filter(th -> VAKIOTEMPLATE.equals(th.getName()));
        Optional<TemplateDetail> o =
                vainVakioHistoriat
                        .flatMap(td -> td.getTemplateReplacements().stream().filter(tdd -> VAKIODETAIL.equals(tdd.getName())))
                        .findAny();
        return o;
    }


    public void muodostaHyvaksymiskirjeetKokoHaulle(String hakuOid, SijoittelunTulosProsessi prosessi) {
        //LOG.error("### Aloitetaan hyväksymiskirjeiden massaluonti");
        tarjontaAsyncResource.haeHaku(hakuOid).subscribe(
                haku -> {
                    muodostaHyvaksymiskirjeetKokoHaulle(
                            hakuOid,
                            haku.getHakukohdeOids()
                            //
                            // Vaan 30 ekaa
                            //
                            //.stream().limit(115).collect(Collectors.toList())
                            ,prosessi
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
                    final PublishSubject<String> subject = PublishSubject.create();
                    hakukohdeKerralla(hakuOid, hakukohdeQueue, subject, prosessi);
                    subject.onNext(hakukohdeQueue.poll());
                };
        final boolean onkoTarveSplitata = hakukohdeOids.size() > 20000;
        IntStream.range(0, onkoTarveSplitata ? 5 : 1).forEach(i -> aloitaAsynkroninenSuoritusHakukohdeJonolle.call());
    }

    private void hakukohdeKerralla(String hakuOid, ConcurrentLinkedQueue<String> hakukohdeQueue, PublishSubject<String> subject, SijoittelunTulosProsessi prosessi) {
        final int kokotyo = hakukohdeQueue.size();
        AtomicInteger counter = new AtomicInteger(0);
        subject.asObservable().map(hakukohde -> {
            //LOG.error("############ käsitellään {}", hakukohde);
            return hakukohdeHandler.call(hakuOid, hakukohde, prosessi);
        }).flatMap(o -> o).subscribe(
                subscribeWithFinally(
                        success -> {
                            LOG.error("###### Success {}", success);
                        },
                        error -> {
                            //LOG.error("###### Error {}", error);
                        },
                        () -> { // finally
                            LOG.error("########### Hakukohde {}/{} aloitetaan hyväksymiskirjeen luonti", counter.incrementAndGet(), kokotyo);

                            //LOG.error("############ Seuraavaa");
                            if (hakukohdeQueue.isEmpty()) {
                                LOG.error("############ Oli tyhjä");
                            } else {

                                Optional.ofNullable(hakukohdeQueue.poll()).ifPresent(seuraava -> {
                                    LOG.error("############ Uusi hakukohde käsittelyyn {}", seuraava);
                                    subject.onNext(seuraava);
                                });
                            }
                        }
                )
        );
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action0 doFinally) {
        return subscribeWithFinally(f -> {}, f-> {}, ()->{},doFinally);
    }
    private static <T> Subscriber<T> subscribeWithFinally(Action1<T> doNext, Action1<Throwable> doError, Action0 doFinally) {
        return subscribeWithFinally(doNext, doError, ()->{},doFinally);
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action1<T> doNext, Action1<Throwable> doError, Action0 doComplete, Action0 doFinally) {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                try {
                    doComplete.call();
                }finally {
                    try {
                        doFinally.call();
                    }catch (Throwable t){}
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    doError.call(e);
                }finally {
                    try {
                        doFinally.call();
                    }catch (Throwable t){}
                }
            }

            @Override
            public void onNext(T t) {
                try {
                    doNext.call(t);
                }finally {
                    try {
                        doFinally.call();
                    }catch (Throwable tt){}
                }
            }
        };
    }
    private <T> Observable<T> wrapAsRunOnlyOnceObservable(Observable<T> o) {
        final ConnectableObservable<T> replayingObservable = o.replay(1);
        replayingObservable.connect();
        return replayingObservable;
    }
}
