package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.valinta.http.ObservableUtil.wrapAsRunOnlyOnceObservable;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.tuple.Pair.of;
import static rx.Observable.combineLatest;
import static rx.Observable.just;
import static rx.Observable.timer;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaResurssinhakuObservable.PyynnonTunniste;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.http.ObservableUtil.wrapAsRunOnlyOnceObservable;
import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.ilmoitus;
import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.virheilmoitus;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.tuple.Pair.of;
import static rx.Observable.*;

@Service
@ManagedResource(objectName = "OPH:name=LaskentaActorFactory", description = "LaskentaActorFactory mbean")
public class LaskentaActorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorFactory.class);

    private final ValintapisteAsyncResource valintapisteAsyncResource;
    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private volatile int splittaus;

    @Autowired
    public LaskentaActorFactory(
            @Value("${valintalaskentakoostepalvelu.laskennan.splittaus:1}") int splittaus,
            ValintalaskentaAsyncResource valintalaskentaAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            ValintapisteAsyncResource valintapisteAsyncResource
    ) {
        this.splittaus = splittaus;
        this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.valintapisteAsyncResource = valintapisteAsyncResource;
    }
    private Pair<String, Collection<String>> headAndTail(Collection<String> c) {
        String head = c.iterator().next();
        Collection<String> tail = c.stream().skip(1L).collect(Collectors.toList());
        return Pair.of(head,tail);
    }

    private Observable<Pair<Collection<String>, List<LaskeDTO>>> fetchRecursively(Function<String, Observable<LaskeDTO>> haeLaskeDTOHakukohteelle, Observable<Pair<Collection<String>, List<LaskeDTO>>> tulosObservable) {
        return tulosObservable.switchMap(haetutResurssit -> {
            Collection<String> oids = haetutResurssit.getKey();
            if(oids.isEmpty()) {
                return tulosObservable;
            } else {
                Pair<String, Collection<String>> headWithTail = headAndTail(oids);
                Observable<Pair<Collection<String>, List<LaskeDTO>>> aiemmatJaUusiResurssi = haeLaskeDTOHakukohteelle.apply(headWithTail.getKey())
                        .map(laskeDTO -> Pair.of(headWithTail.getRight(), Stream.concat(Stream.of(laskeDTO), haetutResurssit.getRight().stream()).collect(Collectors.toList())));
                return aiemmatJaUusiResurssi.switchMap(haetut -> fetchRecursively(haeLaskeDTOHakukohteelle, Observable.just(haetut)));
            }
        });
    }

    private LaskentaActor createValintaryhmaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams a) {
        LaskentaActorParams fakeOnlyOneHakukohdeParams = new LaskentaActorParams(a.getLaskentaStartParams(), Collections.singletonList(new HakukohdeJaOrganisaatio()), a.getParametritDTO());
        fakeOnlyOneHakukohdeParams.setValintaryhmalaskenta(true);
        return laskentaHakukohteittainActor(laskentaSupervisor, fakeOnlyOneHakukohdeParams,
                hakukohdeJaOrganisaatio -> {
                    String uuid = a.getUuid();
                    Collection<String> hakukohdeOids = a.getHakukohdeOids().stream().map(HakukohdeJaOrganisaatio::getHakukohdeOid).collect(Collectors.toList());
                    String hakukohteidenNimi = String.format("Valintaryhmälaskenta %s hakukohteella", hakukohdeOids.size());
                    LOG.info("(Uuid={}) {}", uuid, hakukohteidenNimi);
                    Observable<Pair<Collection<String>, List<LaskeDTO>>> recursiveSequentialFetch = just(of(hakukohdeOids, emptyList()));

                    Function<String, Observable<LaskeDTO>> fetchLaskeDTO = h -> fetchResourcesForOneLaskenta(
                            auditSession, uuid, haku, h, a, true, true);

                    Observable<String> laskenta = fetchRecursively(fetchLaskeDTO, recursiveSequentialFetch).switchMap(hksAndDtos -> {
                        List<LaskeDTO> allLaskeDTOs = hksAndDtos.getRight();
                        if(!hksAndDtos.getKey().isEmpty()) { // sanity check
                            throw new RuntimeException("Kaikkia hakukohteita ei ollut vielä haettu!");
                        } else if(allLaskeDTOs.size() != hakukohdeOids.size()) {
                            throw new RuntimeException("Hakukohteita oli " + hakukohdeOids.size() + " mutta haettuja laskeDTOita oli " + allLaskeDTOs.size() + "!");
                        }
                        return valintalaskentaAsyncResource.laskeJaSijoittele(allLaskeDTOs);
                    });
                    return laskenta;
                }
        );
    }

    @ManagedOperation
    public void setLaskentaSplitCount(int splitCount) {
        this.splittaus = splitCount;
        LOG.info("Laskenta split count asetettu arvoon {}", splitCount);
    }

    private LaskentaActor createValintakoelaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    Observable<String> laskenta = fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false, false)
                            .switchMap(timedSwitchMap((took, exception) -> {
                                if (exception.isPresent()) {
                                    LOG.error("(Uuid={}) (Kesto {}s) Laskenta hakukohteelle {} on päättynyt virheeseen: {}", uuid, millisToString(took), hakukohdeOid, exception.get());
                                } else {
                                    LOG.info("(Uuid={}) (Kesto {}s) Laskenta hakukohteelle {} on päättynyt onnistuneesti.", uuid, millisToString(took), hakukohdeOid);
                                }
                            }, valintalaskentaAsyncResource::valintakokeet));
                    return laskenta;
                }
        );
    }

    private <A, T> Func1<A, Observable<T>> timedSwitchMap(BiConsumer<Long, Optional<Throwable>> log, Function<A, Observable<T>> f) {//Function<A,Observable<T>> switchMap) {
        return (A a) -> {
            long start = System.currentTimeMillis();
            Observable<T> t = wrapAsRunOnlyOnceObservable(f.apply(a));
            t.subscribe(
                    (n) -> log.accept(System.currentTimeMillis() - start, Optional.empty()),
                    (n) -> log.accept(System.currentTimeMillis() - start, Optional.ofNullable(n)));
            return t;
        };
    }

    private static String millisToString(long millis) {
        return new BigDecimal(millis).divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private LaskentaActor createValintalaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);


                    Observable<String> laskenta = fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false, true)
                            .switchMap(timedSwitchMap((took, exception) -> {
                                if (exception.isPresent()) {
                                    LOG.error("(Uuid={}) (Kesto {}s) Laskenta hakukohteelle {} on päättynyt virheeseen: {}", uuid, millisToString(took), hakukohdeOid, exception.get());
                                } else {
                                    LOG.info("(Uuid={}) (Kesto {}s) Laskenta hakukohteelle {} on päättynyt onnistuneesti.", uuid, millisToString(took), hakukohdeOid);
                                }
                            }, valintalaskentaAsyncResource::laske));
                    return laskenta;
                }
        );
    }

    private static final BiFunction<String, String, Action1<? super Object>> laskentaOK = (uuid, hakukohde) -> resurssi -> LOG.info("(Uuid={}) Laskenta onnistui hakukohteelle {}", uuid, hakukohde);
    private static final BiFunction<String, String, Action1<Throwable>> laskentaException = (uuid, hakukohde) -> error -> {
        String message = HttpExceptionWithResponse.appendWrappedResponse(String.format("(Uuid=%s) Laskenta epäonnistui hakukohteelle %s", uuid, hakukohde), error);
        LOG.error(message, error);
    };

    private LaskentaActor createValintalaskentaJaValintakoelaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan + valintakoelaskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
                    Observable<String> laskenta = fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false,true)
                            .switchMap(timedSwitchMap((took, exception) -> {
                                if (exception.isPresent()) {
                                    LOG.error("(Uuid={}) (Kesto {}s) Laskenta hakukohteelle {} on päättynyt virheeseen: {}", uuid, millisToString(took), hakukohdeOid, exception.get());
                                } else {
                                    LOG.info("(Uuid={}) (Kesto {}s) Laskenta hakukohteelle {} on päättynyt onnistuneesti.", uuid, millisToString(took), hakukohdeOid);
                                }
                            }, valintalaskentaAsyncResource::laskeKaikki));
                    return laskenta;
                }
        );
    }

    public LaskentaActor createLaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        if (LaskentaTyyppi.VALINTARYHMALASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTARYHMALASKENTA");
            return createValintaryhmaActor(auditSession, laskentaSupervisor, haku, actorParams);
        }
        if (LaskentaTyyppi.VALINTAKOELASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTAKOELASKENTA");
            return createValintakoelaskentaActor(auditSession, laskentaSupervisor, haku, actorParams);
        }
        if (LaskentaTyyppi.VALINTALASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTALASKENTA");
            return createValintalaskentaActor(auditSession, laskentaSupervisor, haku, actorParams);
        }
        LOG.info("Muodostetaan KAIKKI VAIHEET LASKENTA koska valinnanvaihe oli {} ja valintakoelaskenta ehto {}", actorParams.getValinnanvaihe(), actorParams.isValintakoelaskenta());
        return createValintalaskentaJaValintakoelaskentaActor(auditSession, laskentaSupervisor, haku, actorParams);
    }

    private LaskentaActor laskentaHakukohteittainActor(LaskentaSupervisor laskentaSupervisor, LaskentaActorParams actorParams, Func1<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> r) {
        return new LaskentaActorForSingleHakukohde(actorParams, r, laskentaSupervisor, laskentaSeurantaAsyncResource, splittaus);
    }

    private Observable<LaskeDTO> getLaskeDTOObservable(String uuid,
                                                       HakuV1RDTO haku,
                                                       String hakukohdeOid,
                                                       LaskentaActorParams actorParams,
                                                       boolean withHakijaRyhmat,
                                                       Observable<List<ValintaperusteetDTO>> valintaperusteet,
                                                       Observable<List<Oppija>> oppijat,
                                                       Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes,
                                                       Observable<PisteetWithLastModified> valintapisteetForHakukohdes,
                                                       Observable<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat,
                                                       Observable<List<Hakemus>> hakemukset) {
        return wrapAsRunOnlyOnceObservable(combineLatest(
                valintapisteetForHakukohdes,
                hakijaryhmat,
                valintaperusteet,
                hakemukset,
                oppijat,
                hakukohdeRyhmasForHakukohdes,
                (vp, hr, v, h, o, r) -> {
                    LOG.info("(Uuid: {}) Kaikki resurssit hakukohteelle {} saatu. Kootaan ja palautetaan LaskeDTO.", uuid, hakukohdeOid);
                    if(!withHakijaRyhmat) {
                        return new LaskeDTO(
                                uuid,
                                haku.isKorkeakouluHaku(),
                                actorParams.isErillishaku(),
                                hakukohdeOid,
                                HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, hakukohdeOid, r, h, vp.valintapisteet, o, actorParams.getParametritDTO(), true), v);

                    } else {
                        return new LaskeDTO(
                                uuid,
                                haku.isKorkeakouluHaku(),
                                actorParams.isErillishaku(),
                                hakukohdeOid,
                                HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, hakukohdeOid, r, h, vp.valintapisteet, o, actorParams.getParametritDTO(), true), v, hr);
                    }}));
    }

    private Observable<LaskeDTO> getLaskeDTOObservableForAtaruHakemukset(String uuid,
                                                                         HakuV1RDTO haku,
                                                                         String hakukohdeOid,
                                                                         LaskentaActorParams actorParams,
                                                                         boolean withHakijaRyhmat,
                                                                         Observable<List<ValintaperusteetDTO>> valintaperusteet,
                                                                         Observable<List<Oppija>> oppijat,
                                                                         Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes,
                                                                         Observable<PisteetWithLastModified> valintapisteetForHakukohdes,
                                                                         Observable<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat,
                                                                         Observable<List<AtaruHakemus>> hakemukset) {
        return wrapAsRunOnlyOnceObservable(combineLatest(
                valintapisteetForHakukohdes,
                hakijaryhmat,
                valintaperusteet,
                hakemukset,
                oppijat,
                hakukohdeRyhmasForHakukohdes,
                (vp, hr, v, h, o, r) -> {
                    LOG.info("(Uuid: {}) Kaikki resurssit hakukohteelle {} saatu. Kootaan ja palautetaan LaskeDTO.", uuid, hakukohdeOid);
                    if(!withHakijaRyhmat) {
                        return new LaskeDTO(
                                uuid,
                                haku.isKorkeakouluHaku(),
                                actorParams.isErillishaku(),
                                hakukohdeOid,
                                HakemuksetConverterUtil.muodostaHakemuksetDTOfromAtaruHakemukset(haku, hakukohdeOid, r, h, vp.valintapisteet, o, actorParams.getParametritDTO(), true), v);

                    } else {
                        return new LaskeDTO(
                                uuid,
                                haku.isKorkeakouluHaku(),
                                actorParams.isErillishaku(),
                                hakukohdeOid,
                                HakemuksetConverterUtil.muodostaHakemuksetDTOfromAtaruHakemukset(haku, hakukohdeOid, r, h, vp.valintapisteet, o, actorParams.getParametritDTO(), true), v, hr);
                    }}));
    }

    private Observable<LaskeDTO> fetchResourcesForOneLaskenta(final AuditSession auditSession,
                                                              final String uuid,
                                                              HakuV1RDTO haku,
                                                              final String hakukohdeOid,
                                                              LaskentaActorParams actorParams,
                                                              boolean retryHakemuksetAndOppijat,
                                                              boolean withHakijaRyhmat) {
        final String hakuOid = haku.getOid();

        PyynnonTunniste tunniste = new PyynnonTunniste("Please put individual resource source identifier here!", uuid, hakukohdeOid);
        Observable<List<ValintaperusteetDTO>> valintaperusteet = createResurssiObservable(tunniste,
            "valintaperusteetAsyncResource.haeValintaperusteet",
            valintaperusteetAsyncResource.haeValintaperusteet(hakukohdeOid, actorParams.getValinnanvaihe()));
        Observable<List<Oppija>> oppijat = createResurssiObservable(tunniste,
            "suoritusrekisteriAsyncResource.getOppijatByHakukohde",
            suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid),
            retryHakemuksetAndOppijat);
        Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes = createResurssiObservable(tunniste,
            "tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes",
            tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid));
        Observable<PisteetWithLastModified> valintapisteetForHakukohdes = createResurssiObservable(tunniste,
            "valintapisteAsyncResource.getValintapisteet",
            valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession));
        Observable<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat = withHakijaRyhmat ? createResurssiObservable(tunniste,
            "valintaperusteetAsyncResource.haeHakijaryhmat",
            valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid)) : just(emptyList());

        if (StringUtils.isNotEmpty(haku.getAtaruLomakeAvain())) {
            Observable<List<AtaruHakemus>> hakemukset = createResurssiObservable(tunniste,
                    "applicationAsyncResource.getAtaruApplicationsByHakukohde",
                    applicationAsyncResource.getAtaruApplicationsByHakukohde(hakukohdeOid),
                    retryHakemuksetAndOppijat);
            LOG.info("(Uuid: {}) Odotetaan kaikkien resurssihakujen valmistumista hakukohteelle {}, jotta voidaan palauttaa ne yhtenä pakettina.", uuid, hakukohdeOid);
            return getLaskeDTOObservableForAtaruHakemukset(uuid, haku, hakukohdeOid, actorParams, withHakijaRyhmat, valintaperusteet, oppijat, hakukohdeRyhmasForHakukohdes, valintapisteetForHakukohdes, hakijaryhmat, hakemukset);
        } else {
            Observable<List<Hakemus>> hakemukset = createResurssiObservable(tunniste,
                "applicationAsyncResource.getApplicationsByOid",
                applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid),
                retryHakemuksetAndOppijat);
            LOG.info("(Uuid: {}) Odotetaan kaikkien resurssihakujen valmistumista hakukohteelle {}, jotta voidaan palauttaa ne yhtenä pakettina.", uuid, hakukohdeOid);
            return getLaskeDTOObservable(uuid, haku, hakukohdeOid, actorParams, withHakijaRyhmat, valintaperusteet, oppijat, hakukohdeRyhmasForHakukohdes, valintapisteetForHakukohdes, hakijaryhmat, hakemukset);
        }
    }

    private <T> Observable<T> createResurssiObservable(PyynnonTunniste tunniste, String resurssi, Observable<T> sourceObservable, boolean retry) {
        return new LaskentaResurssinhakuObservable<>(sourceObservable, tunniste.withNimi(resurssi), retry).getObservable();
    }

    private <T> Observable<T> createResurssiObservable(PyynnonTunniste tunniste, String resurssi, Observable<T> sourceObservable) {
        return createResurssiObservable(tunniste, resurssi, sourceObservable, false);
    }
}
