package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.ilmoitus;
import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.virheilmoitus;

class LaskentaActorForSingleHakukohde implements LaskentaActor {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorForSingleHakukohde.class);

    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicBoolean done;
    private final String uuid;
    private final int totalKohteet;
    private final AtomicBoolean retryActive = new AtomicBoolean(false);
    private final AtomicInteger successTotal = new AtomicInteger(0);
    private final AtomicInteger retryTotal = new AtomicInteger(0);
    private final AtomicInteger failedTotal = new AtomicInteger(0);
    private final LaskentaActorParams actorParams;
    private final Func1<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> r;
    private final LaskentaSupervisor laskentaSupervisor;
    private LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private int splittaus;

    public LaskentaActorForSingleHakukohde(LaskentaActorParams actorParams, Func1<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> r, LaskentaSupervisor laskentaSupervisor, LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource, int splittaus) {
        this.actorParams = actorParams;
        this.r = r;
        this.laskentaSupervisor = laskentaSupervisor;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.splittaus = splittaus;
        done = new AtomicBoolean(false);
        uuid = actorParams.getUuid();
        totalKohteet = actorParams.getHakukohdeOids().size();
    }

    public String getHakuOid() {
        return actorParams.getHakuOid();
    }

    public boolean isValmis() {
        return false;
    }

    public void start() {
        LOG.info("(Uuid={}) Laskenta-actor käynnistetty haulle {}, hakukohteita yhteensä {} ", uuid, getHakuOid(), totalKohteet);
        final ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> hakukohdeQueue = new ConcurrentLinkedQueue<>(actorParams.getHakukohdeOids());
        final ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> retryQueue = new ConcurrentLinkedQueue<>();
        final boolean onkoTarveSplitata = actorParams.getHakukohdeOids().size() > 20;
        IntStream.range(0, onkoTarveSplitata ? splittaus : 1).forEach(i -> hakukohdeKerralla(hakukohdeQueue, retryQueue));
    }

    private void hakukohdeKerralla(ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> hakukohdeQueue, ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> retryQueue) {
        Optional<HakukohdeJaOrganisaatio> hkJaOrg;
        boolean fromRetryQueue;
        if(!retryActive.get()) {
            hkJaOrg = Optional.ofNullable(hakukohdeQueue.poll());
            fromRetryQueue = false;
        } else {
            hkJaOrg = Optional.ofNullable(retryQueue.poll());
            fromRetryQueue = true;
        }
        hkJaOrg.ifPresent(
                h ->
                        Observable.amb(
                                r.call(h),
                                Observable.timer(90L, TimeUnit.MINUTES).switchMap(t -> Observable.error(
                                        new TimeoutException("Laskentaa odotettiin 90 minuuttia ja ohitettiin")
                                ))
                        ).subscribe(
                                s -> {
                                    if (fromRetryQueue) {
                                        LOG.info("(Uuid={}) Hakukohteen {} laskenta onnistui uudelleenyrityksellä. Valmiita kohteita laskennassa yhteensä {}/{}", uuid, h.getHakukohdeOid(), successTotal.incrementAndGet(), totalKohteet);
                                    } else {
                                        LOG.info("(Uuid={}) Hakukohteen {} laskenta onnistui. Valmiita kohteita laskennassa yhteensä {}/{}", uuid, h.getHakukohdeOid(), successTotal.incrementAndGet(), totalKohteet);
                                    }
                                    try {
                                        laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid, h.getHakukohdeOid(), HakukohdeTila.VALMIS, Optional.empty());
                                        LOG.info("(Uuid={}) Hakukohteen {} laskenta on valmis, hakukohteen tila saatiin merkattua seurantaan.", uuid, h.getHakukohdeOid());
                                    } catch (Throwable t) {
                                        LOG.error("(Uuid={}) Hakukohteen {} laskenta on valmis mutta ei saatu merkattua.", uuid, h.getHakukohdeOid(), t);
                                    }
                                    hakukohdeKerralla(hakukohdeQueue, retryQueue);
                                },
                                e -> {
                                    if (!fromRetryQueue) {
                                        LOG.warn("(Uuid={}) Lisätään hakukohde {} epäonnistuneiden jonoon uudelleenyritystä varten. Uudelleenyritettäviä kohteita laskennassa yhteensä {}/{}", uuid, h.getHakukohdeOid(), retryTotal.incrementAndGet(), totalKohteet, e);
                                        retryQueue.add(h);
                                    } else {
                                        LOG.error("(Uuid={}) Hakukohteen {} laskenta epäonnistui myös uudelleenyrityksellä. Lopullisesti epäonnistuneita kohteita laskennassa yhteensä {}/{}", uuid, h.getHakukohdeOid(), failedTotal.incrementAndGet(), totalKohteet, e);
                                        try {
                                            laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid, h.getHakukohdeOid(), HakukohdeTila.KESKEYTETTY,
                                                    Optional.of(virheilmoitus(e.getMessage(), Arrays.toString(e.getStackTrace()))));
                                            LOG.error("(Uuid={}) Laskenta epäonnistui hakukohteelle {}, tulos merkattu onnistuneesti seurantaan ", uuid, h.getHakukohdeOid());
                                        } catch (Throwable e1) {
                                            LOG.error("(Uuid={}) Hakukohteen {} laskenta epäonnistui mutta ei saatu merkattua ", uuid, h.getHakukohdeOid(), e1);
                                        }
                                    }
                                    hakukohdeKerralla(hakukohdeQueue, retryQueue);
                                },
                                () -> {
                                }
                        )
        );
        if (!hkJaOrg.isPresent()) {
            if (retryActive.compareAndSet(false, true)) {
                if (retryQueue.peek() != null) {
                    LOG.info("Laskenta (uuid={}) olisi päättynyt, mutta sisältää keskeytettyjä hakukohteita. Yritetään epäonnistuneita kohteita ({} kpl) uudelleen.", uuid, retryTotal.get());
                    final boolean splitRetry = retryQueue.size() > 20;
                    IntStream.range(0, splitRetry ? splittaus : 1).forEach(i -> hakukohdeKerralla(hakukohdeQueue, retryQueue));
                } else {
                    LOG.info("Laskennassa (uuid={}) ei ole epäonnistuneita hakukohteita uudelleenyritettäviksi.", uuid);
                }
            }
            if (totalKohteet == (successTotal.get() + failedTotal.get())) {
                done.set(true);
                lopeta();
            }
        }
    }

    public void lopeta() {
        active.set(false);
        if (!done.get()) {
            LOG.warn("#### (Uuid={}) Laskenta lopetettu", uuid);
            laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU, Optional.of(ilmoitus("Laskenta on peruutettu")));
        } else {
            LOG.info("#### (Uuid={}) Laskenta valmis koska ei enää hakukohteita käsiteltävänä. Onnistuneita {}, Uudelleenyrityksiä {}, Lopullisesti epäonnistuneita {}", uuid, successTotal.get(), retryTotal.get(), failedTotal.get());
            laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
        }
        laskentaSupervisor.ready(uuid);
    }

    public void postStop() {
        LOG.info("PostStop ajettu");
        lopeta();
    }
}
