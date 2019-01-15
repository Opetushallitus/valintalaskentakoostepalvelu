package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.ei_osallistunut;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.tyhja;
import static org.apache.commons.collections.ListUtils.union;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.jasig.cas.client.util.CommonUtils.isNotEmpty;
import com.google.common.collect.Sets;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.User;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeCreateDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.sharedutils.AuditLog;
import fi.vm.sade.sharedutils.ValintaResource;
import fi.vm.sade.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.HakukohdeHelper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviKuuntelija;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AmmatillisenKielikoetulosOperations.CompositeCommand;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.observables.ConnectableObservable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPistesyottoKoosteService {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPistesyottoKoosteService.class);
    public static final String OPPILAITOS = fi.vm.sade.organisaatio.api.model.types.OrganisaatioTyyppi.OPPILAITOS.name().toUpperCase();

    public static String KIELIKOE_SUORITUS_TILA = "VALMIS";
    public static String KIELIKOE_ARVOSANA_AINE = "kielikoe";
    public static String KIELIKOE_SUORITUS_YKSILOLLISTAMINEN = "Ei";
    public static String KIELIKOE_KEY_PREFIX = "kielikoe_";

    protected final ApplicationAsyncResource applicationAsyncResource;
    protected final AtaruAsyncResource ataruAsyncResource;
    protected final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    protected final TarjontaAsyncResource tarjontaAsyncResource;
    protected final OhjausparametritAsyncResource ohjausparametritAsyncResource;
    protected final OrganisaatioAsyncResource organisaatioAsyncResource;
    protected final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    protected final ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;
    protected final ValintapisteAsyncResource valintapisteAsyncResource;

    protected AbstractPistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                               AtaruAsyncResource ataruAsyncResource,
                                               ValintapisteAsyncResource valintapisteAsyncResource,
                                               SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                               TarjontaAsyncResource tarjontaAsyncResource,
                                               OhjausparametritAsyncResource ohjausparametritAsyncResource,
                                               OrganisaatioAsyncResource organisaatioAsyncResource,
                                               ValintaperusteetAsyncResource valintaperusteetAsyncResource,
                                               ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.ataruAsyncResource = ataruAsyncResource;
        this.valintapisteAsyncResource = valintapisteAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
    }

    private PistesyottoExcel muodostoPistesyottoExcel(String hakuOid,
                                                      String hakukohdeOid,
                                                      Optional<String> aikaleima,
                                                      List<ValintakoeOsallistuminenDTO> osallistumistiedot,
                                                      List<HakemusWrapper> hakemukset,
                                                      List<ValintaperusteDTO> valintaperusteet,
                                                      List<ApplicationAdditionalDataDTO> pistetiedot,
                                                      List<HakukohdeJaValintakoeDTO> hakukohdeJaValintakoe,
                                                      HakuV1RDTO hakuV1RDTO,
                                                      HakukohdeV1RDTO hakukohdeDTO,
                                                      Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
        String hakuNimi = new Teksti(hakuV1RDTO.getNimi()).getTeksti();
        String tarjoajaOid = HakukohdeHelper.tarjoajaOid(hakukohdeDTO);
        String hakukohdeNimi = new Teksti(hakukohdeDTO.getHakukohteenNimet()).getTeksti();
        String tarjoajaNimi = new Teksti(hakukohdeDTO.getTarjoajaNimet()).getTeksti();
        Collection<String> valintakoeTunnisteet = valintaperusteet.stream()
                .map(ValintaperusteDTO::getTunniste)
                .collect(Collectors.toList());

        Set<String> kaikkiKutsutaanTunnisteet = hakukohdeJaValintakoe.stream()
                .flatMap(h -> h.getValintakoeDTO().stream())
                .filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki()))
                .map(ValintakoeCreateDTO::getTunniste)
                .collect(Collectors.toSet());

        return new PistesyottoExcel(hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi, hakukohdeNimi, tarjoajaNimi, aikaleima,
                hakemukset, kaikkiKutsutaanTunnisteet, valintakoeTunnisteet, osallistumistiedot, valintaperusteet,
                pistetiedot, kuuntelijat);
    }

    protected Observable<List<HakemusWrapper>> getHakemuksetByOids(List<String> hakemusOids) {
        return ataruAsyncResource.getApplicationsByOids(hakemusOids)
                .flatMap(hakemukset -> {
                    if (hakemukset.isEmpty()) {
                        return applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids);
                    } else {
                        return Observable.just(hakemukset);
                    }
                });
    }

    private Observable<List<HakemusWrapper>> getHakemukset(HakuV1RDTO haku, String hakukohdeOid) {
        if (StringUtils.isEmpty(haku.getAtaruLomakeAvain())) {
            return applicationAsyncResource.getApplicationsByOid(haku.getOid(), hakukohdeOid);
        } else {
            return ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid);
        }
    }

    protected Observable<Triple<PistesyottoExcel, Map<String, ApplicationAdditionalDataDTO>, Map<String, HakemusWrapper>>> muodostaPistesyottoExcel(
            String hakuOid,
            String hakukohdeOid,
            AuditSession auditSession,
            DokumenttiProsessi prosessi,
            Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
        BiFunction<List<ValintakoeOsallistuminenDTO>, PisteetWithLastModified, Observable<PisteetWithLastModified>> haePuuttuvatLisatiedot = (osallistumiset, lisatiedot) -> {
            final Map<String, ValintakoeOsallistuminenDTO> osallistuminenByHakemusOID = osallistumiset.stream().collect(Collectors.toMap(o -> o.getHakemusOid(), o -> o));
            Sets.SetView<String> puuttuvatLisatiedot = Sets.difference(osallistuminenByHakemusOID.keySet(), lisatiedot.valintapisteet.stream().map(l -> l.getHakemusOID()).collect(Collectors.toSet()));
            if (puuttuvatLisatiedot.isEmpty()) {
                return Observable.just(lisatiedot);
            }
            prosessi.inkrementoiKokonaistyota();
            Function<Valintapisteet, Valintapisteet> populateNameAndOppijaOID = v -> {
                ValintakoeOsallistuminenDTO o = osallistuminenByHakemusOID.get(v.getHakemusOID());
                return new Valintapisteet(v.getHakemusOID(), o.getHakijaOid(), o.getEtunimi(), o.getSukunimi(), v.getPisteet());
            };

            return valintapisteAsyncResource.getValintapisteet(puuttuvatLisatiedot, auditSession).map(v ->
                    v.valintapisteet.stream().map(populateNameAndOppijaOID).collect(Collectors.toList())).map(p ->
                    new PisteetWithLastModified(lisatiedot.lastModified, union(lisatiedot.valintapisteet, p))).doOnComplete(prosessi::inkrementoiTehtyjaToita);
        };
        BiFunction<List<ValintakoeOsallistuminenDTO>, List<HakemusWrapper>, Observable<List<HakemusWrapper>>> haePuuttuvatHakemukset = (osallistumiset, hakemukset) -> {
            Set<String> puuttuvatHakemukset = osallistumiset.stream().map(ValintakoeOsallistuminenDTO::getHakemusOid).collect(Collectors.toSet());
            puuttuvatHakemukset.removeAll(hakemukset.stream().map(HakemusWrapper::getOid).collect(Collectors.toSet()));
            if (puuttuvatHakemukset.isEmpty()) {
                return Observable.just(hakemukset);
            }
            prosessi.inkrementoiKokonaistyota();
            return getHakemuksetByOids(new ArrayList<>(puuttuvatHakemukset))
                    .map(hs -> Stream.concat(hakemukset.stream(), hs.stream()).collect(Collectors.toList()))
                    .doOnComplete(prosessi::inkrementoiTehtyjaToita);

        };
        io.reactivex.functions.Function<List<ValintaperusteDTO>, Observable<List<Oppija>>> haeKielikoetulokset = kokeet -> {
            if (kokeet.stream().map(ValintaperusteDTO::getTunniste).anyMatch(t -> t.matches(PistesyottoExcel.KIELIKOE_REGEX))) {
                prosessi.inkrementoiKokonaistyota();
                return suoritusrekisteriAsyncResource.getOppijatByHakukohdeWithoutEnsikertalaisuus(hakukohdeOid, hakuOid)
                        .doOnComplete(prosessi::inkrementoiTehtyjaToita);
            } else {
                return Observable.just(new ArrayList<>());
            }
        };
        ConnectableObservable<List<ValintaperusteDTO>> kokeetO = valintaperusteetAsyncResource.findAvaimet(hakukohdeOid).replay(1);
        ConnectableObservable<List<ValintakoeOsallistuminenDTO>> osallistumistiedotO = valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid).replay(1);
        Observable<PisteetWithLastModified> merge = Observable.merge(Observable.zip(
                osallistumistiedotO,
                valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession),
                haePuuttuvatLisatiedot
        ));
        Observable<Pair<Optional<String>, List<ApplicationAdditionalDataDTO>>> lisatiedotO = Observable.zip(
                merge,
                kokeetO,
                osallistumistiedotO,
                kokeetO.flatMap(haeKielikoetulokset),
                ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid),
                (lisatiedot, kokeet, osallistumistiedot, kielikoetulokset, ohjausparametrit) -> {
                    Map<String, ValintakoeOsallistuminenDTO> osallistumistiedotByHakemusOid = osallistumistiedot.stream()
                            .collect(Collectors.toMap(ValintakoeOsallistuminenDTO::getHakemusOid, o -> o));
                    Map<String, Oppija> kielikoetuloksetByPersonOid = kielikoetulokset.stream()
                            .collect(Collectors.toMap(Oppija::getOppijanumero, o -> o));
                    return Pair.of(lisatiedot.lastModified, lisatiedot.valintapisteet.stream().map(l ->
                            new HakemuksenKoetulosYhteenveto(
                                    l,
                                    Pair.of(hakukohdeOid, kokeet),
                                    osallistumistiedotByHakemusOid.get(l.getHakemusOID()),
                                    kielikoetuloksetByPersonOid.get(l.getOppijaOID()),
                                    ohjausparametrit
                            ).applicationAdditionalDataDTO
                    ).collect(Collectors.toList()));
                }
        );
        Observable<HakuV1RDTO> hakuO = tarjontaAsyncResource.haeHaku(hakuOid);
        Observable<List<HakemusWrapper>> hakemuksetO = Observable.merge(Observable.zip(
                osallistumistiedotO,
                hakuO.flatMap(haku -> getHakemukset(haku, hakukohdeOid)),
                haePuuttuvatHakemukset
        ));

        prosessi.inkrementoiKokonaistyota();
        kokeetO.connect();
        osallistumistiedotO.connect();
        return Observable.zip(
                osallistumistiedotO,
                lisatiedotO,
                hakemuksetO,
                kokeetO,
                valintaperusteetAsyncResource.haeValintakokeetHakutoiveille(Collections.singletonList(hakukohdeOid)),
                tarjontaAsyncResource.haeHakukohde(hakukohdeOid),
                hakuO,
                (osallistumistiedot, lisatiedot, hakemukset, kokeet, valintakoeosallistumiset, hakukohde, haku) -> Triple.of(
                        muodostoPistesyottoExcel(hakuOid, hakukohdeOid, lisatiedot.getKey(), osallistumistiedot, hakemukset, kokeet,
                                lisatiedot.getRight(), valintakoeosallistumiset, haku, hakukohde, kuuntelijat),
                        lisatiedot.getRight().stream().collect(Collectors.toMap(ApplicationAdditionalDataDTO::getOid, l -> l)),
                        hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h))
                )
        ).doOnComplete(prosessi::inkrementoiTehtyjaToita);
    }

    protected Observable<Set<String>> tallennaKoostetutPistetiedot(String hakuOid,
                                                                   String hakukohdeOid,
                                                                   Optional<String> ifUnmodifiedSince,
                                                                   List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                                   Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                                   ValintaperusteetOperation auditLogOperation,
                                                                   AuditSession auditSession) {
        Observable<String> kielikoeTallennus = Observable.zip(
                findSourceOid(hakukohdeOid),
                haeOppijatSuresta(hakuOid, hakukohdeOid),
                Pair::of)
                .flatMap(sourceAndOppijat -> {
                    String sourceOid = sourceAndOppijat.getLeft();
                    List<Oppija> oppijatSuresta = sourceAndOppijat.getRight();
                    return tallennaKielikoetulokset(hakuOid, hakukohdeOid, sourceOid, pistetiedotHakemukselle,
                        kielikoetuloksetSureen, auditSession.getPersonOid(), auditLogOperation, auditSession.asAuditUser(), oppijatSuresta);
                });

        return kielikoeTallennus.flatMap(a ->
            tallennaPisteetValintaPisteServiceen(hakuOid, hakukohdeOid, ifUnmodifiedSince, pistetiedotHakemukselle, auditLogOperation, auditSession))
            .onErrorResumeNext((Throwable t) -> Observable.error(
                new IllegalStateException(String.format("Virhe tallennettaessa koostettuja pistetietoja haun %s hakukohteelle %s", hakuOid, hakukohdeOid), t)));
    }

    private Observable<String> tallennaKielikoetulokset(String hakuOid, String hakukohdeOid, String sourceOid,
                                                      List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                      Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                      String username, ValintaperusteetOperation auditLogOperation,
                                                      User user,
                                                      List<Oppija> oppijatSuresta) {
        Function<String,String> findPersonOidByHakemusOid = hakemusOid -> pistetiedotHakemukselle.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
        AmmatillisenKielikoetulosOperations operations = new AmmatillisenKielikoetulosOperations(sourceOid, oppijatSuresta, kielikoetuloksetSureen, findPersonOidByHakemusOid);

        Map<String, Optional<CompositeCommand>> resultsToSendToSure = operations.getResultsToSendToSure();
        if (resultsToSendToSure.isEmpty()) {
            LOG.info(String.format("Näyttää siltä, että kaikki %d ammatillisen kielikokeen tulostietoa Suoritusrekisterissä " +
                "ovat jo ajan tasalla, ei päivitetä.", kielikoetuloksetSureen.size()));
            return Observable.just("OK");
        }

        return Observable.fromIterable(resultsToSendToSure.keySet()).flatMap(hakemusOid -> {
            String personOid = findPersonOidByHakemusOid.apply(hakemusOid);
            Optional<CompositeCommand> operationOptional = resultsToSendToSure.get(hakemusOid);
            if (!operationOptional.isPresent()) {
                return Observable.just("OK");
            }

            CompositeCommand compositeCommandForHakemus = operationOptional.get();
            Observable<Arvosana> sureOperations = compositeCommandForHakemus.createSureOperation(suoritusrekisteriAsyncResource)
                .onErrorResumeNext((Throwable t) -> Observable.error(new IllegalStateException(String.format(
                    "Virhe hakemuksen %s tulosten tallentamisessa Suoritusrekisteriin ", hakemusOid), t)));

            return sureOperations.doOnNext(processedArvosana -> {
                Map<String, String> additionalAuditInfo = new HashMap<>();
                additionalAuditInfo.put("usernameForAudit", username);
                additionalAuditInfo.put("hakuOid", hakuOid);
                additionalAuditInfo.put("hakukohdeOid", hakukohdeOid);
                additionalAuditInfo.put("hakijaOid", personOid);
                additionalAuditInfo.put("hakemusOid", hakemusOid);
                additionalAuditInfo.put(KIELIKOE_KEY_PREFIX + processedArvosana.getLisatieto().toLowerCase(), processedArvosana.getArvio().getArvosana());
                AuditLog.log(KoosteAudit.AUDIT,
                    user,
                    auditLogOperation,
                    ValintaResource.PISTESYOTTOSERVICE,
                    processedArvosana.getId(),
                    Changes.addedDto(processedArvosana),
                    additionalAuditInfo);
            });
        }).last(new Arvosana()).<String>map(x -> "OK").doAfterSuccess(aVoid -> LOG.info("Kielikoetietojen tallennus Suoritusrekisteriin onnistui")).toObservable();
    }



    private List<Valintapisteet> pistetiedotHakemukselle(String tallettaja, List<ApplicationAdditionalDataDTO> additionalDataDTOS) {
        return additionalDataDTOS.stream().map(a -> Pair.of(tallettaja, a)).map(Valintapisteet::new).collect(Collectors.toList());
    }

    private Observable<Set<String>> tallennaPisteetValintaPisteServiceen(String hakuOid,
                                                                         String hakukohdeOid,
                                                                         Optional<String> ifUnmodifiedSince,
                                                                         List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                                         ValintaperusteetOperation auditLogOperation,
                                                                         AuditSession auditSession) {
        return valintapisteAsyncResource.putValintapisteet(ifUnmodifiedSince, pistetiedotHakemukselle(auditSession.getPersonOid(), pistetiedotHakemukselle), auditSession)
                //.<Void>map(a -> null)
                .doOnNext(conflictingHakemusOids ->
                        pistetiedotHakemukselle.forEach(pistetieto -> {
                                    Map <String, String> additionalInfo = new HashMap<>();
                                    additionalInfo.put("Username from call params", auditSession.getPersonOid());
                                    additionalInfo.put("hakuOid", hakuOid);
                                    additionalInfo.put("hakukohdeOid", hakukohdeOid);
                                    additionalInfo.put("hakijaOid", pistetieto.getPersonOid());
                                    AuditLog.log(KoosteAudit.AUDIT, auditSession.asAuditUser(), auditLogOperation, ValintaResource.PISTESYOTTOSERVICE, pistetieto.getOid(), Changes.addedDto(pistetieto), additionalInfo);
                                }
                        ))
                            .onErrorResumeNext((Throwable t) -> Observable.error(new IllegalStateException(
                                    "Lisätietojen tallennus hakemukselle epäonnistui", t)));
    }

    private Observable<List<Oppija>> haeOppijatSuresta(String hakuOid, String hakukohdeOid) {
        return suoritusrekisteriAsyncResource.getOppijatByHakukohdeWithoutEnsikertalaisuus(hakukohdeOid, hakuOid)
                .onErrorResumeNext((Throwable t) -> Observable.error(new IllegalStateException(String.format(
                        "Oppijoiden haku Suoritusrekisteristä haun %s hakukohteelle %s epäonnistui",
                        hakuOid,
                        hakukohdeOid
                ), t)));
    }

    private Observable<String> findSourceOid(String hakukohdeOid) {
        return tarjontaAsyncResource.haeHakukohde(hakukohdeOid).flatMap(hakukohde -> {
            Optional<String> tarjoajaOid = hakukohde.getTarjoajaOids().stream().findFirst();
            if (tarjoajaOid.isPresent()) {
                return organisaatioAsyncResource.haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(tarjoajaOid.get()).flatMap(hierarkia -> {
                    if (hierarkia == null) {
                        return Observable.error(new IllegalStateException(String.format(
                                "Hakukohteen %s tarjoajalle %s ei löytynyt organisaatiohierarkiaa.",
                                hakukohdeOid,
                                tarjoajaOid
                        )));
                    }
                    AtomicReference<String> sourceRef = new AtomicReference<>();
                    etsiOppilaitosHierarkiasta(tarjoajaOid.get(), hierarkia.getOrganisaatiot(), sourceRef);
                    if (isEmpty(sourceRef.get())) {
                        return Observable.error(new IllegalStateException(String.format(
                                "Hakukohteen %s suoritukselle ei löytynyt lähdettä, tarjoaja on %s ja sillä %s organisaatiota.",
                                hakukohdeOid,
                                tarjoajaOid,
                                hierarkia.getOrganisaatiot().size()
                        )));
                    }
                    return Observable.just(sourceRef.get());
                });
            } else {
                return Observable.error(new IllegalStateException(String.format(
                        "Hakukohteella %s ei ole tarjoajaa.", hakukohdeOid)));
            }
        });
    }

    public void etsiOppilaitosHierarkiasta(String tarjoajaOid, List<OrganisaatioTyyppi> tasonOrganisaatiot, AtomicReference<String> myontajaRef) {
        etsiOppilaitosHierarkiasta(tarjoajaOid, tasonOrganisaatiot, myontajaRef, false);
    }

    private AtomicReference<String> etsiOppilaitosHierarkiasta(String tarjoajaOid, List<OrganisaatioTyyppi> taso, AtomicReference<String> oppilaitosRef, boolean tarjoajaLevelReached) {
        Optional<OrganisaatioTyyppi> oppilaitos = taso.stream().filter(ot -> ot.getOrganisaatiotyypit().contains(OPPILAITOS)).findFirst();
        oppilaitos.ifPresent(organisaatioTyyppi -> oppilaitosRef.set(organisaatioTyyppi.getOid()));

        Optional<OrganisaatioTyyppi> tarjoaja = taso.stream().filter(ot -> ot.getOid().equals(tarjoajaOid)).findFirst();
        tarjoajaLevelReached = tarjoajaLevelReached || tarjoaja.isPresent();

        if (isNotEmpty(oppilaitosRef.get()) && tarjoajaLevelReached) {
            return oppilaitosRef;
        }
        if (tarjoaja.isPresent() && isEmpty(oppilaitosRef.get())) {
            LOG.warn(String.format("Ei löytynyt %s -tyyppistä organisaatiota tarjoajan %s tasolta tai ylempää, etsitään organisaatiohierarkian alemmilta tasoilta.",
                OPPILAITOS, tarjoajaOid));
            tarjoajaLevelReached = true;
        }
        List<OrganisaatioTyyppi> seuraavaTaso = taso.stream().map(OrganisaatioTyyppi::getChildren).flatMap(Collection::stream).collect(Collectors.toList());
        if (seuraavaTaso.size() == 0) {
            return oppilaitosRef;
        }
        return etsiOppilaitosHierarkiasta(tarjoajaOid, seuraavaTaso, oppilaitosRef, tarjoajaLevelReached);
    }

    protected void siirraKielikoepistetiedotKielikoetulosMapiin(Date valmistuminen, Map<String, List<SingleKielikoeTulos>> uudetKielikoetulokset, String hakemusOid, Map<String, String> newPistetiedot) {
        List<String> kielikoeAvaimet = newPistetiedot.keySet().stream().filter(a -> a.matches(PistesyottoExcel.KIELIKOE_REGEX)).collect(Collectors.toList());
        if(0 < kielikoeAvaimet.size()) {
            uudetKielikoetulokset.put(hakemusOid, kielikoeAvaimet.stream().map(avain -> {
                String osallistuminenAvain = avain + "-OSALLISTUMINEN";
                String osallistumisArvo = newPistetiedot.get(osallistuminenAvain);
                if ("OSALLISTUI".equals(osallistumisArvo)) {
                    String tulosArvo = newPistetiedot.get(avain);
                    if ("true".equals(tulosArvo)) {
                        return new SingleKielikoeTulos(avain, hyvaksytty, valmistuminen);
                    }
                    if ("false".equals(tulosArvo)) {
                        return new SingleKielikoeTulos(avain, hylatty, valmistuminen);
                    }
                    throw new IllegalArgumentException(String.format("Huono arvosana '%s' hakemuksella %s", tulosArvo, hakemusOid));
                }
                if ("EI_OSALLISTUNUT".equals(osallistumisArvo)) {
                    return new SingleKielikoeTulos(avain, ei_osallistunut, valmistuminen);
                }
                if (Arrays.asList("MERKITSEMATTA", "EI_VAADITA").contains(osallistumisArvo)) {
                    return new SingleKielikoeTulos(avain, tyhja, valmistuminen);
                }
                throw new IllegalArgumentException(String.format("Huono osallistumistieto '%s' hakemuksella %s",
                    osallistumisArvo, hakemusOid));
            }).collect(Collectors.toList()));
        }
        kielikoeAvaimet.forEach(newPistetiedot::remove);
    }

    public static class SingleKielikoeTulos {
        public final String kokeenTunnus; // kielikoe_fi , kielikoe_sv
        public final SureHyvaksyttyArvosana arvioArvosana;
        public final Date valmistuminen;

        public SingleKielikoeTulos(String kokeenTunnus, SureHyvaksyttyArvosana arvioArvosana, Date valmistuminen) {
            this.kokeenTunnus = kokeenTunnus;
            this.arvioArvosana = arvioArvosana;
            this.valmistuminen = valmistuminen;
        }

        public String kieli() {
            return kokeenTunnus.replace("kielikoe_", "");
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
