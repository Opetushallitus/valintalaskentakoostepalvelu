package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.ilmoitus;
import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.virheilmoitus;
import com.google.common.collect.Lists;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakuUuidHakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintaryhmaPalvelukutsuYhdiste;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintaryhmatKatenoivaValintalaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.YksiPalvelukutsuKerrallaPalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

@Service
@ManagedResource(objectName = "OPH:name=LaskentaActorFactory", description = "LaskentaActorFactory mbean")
public class LaskentaActorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorFactory.class);

    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private volatile int splittaus;

    @Autowired
    public LaskentaActorFactory(
            @Value("${valintalaskentakoostepalvelu.laskennan.splittaus:1}") int splittaus,
            ValintalaskentaAsyncResource valintalaskentaAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource
    ) {
        this.splittaus = splittaus;
        this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    }

    public LaskentaActor createValintaryhmaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final PalvelukutsuStrategia laskentaStrategia = createStrategia();
        final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
        final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
        final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
        final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
        final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
                laskentaStrategia,
                valintaperusteetStrategia,
                hakemuksetStrategia,
                hakijaryhmatStrategia,
                suoritusrekisteriStrategia
        );

        final List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakemuksetPalvelukutsu>> hakemuksetPalvelukutsut = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<ValintaperusteetPalvelukutsu>> valintaperusteetPalvelukutsut = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakijaryhmatPalvelukutsu>> hakijaryhmatPalvelukutsut = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<SuoritusrekisteriPalvelukutsu>> suoritusrekisteriPalvelukutsut = Lists.newArrayList();
        actorParams.getHakukohdeOids().forEach(hk -> {
            UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), hk);
            ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu = new ValintaperusteetPalvelukutsu(uudiHk, actorParams.getValinnanvaihe(), valintaperusteetAsyncResource);
            HakemuksetPalvelukutsu hakemuksetPalvelukutsu = new HakemuksetPalvelukutsu(actorParams.getHakuOid(), uudiHk, applicationAsyncResource);
            SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu = new SuoritusrekisteriPalvelukutsu(new HakuUuidHakukohdeJaOrganisaatio(haku, uudiHk.getUuid(), uudiHk.getHakukohdeJaOrganisaatio()), suoritusrekisteriAsyncResource);
            HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu = new HakijaryhmatPalvelukutsu(uudiHk, valintaperusteetAsyncResource);
            hakemuksetPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(hakemuksetPalvelukutsu, hakemuksetStrategia));
            valintaperusteetPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(valintaperusteetPalvelukutsu, valintaperusteetStrategia));
            hakijaryhmatPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(hakijaryhmatPalvelukutsu, hakijaryhmatStrategia));
            suoritusrekisteriPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(suoritusrekisteriPalvelukutsu, suoritusrekisteriStrategia));
            valintaryhmaPalvelukutsuYhdiste.add(new ValintaryhmaPalvelukutsuYhdiste(
                    hk.getHakukohdeOid(),
                    hakemuksetPalvelukutsu,
                    valintaperusteetPalvelukutsu,
                    hakijaryhmatPalvelukutsu,
                    suoritusrekisteriPalvelukutsu
            ));
        });

        ValintaryhmatKatenoivaValintalaskentaPalvelukutsu laskentaPk = new ValintaryhmatKatenoivaValintalaskentaPalvelukutsu(
                haku,
                actorParams.getParametritDTO(),
                actorParams.isErillishaku(),
                new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), new HakukohdeJaOrganisaatio("Valintaryhmalaskenta(" + actorParams.getHakukohdeOids().size() + "kohdetta)", "kaikkiOrganisaatiot")),
                valintalaskentaAsyncResource,
                valintaryhmaPalvelukutsuYhdiste,
                hakemuksetPalvelukutsut,
                valintaperusteetPalvelukutsut,
                hakijaryhmatPalvelukutsut,
                suoritusrekisteriPalvelukutsut
        );

        ValintaryhmaLaskentaActorImpl v = new ValintaryhmaLaskentaActorImpl(
                laskentaSupervisor,
                actorParams.getUuid(),
                actorParams.getHakuOid(),
                laskentaPk,
                strategiat,
                laskentaStrategia,
                laskentaSeurantaAsyncResource
        );
        laskentaPk.setCallback(v);
        return v;
    }

    private <T> Observable<T> wrapAsRunOnlyOnceObservable(Observable<T> o) {
        final ConnectableObservable<T> replayingObservable = o.replay(1);
        replayingObservable.connect();
        return replayingObservable;
    }

    @ManagedOperation
    public void setLaskentaSplitCount(int splitCount) {
        this.splittaus = splitCount;
        LOG.info("Laskenta split count asetettu arvoon {}", splitCount);
    }

    public LaskentaActor createValintakoelaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan valintakoelaskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
                    Observable<List<ValintaperusteetDTO>> valintaperusteet = valintaperusteetAsyncResource.haeValintaperusteet(hakukohdeJaOrganisaatio.getHakukohdeOid(), actorParams.getValinnanvaihe());
                    valintaperusteet.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<Hakemus>> hakemukset = applicationAsyncResource.getApplicationsByOid(actorParams.getHakuOid(), hakukohdeJaOrganisaatio.getHakukohdeOid());
                    hakemukset.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<Oppija>> oppijat = suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeJaOrganisaatio.getHakukohdeOid(), haku.getOid());
                    oppijat.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));


                    return wrapAsRunOnlyOnceObservable(Observable.combineLatest(
                            valintaperusteet,
                            hakemukset,
                            oppijat,
                            (v, h, o) -> {
                                Observable<String> l =
                                        valintalaskentaAsyncResource.valintakokeet(new LaskeDTO(
                                                actorParams.getUuid(),
                                                haku.isKorkeakouluHaku(),
                                                actorParams.isErillishaku(),
                                                hakukohdeOid,
                                                HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, hakukohdeOid, h, o, actorParams.getParametritDTO(), true), v));
                                l.subscribe(laskentaOK.apply(uuid, hakukohdeOid), laskentaException.apply(uuid, hakukohdeOid));
                                return l;
                            }
                    ).flatMap(o -> o));
                }
        );
    }

    private static final BiFunction<String, String, Action1<? super Object>> resurssiOK = (uuid, hakukohde) -> resurssi -> LOG.info("(Uuid={}) Saatiin resurssi hakukohteelle {}", uuid, hakukohde);
    private static final BiFunction<String, String, Action1<Throwable>> resurssiException = (uuid, hakukohde) -> error -> {
        String message = HttpExceptionWithResponse.appendWrappedResponse(String.format("(Uuid=%s) Resurssin lataus epäonnistui hakukohteelle %s", uuid, hakukohde)
            , error);
        LOG.warn(message, error);
    };

    public LaskentaActor createValintalaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
                    Observable<List<ValintaperusteetDTO>> valintaperusteet = valintaperusteetAsyncResource.haeValintaperusteet(hakukohdeJaOrganisaatio.getHakukohdeOid(), actorParams.getValinnanvaihe());
                    valintaperusteet.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<Hakemus>> hakemukset = applicationAsyncResource.getApplicationsByOid(actorParams.getHakuOid(), hakukohdeJaOrganisaatio.getHakukohdeOid());
                    hakemukset.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<Oppija>> oppijat = suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeJaOrganisaatio.getHakukohdeOid(), haku.getOid());
                    oppijat.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat = valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid);
                    hakijaryhmat.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));

                    return wrapAsRunOnlyOnceObservable(Observable.combineLatest(
                            hakijaryhmat,
                            valintaperusteet,
                            hakemukset,
                            oppijat,
                            (hr, v, h, o) -> {
                                Observable<String> l =
                                        valintalaskentaAsyncResource.laske(new LaskeDTO(
                                                actorParams.getUuid(),
                                                haku.isKorkeakouluHaku(),
                                                actorParams.isErillishaku(),
                                                hakukohdeOid,
                                                HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, hakukohdeOid, h, o, actorParams.getParametritDTO(), true), v, hr));
                                l.subscribe(laskentaOK.apply(uuid, hakukohdeOid), laskentaException.apply(uuid, hakukohdeOid));
                                return l;
                            }

                    ).flatMap(o -> o));
                }
        );
    }

    private static final BiFunction<String, String, Action1<? super Object>> laskentaOK = (uuid, hakukohde) -> resurssi -> LOG.info("(Uuid={}) Laskenta onnistui hakukohteelle {}", uuid, hakukohde);
    private static final BiFunction<String, String, Action1<Throwable>> laskentaException = (uuid, hakukohde) -> error -> {
        String message = HttpExceptionWithResponse.appendWrappedResponse(String.format("(Uuid=%s) Laskenta epäonnistui hakukohteelle %s", uuid, hakukohde), error);
        LOG.warn(message, error);
    };

    public LaskentaActor createValintalaskentaJaValintakoelaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan + valintakoelaskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
                    Observable<List<ValintaperusteetDTO>> valintaperusteet = valintaperusteetAsyncResource.haeValintaperusteet(hakukohdeJaOrganisaatio.getHakukohdeOid(), actorParams.getValinnanvaihe());
                    valintaperusteet.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<Hakemus>> hakemukset = applicationAsyncResource.getApplicationsByOid(actorParams.getHakuOid(), hakukohdeJaOrganisaatio.getHakukohdeOid());
                    hakemukset.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<Oppija>> oppijat = suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeJaOrganisaatio.getHakukohdeOid(), haku.getOid());
                    oppijat.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));
                    Observable<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat = valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid);
                    hakijaryhmat.subscribe(resurssiOK.apply(uuid, hakukohdeOid), resurssiException.apply(uuid, hakukohdeOid));

                    return wrapAsRunOnlyOnceObservable(Observable.combineLatest(
                            hakijaryhmat,
                            valintaperusteet,
                            hakemukset,
                            oppijat,
                            (hr, v, h, o) ->
                            {
                                Observable<String> l =
                                        valintalaskentaAsyncResource.laskeKaikki(new LaskeDTO(
                                                actorParams.getUuid(),
                                                haku.isKorkeakouluHaku(),
                                                actorParams.isErillishaku(),
                                                hakukohdeOid,
                                                HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, hakukohdeOid, h, o, actorParams.getParametritDTO(), true), v, hr));
                                l.subscribe(laskentaOK.apply(uuid, hakukohdeOid), laskentaException.apply(uuid, hakukohdeOid));
                                return l;
                            }
                    ).flatMap(o -> o));

                }
        );
    }

    private PalvelukutsuStrategia createStrategia() {
        return new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
    }

    public LaskentaActor createLaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        if (LaskentaTyyppi.VALINTARYHMALASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTARYHMALASKENTA");
            return createValintaryhmaActor(laskentaSupervisor, haku, actorParams);
        }
        if (LaskentaTyyppi.VALINTAKOELASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTAKOELASKENTA");
            return createValintakoelaskentaActor(laskentaSupervisor, haku, actorParams);
        }
        if (LaskentaTyyppi.VALINTALASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTALASKENTA");
            return createValintalaskentaActor(laskentaSupervisor, haku, actorParams);
        }
        LOG.info("Muodostetaan KAIKKI VAIHEET LASKENTA koska valinnanvaihe oli {} ja valintakoelaskenta ehto {}", actorParams.getValinnanvaihe(), actorParams.isValintakoelaskenta());
        return createValintalaskentaJaValintakoelaskentaActor(laskentaSupervisor, haku, actorParams);
    }

    private LaskentaActor laskentaHakukohteittainActor(LaskentaSupervisor laskentaSupervisor, LaskentaActorParams actorParams, Func1<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> r) {
        return new LaskentaActor() {
            final AtomicBoolean active = new AtomicBoolean(true);
            final AtomicBoolean done = new AtomicBoolean(false);
            final String uuid = actorParams.getUuid();

            public String getHakuOid() {
                return actorParams.getHakuOid();
            }

            public boolean isValmis() {
                return false;
            }

            public void start() {
                final ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> hakukohdeQueue = new ConcurrentLinkedQueue<>(actorParams.getHakukohdeOids());
                final boolean onkoTarveSplitata = actorParams.getHakukohdeOids().size() > 20;
                IntStream.range(0, onkoTarveSplitata ? splittaus : 1).forEach(i -> hakukohdeKerralla(hakukohdeQueue));
            }

            private void hakukohdeKerralla(ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> hakukohdeQueue) {
                Optional<HakukohdeJaOrganisaatio> hkJaOrg = Optional.ofNullable(hakukohdeQueue.poll());
                hkJaOrg.ifPresent(
                        h ->
                                Observable.amb(
                                        r.call(h),
                                        Observable.timer(90L, TimeUnit.MINUTES).switchMap(t -> Observable.error(
                                                new TimeoutException("Laskentaa odotettiin 90 minuuttia ja ohitettiin")
                                        ))
                                ).subscribe(
                                        s -> {
                                            try {
                                                laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid, h.getHakukohdeOid(), HakukohdeTila.VALMIS, Optional.empty());
                                                LOG.info("(Uuid={}) Hakukohteen {} laskenta on valmis", uuid, h.getHakukohdeOid());
                                            } catch (Throwable t) {
                                                LOG.error("(Uuid={}) Hakukohteen {} laskenta on valmis mutta ei saatu merkattua", uuid, h.getHakukohdeOid(), t);
                                            }
                                            hakukohdeKerralla(hakukohdeQueue);
                                        },
                                        e -> {
                                            try {
                                                laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid, h.getHakukohdeOid(), HakukohdeTila.KESKEYTETTY,
                                                        Optional.of(virheilmoitus(e.getMessage(), Arrays.toString(e.getStackTrace()))));
                                                LOG.info("(Uuid={}) Laskenta epäonnistui hakukohteelle {}", uuid, h.getHakukohdeOid(), e);
                                            } catch (Throwable e1) {
                                                LOG.error("(Uuid={}) Hakukohteen {} laskenta epäonnistui mutta ei saatu merkattua", uuid, h.getHakukohdeOid(), e1);
                                            }
                                            hakukohdeKerralla(hakukohdeQueue);
                                        },
                                        () -> {

                                        }
                                )
                );
                if (!hkJaOrg.isPresent()) {
                    done.set(true);
                    lopeta();
                }
            }

            public void lopeta() {
                active.set(false);
                if (!done.get()) {
                    LOG.warn("#### (Uuid={}) Laskenta lopetettu", uuid);
                    laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU, Optional.of(ilmoitus("Laskenta on peruutettu")));
                } else {
                    LOG.info("#### (Uuid={}) Laskenta valmis koska ei enää hakukohteita käsiteltävänä", uuid);
                    laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
                }
                laskentaSupervisor.ready(uuid);
            }

            public void postStop() {
                lopeta();
            }
        };
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action1<T> doNext, Action1<Throwable> doError, Action0 doFinally) {
        return subscribeWithFinally(doNext, doError, () -> {
        }, doFinally);
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action0 doFinally) {
        return subscribeWithFinally(f -> {
        }, f -> {
        }, () -> {
        }, doFinally);
    }

    private static <T> Subscriber<T> subscribeWithFinally(Action1<T> doNext, Action1<Throwable> doError, Action0 doComplete, Action0 doFinally) {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                try {
                    doComplete.call();
                } finally {
                    doFinally.call();
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    doError.call(e);
                } finally {
                    doFinally.call();
                }
            }

            @Override
            public void onNext(T t) {
                try {
                    doNext.call(t);
                } finally {
                    doFinally.call();
                }
            }
        };
    }

}
