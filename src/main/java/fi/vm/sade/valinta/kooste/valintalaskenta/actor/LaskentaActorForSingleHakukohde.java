package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorForSingleHakukohde.State.COMPLETE;
import static fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorForSingleHakukohde.State.FIRST_ATTEMPTS;
import static fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorForSingleHakukohde.State.RERUNS;
import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.ilmoitus;
import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.virheilmoitus;

import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

class LaskentaActorForSingleHakukohde implements LaskentaActor {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorForSingleHakukohde.class);

    private final AtomicReference<State> state = new AtomicReference<>(FIRST_ATTEMPTS);
    private final AtomicInteger successTotal = new AtomicInteger(0);
    private final AtomicInteger retryTotal = new AtomicInteger(0);
    private final AtomicInteger failedTotal = new AtomicInteger(0);
    private final LaskentaActorParams actorParams;
    private final Func1<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> hakukohteenLaskenta;
    private final LaskentaSupervisor laskentaSupervisor;
    private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private final int splittaus;
    private final ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> hakukohdeQueue;
    private final ConcurrentLinkedQueue<HakukohdeJaOrganisaatio> retryQueue = new ConcurrentLinkedQueue<>();
    private final boolean isValintaryhmalaskenta;

    public LaskentaActorForSingleHakukohde(LaskentaActorParams actorParams,
                                           Func1<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> hakukohteenLaskenta,
                                           LaskentaSupervisor laskentaSupervisor,
                                           LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
                                           int splittaus) {
        this.actorParams = actorParams;
        this.hakukohteenLaskenta = hakukohteenLaskenta;
        this.laskentaSupervisor = laskentaSupervisor;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.splittaus = splittaus;
        hakukohdeQueue = new ConcurrentLinkedQueue<>(actorParams.getHakukohdeOids());
        this.isValintaryhmalaskenta = actorParams.isValintaryhmalaskenta();
    }

    public void start() {
        LOG.info("(Uuid={}) Laskenta-actor käynnistetty haulle {}, hakukohteita yhteensä {} ", uuid(), getHakuOid(), totalKohteet());
        final boolean onkoTarveSplitata = actorParams.getHakukohdeOids().size() > 20;
        IntStream.range(0, onkoTarveSplitata ? splittaus : 1).forEach(i -> laskeSeuraavaHakukohde());
    }

    private void laskeSeuraavaHakukohde() {
        final Optional<HakukohdeJaOrganisaatio> hkJaOrg;
        final boolean fromRetryQueue;
        if (FIRST_ATTEMPTS.equals(state.get())) {
            hkJaOrg = Optional.ofNullable(hakukohdeQueue.poll());
            fromRetryQueue = false;
        } else if (RERUNS.equals(state.get())) {
            hkJaOrg = Optional.ofNullable(retryQueue.poll());
            fromRetryQueue = true;
        } else {
            throw new IllegalStateException(getClass().getSimpleName() + " on ajamassa laskentaa, vaikka tila on " + state.get());
        }

        if (hkJaOrg.isPresent()) {
            HakukohdeJaOrganisaatio hakukohdeJaOrganisaatio = hkJaOrg.get();
            String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
            Observable.amb(
                hakukohteenLaskenta.call(hakukohdeJaOrganisaatio),
                Observable.timer(90L, TimeUnit.MINUTES).switchMap(t -> Observable.error(new TimeoutException("Laskentaa odotettiin 90 minuuttia ja ohitettiin"))))
                .subscribe(
                    s -> handleSuccessfulLaskentaResult(fromRetryQueue, hakukohdeOid),
                    e -> handleFailedLaskentaResult(fromRetryQueue, hakukohdeJaOrganisaatio, e));
        } else {
            handleEmptyWorkQueueResult();
        }
    }

    private void handleSuccessfulLaskentaResult(boolean fromRetryQueue, String hakukohdeOid) {
        if (fromRetryQueue) {
            LOG.info("(Uuid={}) Hakukohteen ({}) laskenta onnistui uudelleenyrityksellä. Valmiita kohteita laskennassa yhteensä {}/{}",
                uuid(),
                hakukohdeOid,
                successTotal.incrementAndGet(),
                totalKohteet());
        } else {
            LOG.info("(Uuid={}) Hakukohteen ({}) laskenta onnistui. Valmiita kohteita laskennassa yhteensä {}/{}",
                uuid(),
                hakukohdeOid,
                successTotal.incrementAndGet(),
                totalKohteet());
        }
        if (!isValintaryhmalaskenta) {
            HakukohdeTila tila = HakukohdeTila.VALMIS;
            laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid(), hakukohdeOid, tila, Optional.empty()).subscribe(
                ok -> LOG.info("(Uuid={}) Hakukohteen ({}) laskenta on valmis, hakukohteen tila saatiin merkattua seurantaan.",
                    uuid(),
                    hakukohdeOid),
                t -> LOG.error(String.format("(UUID = %s) Hakukohteen (%s) tilan (%s) merkkaaminen epaonnistui!",
                    uuid(), hakukohdeOid, tila), t));
        } else {
            LOG.info("Ei merkitä valintaryhmälaskennan hakukohteiden tilaa seurantaan. (Onnistunut laskenta)");
        }
        laskeSeuraavaHakukohde();
    }

    private void handleFailedLaskentaResult(boolean fromRetryQueue,
                                            HakukohdeJaOrganisaatio hakukohdeJaOrganisaatio,
                                            Throwable failure) {
        String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
        if (!fromRetryQueue) {
            LOG.warn("(Uuid={}) Lisätään hakukohde ({}) epäonnistuneiden jonoon uudelleenyritystä varten. Uudelleenyritettäviä kohteita laskennassa yhteensä {}/{}",
                uuid(),
                hakukohdeOid,
                retryTotal.incrementAndGet(),
                totalKohteet(),
                failure);
            retryQueue.add(hakukohdeJaOrganisaatio);
        } else {
            LOG.error("(Uuid={}) Hakukohteen ({}) laskenta epäonnistui myös uudelleenyrityksellä. Lopullisesti epäonnistuneita kohteita laskennassa yhteensä {}/{}",
                uuid(),
                hakukohdeOid,
                failedTotal.incrementAndGet(),
                totalKohteet(),
                failure);
            if (!isValintaryhmalaskenta) {
                try {
                    laskentaSeurantaAsyncResource.merkkaaHakukohteenTila(uuid(), hakukohdeOid, HakukohdeTila.KESKEYTETTY,
                            Optional.of(virheilmoitus(failure.getMessage(), Arrays.toString(failure.getStackTrace()))));
                    LOG.error("(Uuid={}) Laskenta epäonnistui hakukohteelle {}, tulos merkattu onnistuneesti seurantaan ", uuid(), hakukohdeOid);
                } catch (Throwable e1) {
                    LOG.error("(Uuid={}) Hakukohteen ({}) laskenta epäonnistui mutta ei saatu merkattua ", uuid(), hakukohdeOid, e1);
                }
            } else {
                LOG.info("Ei merkitä valintaryhmälaskennan hakukohteiden tilaa seurantaan. (Epäonnistunut laskenta)");
            }
        }
        laskeSeuraavaHakukohde();
    }

    private void handleEmptyWorkQueueResult() {
        if (state.compareAndSet(FIRST_ATTEMPTS, RERUNS)) {
            if (retryQueue.peek() != null) {
                LOG.info("Laskenta (uuid={}) olisi päättynyt, mutta sisältää keskeytettyjä hakukohteita. Yritetään epäonnistuneita kohteita ({} kpl) uudelleen.",
                    uuid(),
                    retryTotal.get());
                final boolean splitRetry = retryQueue.size() > 20;
                IntStream.range(0, splitRetry ? splittaus : 1).forEach(i -> laskeSeuraavaHakukohde());
                return;
            } else {
                LOG.info("Laskennassa (uuid={}) ei ole epäonnistuneita hakukohteita uudelleenyritettäviksi.", uuid());
            }
        }
        if (totalKohteet() == (successTotal.get() + failedTotal.get())) {
            if (COMPLETE.equals(state.getAndSet(COMPLETE))) {
                LOG.error("state == " + state + " but it is being set to that again! Looks like a bug!", new Exception());
            }
            lopeta();
        }
    }

    public void lopeta() {
        final Observable<Response> tilanmerkkausObservable;
        if (!COMPLETE.equals(state.get())) {
            LOG.warn("#### (Uuid={}) Laskenta lopetettu", uuid());
            tilanmerkkausObservable = laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid(), LaskentaTila.PERUUTETTU, Optional.of(ilmoitus("Laskenta on peruutettu")));
        } else {
            LOG.info("#### (Uuid={}) Laskenta valmis koska ei enää hakukohteita käsiteltävänä. " +
                "Onnistuneita {}, Uudelleenyrityksiä {}, Lopullisesti epäonnistuneita {}",
                uuid(),
                successTotal.get(),
                retryTotal.get(),
                failedTotal.get());
            tilanmerkkausObservable = laskentaSeurantaAsyncResource.merkkaaLaskennanTila(uuid(), LaskentaTila.VALMIS, Optional.empty());
        }
        tilanmerkkausObservable.subscribe(
            response -> laskentaSupervisor.ready(uuid()),
            e -> LOG.error("Ongelma laskennan merkkaamisessa loppuneeksi", e));
    }

    public void postStop() {
        LOG.info("PostStop ajettu");
        lopeta();
    }

    private String uuid() {
        return actorParams.getUuid();
    }

    private int totalKohteet() {
        return actorParams.getHakukohdeOids().size();
    }

    public String getHakuOid() {
        return actorParams.getHakuOid();
    }

    public boolean isValmis() {
        return false;
    }

    public enum State {
        FIRST_ATTEMPTS, RERUNS, COMPLETE
    }
}
