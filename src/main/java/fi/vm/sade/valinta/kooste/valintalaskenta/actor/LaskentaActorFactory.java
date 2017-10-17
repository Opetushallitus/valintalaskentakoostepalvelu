package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.tuple.Pair.of;
import static rx.Observable.combineLatest;
import static rx.Observable.just;
import static rx.Observable.range;
import static rx.Observable.timer;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
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
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
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
import rx.observables.ConnectableObservable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private Observable<Pair<Collection<String>, List<LaskeDTO>>> fetchRecursively(Function<String, Observable<LaskeDTO>> fetchLaskeDTO, Observable<Pair<Collection<String>, List<LaskeDTO>>> o) {
        return o.switchMap(l -> {
            Collection<String> oids = l.getKey();
            if(oids.isEmpty()) {
                return o;
            } else {
                Pair<String, Collection<String>> headWithTail = headAndTail(oids);
                Observable<Pair<Collection<String>, List<LaskeDTO>>> map = fetchLaskeDTO.apply(headWithTail.getKey()).map(laskeDTO -> Pair.of(headWithTail.getRight(), Stream.concat(Stream.of(laskeDTO), l.getRight().stream()).collect(Collectors.toList())));
                return map.switchMap(t -> fetchRecursively(fetchLaskeDTO, Observable.just(t)));
            }
        });
    }

    public LaskentaActor createValintaryhmaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams a) {
        LaskentaActorParams fakeOnlyOneHakukohdeParams = new LaskentaActorParams(a.getLaskentaStartParams(), Arrays.asList(new HakukohdeJaOrganisaatio()), a.getParametritDTO());
        return laskentaHakukohteittainActor(laskentaSupervisor, fakeOnlyOneHakukohdeParams,
                hakukohdeJaOrganisaatio -> {
                    String uuid = a.getUuid();
                    Collection<String> hakukohdeOids = a.getHakukohdeOids().stream().map(hk -> hk.getHakukohdeOid()).collect(Collectors.toList());
                    String hakukohteidenNimi = String.format("Valintaryhmälaskenta %s hakukohteella", hakukohdeOids.size());
                    LOG.info("(Uuid={}) {}", uuid, hakukohteidenNimi);
                    Observable<Pair<Collection<String>, List<LaskeDTO>>> recursiveSequentialFetch = just(of(hakukohdeOids, emptyList()));

                    Function<String, Observable<LaskeDTO>> fetchLaskeDTO = h ->fetchResourcesForOneLaskenta(
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
                    laskenta.subscribe(laskentaOK.apply(uuid, hakukohteidenNimi), laskentaException.apply(uuid, hakukohteidenNimi));
                    return laskenta;
                }
        );
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

    public LaskentaActor createValintakoelaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    Observable<String> laskenta = fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false, false).switchMap(valintalaskentaAsyncResource::valintakokeet);
                    laskenta.subscribe(laskentaOK.apply(uuid, hakukohdeOid), laskentaException.apply(uuid, hakukohdeOid));
                    return laskenta;
                }
        );
    }
    private static final Action1<? super Object> resurssiOK(String resurssi, String uuid, String hakukohde) {
        return r -> LOG.info("(Uuid={}) Saatiin resurssi {} hakukohteelle {}", uuid, resurssi, hakukohde);
    }

    //private static final BiFunction<String, String, Action1<? super Object>> resurssiOK = (uuid, hakukohde) -> r -> LOG.info("(Uuid={}) Saatiin resurssi {} hakukohteelle {}", resurssi, uuid, hakukohde);
    private static final Action1<Throwable> resurssiException(String resurssi, String uuid, String hakukohde) {
        return error -> {
            String message = HttpExceptionWithResponse.appendWrappedResponse(String.format("(Uuid=%s) Resurssin %s lataus epäonnistui hakukohteelle %s", uuid, resurssi, hakukohde)
                    , error);
            LOG.warn(message, error);
        };
    }

    public LaskentaActor createValintalaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);

                    Observable<String> laskenta = fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false, true).switchMap(valintalaskentaAsyncResource::laske);
                    laskenta.subscribe(laskentaOK.apply(uuid, hakukohdeOid), laskentaException.apply(uuid, hakukohdeOid));
                    return laskenta;
                }
        );
    }

    private static final BiFunction<String, String, Action1<? super Object>> laskentaOK = (uuid, hakukohde) -> resurssi -> LOG.info("(Uuid={}) Laskenta onnistui hakukohteelle {}", uuid, hakukohde);
    private static final BiFunction<String, String, Action1<Throwable>> laskentaException = (uuid, hakukohde) -> error -> {
        String message = HttpExceptionWithResponse.appendWrappedResponse(String.format("(Uuid=%s) Laskenta epäonnistui hakukohteelle %s", uuid, hakukohde), error);
        LOG.warn(message, error);
    };

    public LaskentaActor createValintalaskentaJaValintakoelaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan + valintakoelaskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
                    Observable<String> laskenta = fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false,true).switchMap(valintalaskentaAsyncResource::laskeKaikki);
                    laskenta.subscribe(laskentaOK.apply(uuid, hakukohdeOid), laskentaException.apply(uuid, hakukohdeOid));
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

    private Func1<Observable<? extends Throwable>, Observable<?>> createRetryer() {
        int maxRetries = 2;
        int secondsToWaitMultiplier = 5;
        return errors -> errors.zipWith(range(1, maxRetries), (n, i) -> i).flatMap(i -> {
            int delaySeconds = secondsToWaitMultiplier * i;
            LOG.warn(toString() + " retry number " + i + "/" + maxRetries + ", waiting for " + delaySeconds + " seconds.");
            return timer(delaySeconds, TimeUnit.SECONDS);
        });
    }

    private Observable<LaskeDTO> fetchResourcesForOneLaskenta(final AuditSession auditSession, final String uuid, HakuV1RDTO haku,
                                                              final String hakukohdeOid,
                                                              LaskentaActorParams actorParams,
                                                              boolean retry,
                                                              boolean withHakijaRyhmat) {
        LOG.info("(Uuid={}) Haetaan laskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
        final String hakuOid = haku.getOid();
        Observable<List<ValintaperusteetDTO>> valintaperusteet = valintaperusteetAsyncResource.haeValintaperusteet(hakukohdeOid, actorParams.getValinnanvaihe());
        valintaperusteet.subscribe(resurssiOK("valintaperusteetAsyncResource.haeValintaperusteet", uuid, hakukohdeOid), resurssiException("valintaperusteetAsyncResource.haeValintaperusteet", uuid, hakukohdeOid));
        Observable<List<Hakemus>> hakemukset = applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid);
        if(retry) {
            hakemukset = hakemukset.retryWhen(createRetryer());
        }
        hakemukset.subscribe(resurssiOK("applicationAsyncResource.getApplicationsByOid", uuid, hakukohdeOid), resurssiException("applicationAsyncResource.getApplicationsByOid", uuid, hakukohdeOid));
        Observable<List<Oppija>> oppijat = suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid);
        if(retry) {
            oppijat = oppijat.retryWhen(createRetryer());
        }
        oppijat.subscribe(resurssiOK("suoritusrekisteriAsyncResource.getOppijatByHakukohde", uuid, hakukohdeOid), resurssiException("suoritusrekisteriAsyncResource.getOppijatByHakukohde", uuid, hakukohdeOid));
        Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes = tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid);
        hakukohdeRyhmasForHakukohdes.subscribe(resurssiOK("tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes", uuid, hakukohdeOid), resurssiException("tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes", uuid, hakukohdeOid));
        Observable<PisteetWithLastModified> valintapisteetForHakukohdes = valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession);
        valintapisteetForHakukohdes.subscribe(resurssiOK("tarjontaAsyncResource.valintapisteAsyncResource", uuid, hakukohdeOid), resurssiException("tarjontaAsyncResource.valintapisteAsyncResource", uuid, hakukohdeOid));
        Observable<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat = withHakijaRyhmat ? valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid) : just(emptyList());
        hakijaryhmat.subscribe(resurssiOK("valintaperusteetAsyncResource.haeHakijaryhmat",uuid, hakukohdeOid), resurssiException("valintaperusteetAsyncResource.haeHakijaryhmat", uuid, hakukohdeOid));

        return wrapAsRunOnlyOnceObservable(combineLatest(
                valintapisteetForHakukohdes,
                hakijaryhmat,
                valintaperusteet,
                hakemukset,
                oppijat,
                hakukohdeRyhmasForHakukohdes,
                (vp, hr, v, h, o, r) -> {if(!withHakijaRyhmat) {
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
}
