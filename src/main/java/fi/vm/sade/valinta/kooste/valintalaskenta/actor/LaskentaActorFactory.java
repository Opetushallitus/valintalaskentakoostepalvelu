package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static fi.vm.sade.valinta.sharedutils.http.ObservableUtil.wrapAsRunOnlyOnceObservable;
import static io.reactivex.Observable.just;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.tuple.Pair.of;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaResurssinhakuFuture.PyynnonTunniste;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@ManagedResource(objectName = "OPH:name=LaskentaActorFactory", description = "LaskentaActorFactory mbean")
public class LaskentaActorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorFactory.class);

    private final ValintapisteAsyncResource valintapisteAsyncResource;
    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final AtaruAsyncResource ataruAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final KoskiService koskiService;
    private volatile int splittaus;

    @Autowired
    public LaskentaActorFactory(
        @Value("${valintalaskentakoostepalvelu.laskennan.splittaus:1}") int splittaus,
        ValintalaskentaAsyncResource valintalaskentaAsyncResource,
        ApplicationAsyncResource applicationAsyncResource,
        AtaruAsyncResource ataruAsyncResource,
        ValintaperusteetAsyncResource valintaperusteetAsyncResource,
        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
        TarjontaAsyncResource tarjontaAsyncResource,
        ValintapisteAsyncResource valintapisteAsyncResource,
        KoskiService koskiService
    ) {
        this.splittaus = splittaus;
        this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource  = ataruAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.valintapisteAsyncResource = valintapisteAsyncResource;
        this.koskiService = koskiService;
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

                    Function<String, Observable<LaskeDTO>> fetchLaskeDTO = h -> Observable.fromFuture(fetchResourcesForOneLaskenta(
                            auditSession, uuid, haku, h, a, true, true));

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
                    Observable<String> laskenta = Observable.fromFuture(fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false, false))
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

    private <A, T> io.reactivex.functions.Function<A, Observable<T>> timedSwitchMap(BiConsumer<Long, Optional<Throwable>> log, Function<A, Observable<T>> f) {//Function<A,Observable<T>> switchMap) {
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


                    Observable<String> laskenta = Observable.fromFuture(fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false, true))
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

    private static final BiFunction<String, String, Consumer<? super Object>> laskentaOK = (uuid, hakukohde) -> resurssi -> LOG.info("(Uuid={}) Laskenta onnistui hakukohteelle {}", uuid, hakukohde);
    private static final BiFunction<String, String, Consumer<Throwable>> laskentaException = (uuid, hakukohde) -> error -> {
        String message = HttpExceptionWithResponse.appendWrappedResponse(String.format("(Uuid=%s) Laskenta epäonnistui hakukohteelle %s", uuid, hakukohde), error);
        LOG.error(message, error);
    };

    private LaskentaActor createValintalaskentaJaValintakoelaskentaActor(AuditSession auditSession, LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final String uuid = actorParams.getUuid();
        return laskentaHakukohteittainActor(laskentaSupervisor, actorParams,
                hakukohdeJaOrganisaatio -> {
                    String hakukohdeOid = hakukohdeJaOrganisaatio.getHakukohdeOid();
                    LOG.info("(Uuid={}) Haetaan laskennan + valintakoelaskennan resursseja hakukohteelle {}", uuid, hakukohdeOid);
                    Observable<String> laskenta = Observable.fromFuture(fetchResourcesForOneLaskenta(auditSession, uuid, haku, hakukohdeOid, actorParams, false,true))
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

    private LaskentaActor laskentaHakukohteittainActor(LaskentaSupervisor laskentaSupervisor,
                                                       LaskentaActorParams actorParams,
                                                       io.reactivex.functions.Function<? super HakukohdeJaOrganisaatio, ? extends Observable<?>> r) {
        return new LaskentaActorForSingleHakukohde(actorParams, r, laskentaSupervisor, laskentaSeurantaAsyncResource, splittaus);
    }

    private CompletableFuture<LaskeDTO> getLaskeDtoFuture(String uuid,
                                                          HakuV1RDTO haku,
                                                          String hakukohdeOid,
                                                          LaskentaActorParams actorParams,
                                                          boolean withHakijaRyhmat,
                                                          CompletableFuture<List<ValintaperusteetDTO>> valintaperusteetF,
                                                          CompletableFuture<List<Oppija>> oppijatF,
                                                          CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdesF,
                                                          CompletableFuture<PisteetWithLastModified> valintapisteetForHakukohdesF,
                                                          CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmatF,
                                                          CompletableFuture<List<HakemusWrapper>> hakemuksetF,
                                                          CompletableFuture<Map<String, KoskiOppija>> koskiOppijaByOppijaOidF) {
        return CompletableFuture.allOf(
            valintapisteetForHakukohdesF,
            hakijaryhmatF,
            valintaperusteetF,
            hakemuksetF,
            oppijatF,
            hakukohdeRyhmasForHakukohdesF,
            koskiOppijaByOppijaOidF)
            .thenApplyAsync(x -> {
                List<ValintaperusteetDTO> valintaperusteet = valintaperusteetF.join();
                verifyValintalaskentaKaytossaOrThrowError(uuid, hakukohdeOid, valintaperusteet);
                verifyJonokriteeritOrThrowError(uuid, hakukohdeOid, valintaperusteet);
                LOG.info("(Uuid: {}) Kaikki resurssit hakukohteelle {} saatu. Kootaan ja palautetaan LaskeDTO.", uuid, hakukohdeOid);

                Map<String, List<String>> ryhmatHakukohteittain = hakukohdeRyhmasForHakukohdesF.join();
                PisteetWithLastModified pisteetWithLastModified = valintapisteetForHakukohdesF.join();
                List<HakemusWrapper> hakemukset = hakemuksetF.join();
                List<Oppija> oppijat = oppijatF.join();
                Map<String, KoskiOppija> koskiOppijatOppijanumeroittain = koskiOppijaByOppijaOidF.join();
                // TODO: Ota koskiOppijatOppijanumeroittain käyttöön
                koskiOppijatOppijanumeroittain.forEach((k, v) -> {
                    LOG.debug(String.format("Koskesta löytyi oppijalle %s datat: %s", k, v));
                });

                if (!withHakijaRyhmat) {
                    return new LaskeDTO(
                        uuid,
                        haku.isKorkeakouluHaku(),
                        actorParams.isErillishaku(),
                        hakukohdeOid,
                        HakemuksetConverterUtil.muodostaHakemuksetDTOfromHakemukset(
                            haku,
                            hakukohdeOid,
                            ryhmatHakukohteittain,
                            hakemukset,
                            pisteetWithLastModified.valintapisteet,
                            oppijat,
                            actorParams.getParametritDTO(),
                            true),
                        valintaperusteet);

                } else {
                    return new LaskeDTO(
                        uuid,
                        haku.isKorkeakouluHaku(),
                        actorParams.isErillishaku(),
                        hakukohdeOid,
                        HakemuksetConverterUtil.muodostaHakemuksetDTOfromHakemukset(
                            haku,
                            hakukohdeOid,
                            ryhmatHakukohteittain,
                            hakemukset,
                            pisteetWithLastModified.valintapisteet,
                            oppijat,
                            actorParams.getParametritDTO(),
                            true),
                        valintaperusteet,
                        hakijaryhmatF.join());
                }
            });
    }

    private void verifyValintalaskentaKaytossaOrThrowError(String uuid, String hakukohdeOid, List<ValintaperusteetDTO> valintaperusteetList) {
        boolean jokinValintatapajonoKayttaaValintalaskentaa = valintaperusteetList
                .stream()
                .map(ValintaperusteetDTO::getValinnanVaihe)
                .flatMap(v -> v.getValintatapajono().stream())
                .anyMatch(ValintatapajonoJarjestyskriteereillaDTO::getKaytetaanValintalaskentaa);

        if (!jokinValintatapajonoKayttaaValintalaskentaa) {
            String errorMessage = String.format("(Uuid: %s) Hakukohteen %s valittujen valinnanvaiheiden valintatapajonoissa ei käytetä valintalaskentaa, joten valintalaskentaa ei voida jatkaa ja se keskeytetään", uuid, hakukohdeOid);
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    private void verifyJonokriteeritOrThrowError(String uuid, String hakukohdeOid, List<ValintaperusteetDTO> valintaperusteetList) {
        Predicate<? super ValintatapajonoJarjestyskriteereillaDTO> valintatapajonoHasPuuttuvaJonokriteeri = new Predicate<>() {
            @Override
            public boolean test(ValintatapajonoJarjestyskriteereillaDTO valintatapajono) {
                boolean kaytetaanValintalaskentaa = valintatapajono.getKaytetaanValintalaskentaa();
                boolean hasJarjestyskriteerit = !valintatapajono.getJarjestyskriteerit().isEmpty();

                return (kaytetaanValintalaskentaa && !hasJarjestyskriteerit)
                        || (!kaytetaanValintalaskentaa && hasJarjestyskriteerit);
            }
        };
        Optional<ValintatapajonoJarjestyskriteereillaDTO> valintatapajonoPuutteellisellaJonokriteerilla = valintaperusteetList
                .stream()
                .map(ValintaperusteetDTO::getValinnanVaihe)
                .flatMap(v -> v.getValintatapajono().stream())
                .filter(valintatapajonoHasPuuttuvaJonokriteeri)
                .findFirst();

        if (valintatapajonoPuutteellisellaJonokriteerilla.isPresent()) {
            ValintatapajonoJarjestyskriteereillaDTO valintatapajono = valintatapajonoPuutteellisellaJonokriteerilla.get();
            String errorMessage = String.format("(Uuid: %s) Hakukohteen %s valintatapajonolla %s on joko valintalaskenta ilman jonokriteereitä tai jonokriteereitä ilman valintalaskentaa, joten valintalaskentaa ei voida jatkaa ja se keskeytetään", uuid, hakukohdeOid, valintatapajono.getOid());
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    private CompletableFuture<LaskeDTO> fetchResourcesForOneLaskenta(final AuditSession auditSession,
                                                              final String uuid,
                                                              HakuV1RDTO haku,
                                                              final String hakukohdeOid,
                                                              LaskentaActorParams actorParams,
                                                              boolean retryHakemuksetAndOppijat,
                                                              boolean withHakijaRyhmat) {
        final String hakuOid = haku.getOid();

        PyynnonTunniste tunniste = new PyynnonTunniste("Please put individual resource source identifier here!", uuid, hakukohdeOid);

        CompletableFuture<List<HakemusWrapper>> hakemukset;
        if (StringUtils.isNotEmpty(haku.getAtaruLomakeAvain())) {
            hakemukset = createResurssiFuture(tunniste,
                    "applicationAsyncResource.getApplications",
                    () -> ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid),
                    retryHakemuksetAndOppijat);
        } else {
            hakemukset = createResurssiFuture(tunniste,
                    "applicationAsyncResource.getApplicationsByOid",
                     () -> applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid),
                    retryHakemuksetAndOppijat);
        }

        CompletableFuture<List<Oppija>> oppijasForOidsFromHakemukses = hakemukset.thenComposeAsync(hws -> {
            List<String> oppijaOids = hws.stream().map(HakemusWrapper::getPersonOid).collect(Collectors.toList());
            LOG.info("Got personOids from hakemukses and getting Oppijas for these: {} for hakukohde {}", oppijaOids.toString(), hakukohdeOid);
            return createResurssiFuture(tunniste,
                    "suoritusrekisteriAsyncResource.getSuorituksetByOppijas",
                    () -> suoritusrekisteriAsyncResource.getSuorituksetByOppijas(oppijaOids, hakuOid),
                    retryHakemuksetAndOppijat);
        });

        CompletableFuture<List<ValintaperusteetDTO>> valintaperusteet = createResurssiFuture(tunniste,
            "valintaperusteetAsyncResource.haeValintaperusteet",
            () -> valintaperusteetAsyncResource.haeValintaperusteet(hakukohdeOid, actorParams.getValinnanvaihe()));
        CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes = createResurssiFuture(tunniste,
            "tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes",
            () -> tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid));
        CompletableFuture<PisteetWithLastModified> valintapisteetForHakukohdes = createResurssiFuture(tunniste,
            "valintapisteAsyncResource.getValintapisteet",
            () -> valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession));
        CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat = withHakijaRyhmat
            ? createResurssiFuture(tunniste, "valintaperusteetAsyncResource.haeHakijaryhmat", () -> valintaperusteetAsyncResource.haeHakijaryhmat(hakukohdeOid))
            : CompletableFuture.completedFuture(emptyList());
        CompletableFuture<Map<String, KoskiOppija>> koskiOppijaByOppijaOid = createResurssiFuture(tunniste,
            "koskiService.haeKoskiOppijat",
            () -> koskiService.haeKoskiOppijat(hakukohdeOid, hakemukset));

        LOG.info("(Uuid: {}) Odotetaan kaikkien resurssihakujen valmistumista hakukohteelle {}, jotta voidaan palauttaa ne yhtenä pakettina.", uuid, hakukohdeOid);
        return getLaskeDtoFuture(
            uuid,
            haku,
            hakukohdeOid,
            actorParams,
            withHakijaRyhmat,
            valintaperusteet,
            oppijasForOidsFromHakemukses,
            hakukohdeRyhmasForHakukohdes,
            valintapisteetForHakukohdes,
            hakijaryhmat,
            hakemukset,
            koskiOppijaByOppijaOid);
    }

    private <T> CompletableFuture<T> createResurssiFuture(PyynnonTunniste tunniste, String resurssi, Supplier<CompletableFuture<T>> sourceFuture, boolean retry) {
        return new LaskentaResurssinhakuFuture<>(sourceFuture, tunniste.withNimi(resurssi), retry).getFuture();
    }

    private <T> CompletableFuture<T> createResurssiFuture(PyynnonTunniste tunniste, String resurssi, Supplier<CompletableFuture<T>> sourceFuture) {
        return createResurssiFuture(tunniste, resurssi, sourceFuture, false);
    }
}
