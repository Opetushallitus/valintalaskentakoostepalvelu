package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.HakukohdeHelper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.PistetietoDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviKuuntelija;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AmmatillisenKielikoetulosOperations.CompositeCommand;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.*;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.jasig.cas.client.util.CommonUtils.isNotEmpty;

public abstract class AbstractPistesyottoKoosteService {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPistesyottoKoosteService.class);
    public static final String OPPILAITOS = fi.vm.sade.organisaatio.api.model.types.OrganisaatioTyyppi.OPPILAITOS.name().toUpperCase();

    public static String KIELIKOE_SUORITUS_TILA = "VALMIS";
    public static String KIELIKOE_ARVOSANA_AINE = "kielikoe";
    public static String KIELIKOE_SUORITUS_YKSILOLLISTAMINEN = "Ei";
    public static String KIELIKOE_KEY_PREFIX = "kielikoe_";

    protected final ApplicationAsyncResource applicationAsyncResource;
    protected final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    protected final TarjontaAsyncResource tarjontaAsyncResource;
    protected final OhjausparametritAsyncResource ohjausparametritAsyncResource;
    protected final OrganisaatioAsyncResource organisaatioAsyncResource;
    protected final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    protected final ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource;

    protected AbstractPistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                               SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                               TarjontaAsyncResource tarjontaAsyncResource,
                                               OhjausparametritAsyncResource ohjausparametritAsyncResource,
                                               OrganisaatioAsyncResource organisaatioAsyncResource,
                                               ValintaperusteetAsyncResource valintaperusteetAsyncResource,
                                               ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.valintalaskentaValintakoeAsyncResource = valintalaskentaValintakoeAsyncResource;
    }

    private PistesyottoExcel muodostoPistesyottoExcel(String hakuOid,
                                                      String hakukohdeOid,
                                                      List<ValintakoeOsallistuminenDTO> osallistumistiedot,
                                                      List<Hakemus> hakemukset,
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
                .map(v -> v.getTunniste())
                .collect(Collectors.toList());

        Set<String> kaikkiKutsutaanTunnisteet = hakukohdeJaValintakoe.stream()
                .flatMap(h -> h.getValintakoeDTO().stream())
                .filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki()))
                .map(v -> v.getTunniste())
                .collect(Collectors.toSet());

        return new PistesyottoExcel(hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi, hakukohdeNimi, tarjoajaNimi,
                hakemukset, kaikkiKutsutaanTunnisteet, valintakoeTunnisteet, osallistumistiedot, valintaperusteet,
                pistetiedot, kuuntelijat);
    }

    protected Observable<Pair<PistesyottoExcel, Map<String, ApplicationAdditionalDataDTO>>> muodostaPistesyottoExcel(
            String hakuOid,
            String hakukohdeOid,
            DokumenttiProsessi prosessi,
            Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
        Func2<List<ValintakoeOsallistuminenDTO>, List<ApplicationAdditionalDataDTO>, Observable<List<ApplicationAdditionalDataDTO>>> heaPuuttuvatLisatiedot = (osallistumiset, lisatiedot) -> {
            Set<String> puuttuvatLisatiedot = osallistumiset.stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet());
            puuttuvatLisatiedot.removeAll(lisatiedot.stream().map(l -> l.getOid()).collect(Collectors.toSet()));
            if (puuttuvatLisatiedot.isEmpty()) {
                return Observable.just(lisatiedot);
            }
            prosessi.inkrementoiKokonaistyota();
            return applicationAsyncResource.getApplicationAdditionalData(puuttuvatLisatiedot)
                    .map(ls -> Stream.concat(lisatiedot.stream(), ls.stream()).collect(Collectors.toList()))
                    .doOnCompleted(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    });
        };
        Func2<List<ValintakoeOsallistuminenDTO>, List<Hakemus>, Observable<List<Hakemus>>> haePuuttuvatHakemukset = (osallistumiset, hakemukset) -> {
            Set<String> puuttuvatHakemukset = osallistumiset.stream().map(o -> o.getHakemusOid()).collect(Collectors.toSet());
            puuttuvatHakemukset.removeAll(hakemukset.stream().map(h -> h.getOid()).collect(Collectors.toSet()));
            if (puuttuvatHakemukset.isEmpty()) {
                return Observable.just(hakemukset);
            }
            prosessi.inkrementoiKokonaistyota();
            return applicationAsyncResource.getApplicationsByHakemusOids(new ArrayList<>(puuttuvatHakemukset))
                    .map(hs -> Stream.concat(hakemukset.stream(), hs.stream()).collect(Collectors.toList()))
                    .doOnCompleted(() -> {
                        prosessi.inkrementoiTehtyjaToita();
                    });
        };
        Func1<List<ValintaperusteDTO>, Observable<List<Oppija>>> haeKielikoetulokset = kokeet -> {
            if (kokeet.stream().map(k -> k.getTunniste()).anyMatch(t -> t.matches(PistesyottoExcel.KIELIKOE_REGEX))) {
                prosessi.inkrementoiKokonaistyota();
                return suoritusrekisteriAsyncResource.getOppijatByHakukohdeWithoutEnsikertalaisuus(hakukohdeOid, hakuOid)
                        .doOnCompleted(() -> {
                            prosessi.inkrementoiTehtyjaToita();
                        });
            } else {
                return Observable.just(new ArrayList<>());
            }
        };
        Observable<List<ValintaperusteDTO>> kokeetO = valintaperusteetAsyncResource.findAvaimet(hakukohdeOid);
        Observable<List<ValintakoeOsallistuminenDTO>> osallistumistiedotO = valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid);
        Observable<List<ApplicationAdditionalDataDTO>> lisatiedotO = Observable.zip(
                Observable.merge(Observable.zip(
                        osallistumistiedotO,
                        applicationAsyncResource.getApplicationAdditionalData(hakuOid, hakukohdeOid),
                        heaPuuttuvatLisatiedot
                )),
                kokeetO,
                osallistumistiedotO,
                kokeetO.flatMap(haeKielikoetulokset),
                ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid),
                (lisatiedot, kokeet, osallistumistiedot, kielikoetulokset, ohjausparametrit) -> {
                    Map<String, ValintakoeOsallistuminenDTO> osallistumistiedotByHakemusOid = osallistumistiedot.stream()
                            .collect(Collectors.toMap(o -> o.getHakemusOid(), o -> o));
                    Map<String, Oppija> kielikoetuloksetByPersonOid = kielikoetulokset.stream()
                            .collect(Collectors.toMap(o -> o.getOppijanumero(), o -> o));
                    return lisatiedot.stream().map(l ->
                            new PistetietoDTO(
                                    l,
                                    Pair.of(hakukohdeOid, kokeet),
                                    osallistumistiedotByHakemusOid.get(l.getOid()),
                                    kielikoetuloksetByPersonOid.get(l.getPersonOid()),
                                    ohjausparametrit
                            ).applicationAdditionalDataDTO
                    ).collect(Collectors.toList());
                }
        );
        Observable<List<Hakemus>> hakemuksetO = Observable.merge(Observable.zip(
                osallistumistiedotO,
                applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid),
                haePuuttuvatHakemukset
        ));

        prosessi.inkrementoiKokonaistyota();
        return Observable.zip(
                osallistumistiedotO,
                lisatiedotO,
                hakemuksetO,
                kokeetO,
                valintaperusteetAsyncResource.haeValintakokeetHakutoiveille(Collections.singletonList(hakukohdeOid)),
                tarjontaAsyncResource.haeHakukohde(hakukohdeOid),
                tarjontaAsyncResource.haeHaku(hakuOid),
                (osallistumistiedot, lisatiedot, hakemukset, kokeet, valintakoeosallistumiset, hakukohde, haku) -> {
                    return Pair.of(
                            muodostoPistesyottoExcel(hakuOid, hakukohdeOid, osallistumistiedot, hakemukset, kokeet,
                                    lisatiedot, valintakoeosallistumiset, haku, hakukohde, kuuntelijat),
                            lisatiedot.stream().collect(Collectors.toMap(l -> l.getOid(), l -> l))
                    );
                }
        ).doOnCompleted(() -> {
            prosessi.inkrementoiTehtyjaToita();
        });
    }

    protected Observable<Void> tallennaKoostetutPistetiedot(String hakuOid,
                                                            String hakukohdeOid,
                                                            List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                            Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                            String username,
                                                            ValintaperusteetOperation auditLogOperation) {
        return tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, username, auditLogOperation, true);
    }


    protected Observable<Void> tallennaKoostetutPistetiedot(String hakuOid,
                                                            String hakukohdeOid,
                                                            List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                            Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                            String username,
                                                            ValintaperusteetOperation auditLogOperation,
                                                            boolean saveApplicationAdditionalInfo) {
        Observable<Void> kielikoeTallennus = Observable.zip(
                findSourceOid(hakukohdeOid),
                haeOppijatSuresta(hakuOid, hakukohdeOid),
                Pair::of)
                .flatMap(sourceAndOppijat -> {
                    String sourceOid = sourceAndOppijat.getLeft();
                    List<Oppija> oppijatSuresta = sourceAndOppijat.getRight();
                    return tallennaKielikoetulokset(hakuOid, hakukohdeOid, sourceOid, pistetiedotHakemukselle,
                            kielikoetuloksetSureen, username, auditLogOperation, oppijatSuresta);
                });

        return kielikoeTallennus.flatMap(a -> {
            if (saveApplicationAdditionalInfo) {
                return tallennaAdditionalInfoHakemuksille(hakuOid, hakukohdeOid, pistetiedotHakemukselle, username, auditLogOperation);
            } else {
                return Observable.just(null);
            }
        }).onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                "Virhe tallennettaessa koostettuja pistetietoja haun %s hakukohteelle %s", hakuOid, hakukohdeOid), t)));
    }

    private Observable<Void> tallennaKielikoetulokset(String hakuOid, String hakukohdeOid, String sourceOid,
                                                         List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                         Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                         String username, ValintaperusteetOperation auditLogOperation,
                                                         List<Oppija> oppijatSuresta) {
        Function<String,String> findPersonOidByHakemusOid = hakemusOid -> pistetiedotHakemukselle.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
        AmmatillisenKielikoetulosOperations operations = new AmmatillisenKielikoetulosOperations(sourceOid, oppijatSuresta, kielikoetuloksetSureen, findPersonOidByHakemusOid);

        Map<String, Optional<CompositeCommand>> resultsToSendToSure = operations.getResultsToSendToSure();
        if (resultsToSendToSure.isEmpty()) {
            LOG.info(String.format("Näyttää siltä, että kaikki %d ammatillisen kielikokeen tulostietoa Suoritusrekisterissä " +
                "ovat jo ajan tasalla, ei päivitetä.", kielikoetuloksetSureen.size()));
            return Observable.just(null);
        }

        return Observable.from(resultsToSendToSure.keySet()).flatMap(hakemusOid -> {
            String personOid = findPersonOidByHakemusOid.apply(hakemusOid);
            Optional<CompositeCommand> operationOptional = resultsToSendToSure.get(hakemusOid);
            if (!operationOptional.isPresent()) {
                return Observable.just(null);
            }

            CompositeCommand compositeCommandForHakemus = operationOptional.get();
            Observable<Arvosana> sureOperations = compositeCommandForHakemus.createSureOperation(suoritusrekisteriAsyncResource)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                    "Virhe hakemuksen %s tulosten tallentamisessa Suoritusrekisteriin ", hakemusOid), t)));

            sureOperations.doOnNext(processedArvosana ->
                AUDIT.log(builder()
                    .id(username)
                    .hakuOid(hakuOid)
                    .hakukohdeOid(hakukohdeOid)
                    .hakijaOid(personOid)
                    .hakemusOid(hakemusOid)
                    .addAll(ImmutableMap.of(KIELIKOE_KEY_PREFIX + processedArvosana.getLisatieto().toLowerCase(), processedArvosana.getArvio().getArvosana()))
                    .setOperaatio(auditLogOperation)
                    .build()));

            return sureOperations;
        }).lastOrDefault(null).<Void>map(x -> null).doOnCompleted(() ->
            LOG.info("Kielikoetietojen tallennus Suoritusrekisteriin onnistui"));
    }

    private Observable<Void> tallennaAdditionalInfoHakemuksille(String hakuOid,
                                                                String hakukohdeOid,
                                                                List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                                String username,
                                                                ValintaperusteetOperation auditLogOperation) {
        return applicationAsyncResource.putApplicationAdditionalData(hakuOid, hakukohdeOid, pistetiedotHakemukselle)
                .<Void>map(a -> null)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(
                        "Lisätietojen tallennus hakemukselle epäonnistui", t)))
                .doOnCompleted(() ->
                        pistetiedotHakemukselle.forEach(p -> AUDIT.log(builder()
                                .id(username)
                                .hakuOid(hakuOid)
                                .hakukohdeOid(hakukohdeOid)
                                .hakijaOid(p.getPersonOid())
                                .hakemusOid(p.getOid())
                                .addAll(p.getAdditionalData())
                                .setOperaatio(auditLogOperation)
                                .build())));
    }

    private Observable<List<Oppija>> haeOppijatSuresta(String hakuOid, String hakukohdeOid) {
        return suoritusrekisteriAsyncResource.getOppijatByHakukohdeWithoutEnsikertalaisuus(hakukohdeOid, hakuOid)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
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
        if (oppilaitos.isPresent()) {
            oppilaitosRef.set(oppilaitos.get().getOid());
        }

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

    public static Map<String, List<Arvosana>> ammatillisenKielikoeArvosanat(List<Oppija> oppijat) {
        return oppijat.stream().collect(
                Collectors.toMap(Oppija::getOppijanumero,
                        o -> o.getSuoritukset().stream()
                                .filter(SuoritusJaArvosanatWrapper::isAmmatillisenKielikoe).map(SuoritusJaArvosanat::getArvosanat).flatMap(List::stream)
                                .filter(a -> KIELIKOE_ARVOSANA_AINE.equalsIgnoreCase(a.getAine())).collect(Collectors.toList()))
        );
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
