package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.jasig.cas.client.util.CommonUtils.isNotEmpty;
import com.google.common.collect.ImmutableMap;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    protected final OrganisaatioAsyncResource organisaatioAsyncResource;

    protected AbstractPistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                               SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                               TarjontaAsyncResource tarjontaAsyncResource,
                                               OrganisaatioAsyncResource organisaatioAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
    }

    protected void tallennaKoostetutPistetiedot(String hakuOid,
                                                String hakukohdeOid,
                                                List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                Consumer<String> onSuccess,
                                                BiConsumer<String, Throwable> onError,
                                                String username,
                                                ValintaperusteetOperation auditLogOperation) {
        tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, onSuccess, onError, username, auditLogOperation, true);
    }


    protected void tallennaKoostetutPistetiedot(String hakuOid,
                                                String hakukohdeOid,
                                                List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                Consumer<String> onSuccess,
                                                BiConsumer<String, Throwable> onError,
                                                String username,
                                                ValintaperusteetOperation auditLogOperation, boolean saveApplicationAdditionalInfo) {

        Observable<Boolean> kielikoeTallennus = Observable.zip(
            findMyontajaOid(hakukohdeOid),
            haeOppijatSuresta(hakuOid, hakukohdeOid),
            Pair::of)
            .flatMap(myontajaAndOppijat -> {
                String myontajaOid = myontajaAndOppijat.getLeft();
                List<Oppija> oppijatSuresta = myontajaAndOppijat.getRight();
                return tallennaKielikoetulokset(hakuOid, hakukohdeOid, myontajaOid, pistetiedotHakemukselle, kielikoetuloksetSureen,
                    onError, username, auditLogOperation, oppijatSuresta);
            });

        Observable<Boolean> ennenAdditionalInfonTallennusta = kielikoetuloksetSureen.isEmpty() ? Observable.just(false) : kielikoeTallennus;
        Observable<String> additionalInfonTallennus = saveApplicationAdditionalInfo ?
            tallennaAdditionalInfoHakemuksille(hakuOid, hakukohdeOid, pistetiedotHakemukselle, username, auditLogOperation, onError) :
            Observable.just("ok");

        ennenAdditionalInfonTallennusta.distinct().flatMap(olikoKielikoeTulostenTallennuksia -> {
            if (olikoKielikoeTulostenTallennuksia) {
                LOG.info("Kielikoetietojen tallennus Suoritusrekisteriin onnistui");
            } else {
                LOG.info("Ei Suoritusrekisteriin tallennettavia kielikoetietoja.");
            }
            return additionalInfonTallennus;
        }).doOnError(e -> onError.accept(String.format("Virhe tallennettaessa koostettuja pistetietoja haun %s hakukohteelle %s", hakuOid, hakukohdeOid), e))
            .subscribe(onSuccess::accept);
    }

    private Observable<Boolean> tallennaKielikoetulokset(String hakuOid, String hakukohdeOid, String myontajaOid,
                                                         List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                         Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen,
                                                         BiConsumer<String, Throwable> onError, String username,
                                                         ValintaperusteetOperation auditLogOperation, List<Oppija> oppijatSuresta) {
        SimpleDateFormat valmistuminenFormat = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT);

        Function<String,String> findPersonOidByHakemusOid = hakemusOid -> pistetiedotHakemukselle.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
        AmmatillisenKielikoetulosUpdates updates = new AmmatillisenKielikoetulosUpdates(myontajaOid, oppijatSuresta, kielikoetuloksetSureen, findPersonOidByHakemusOid);
        Map<String, List<SingleKielikoeTulos>> sureenLahetettavatPaivitykset = updates.getResultsToSendToSure();

        if (sureenLahetettavatPaivitykset.isEmpty()) {
            LOG.info(String.format("Näyttää siltä, että kaikki %d ammatillisen kielikokeen tulostietoa Suoritusrekisterissä " +
                "ovat jo ajan tasalla, ei päivitetä.", kielikoetuloksetSureen.size()));
            return Observable.just(false);
        }

        return Observable.from(sureenLahetettavatPaivitykset.keySet()).flatMap(hakemusOid -> {
            String personOid = findPersonOidByHakemusOid.apply(hakemusOid);
            List<SingleKielikoeTulos> hakemuksenKielikoeTulokset = sureenLahetettavatPaivitykset.get(hakemusOid);

            List<SingleKielikoeTulos> lisattavatKielikoetulokset = hakemuksenKielikoeTulokset.stream().filter(t -> isNotEmpty(t.arvioArvosana)).collect(Collectors.toList());
            List<SingleKielikoeTulos> poistettavatKielikoetulokset = hakemuksenKielikoeTulokset.stream().filter(t -> isEmpty(t.arvioArvosana)).collect(Collectors.toList());

            Observable<Pair<SingleKielikoeTulos, Suoritus>> suoritustenTallennukset = Observable.from(lisattavatKielikoetulokset).flatMap(singleKielikoeTulos -> {
                String kieli = singleKielikoeTulos.kieli();
                String valmistuminen = valmistuminenFormat.format(singleKielikoeTulos.valmistuminen);

                Suoritus suoritus = new Suoritus();
                suoritus.setTila(KIELIKOE_SUORITUS_TILA);
                suoritus.setYksilollistaminen(KIELIKOE_SUORITUS_YKSILOLLISTAMINEN);
                suoritus.setHenkiloOid(personOid);
                suoritus.setVahvistettu(true);
                suoritus.setSuoritusKieli(kieli.toUpperCase());
                suoritus.setMyontaja(myontajaOid);
                suoritus.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
                suoritus.setValmistuminen(valmistuminen);

                return Observable.zip(Observable.just(singleKielikoeTulos), suoritusrekisteriAsyncResource.postSuoritus(suoritus)
                    .doOnError(e -> onError.accept(String.format("Suorituksen %s tallentaminen suoritusrekisteriin epäonnistui", suoritus), e)
                ), Pair::of);
            });

            Func1<Pair<SingleKielikoeTulos, Suoritus>, Observable<Boolean>> tallennaArvosana = kielikoetulosJaTallennettuSuoritus -> {
                SingleKielikoeTulos singleKielikoeTulos = kielikoetulosJaTallennettuSuoritus.getLeft();
                Suoritus tallennettuSuoritus = kielikoetulosJaTallennettuSuoritus.getRight();
                String kieli = singleKielikoeTulos.kieli();
                String arvioArvosana = singleKielikoeTulos.arvioArvosana.toLowerCase();
                String valmistuminen = valmistuminenFormat.format(singleKielikoeTulos.valmistuminen);

                Arvosana arvosana = new Arvosana();
                arvosana.setAine(KIELIKOE_ARVOSANA_AINE);
                arvosana.setLisatieto(kieli.toUpperCase());
                arvosana.setArvio(new Arvio(arvioArvosana, AmmatillisenKielikoetuloksetSurestaConverter.SURE_ASTEIKKO_HYVAKSYTTY, null));
                arvosana.setSuoritus(tallennettuSuoritus.getId());
                arvosana.setMyonnetty(valmistuminen);

                Func1<Arvosana, Boolean> kirjoitaAuditLogiin = arvosanaResponse -> {
                    AUDIT.log(builder()
                        .id(username)
                        .hakuOid(hakuOid)
                        .hakukohdeOid(hakukohdeOid)
                        .hakijaOid(personOid)
                        .hakemusOid(hakemusOid)
                        .addAll(ImmutableMap.of(KIELIKOE_KEY_PREFIX + kieli.toLowerCase(), arvioArvosana))
                        .setOperaatio(auditLogOperation)
                        .build());
                    return true;
                };
                return suoritusrekisteriAsyncResource.postArvosana(arvosana)
                    .doOnError(e -> onError.accept(String.format("Arvosanan %s tallentaminen Suoritusrekisteriin epäonnistui", arvosana), e)
                ).map(kirjoitaAuditLogiin);
            };

            Observable<List<Suoritus>> poistettavatSuoritukset = Observable.from(poistettavatKielikoetulokset).map(singleKielikoeTulos -> {
                String kieli = singleKielikoeTulos.kieli();
                Function<SuoritusJaArvosanat, Boolean> isKielikoeArvosana = (suoritusJaArvosana) -> {
                    Suoritus suoritus = suoritusJaArvosana.getSuoritus();
                    return SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE.equals(suoritus.getKomo()) &&
                        myontajaOid.equals(suoritus.getMyontaja()) &&
                        suoritusJaArvosana.getArvosanat().stream().map(Arvosana::getLisatieto).anyMatch(kieli::equalsIgnoreCase);
                };
                return oppijatSuresta.stream().filter(o -> o.getOppijanumero().equals(personOid))
                    .map(Oppija::getSuoritukset).flatMap(Collection::stream).filter(isKielikoeArvosana::apply)
                    .map(SuoritusJaArvosanat::getSuoritus).collect(Collectors.toList());
            });
            Observable<Suoritus> suoritustenPoistot = poistettavatSuoritukset.flatMap(p ->
                Observable.from(p)
                    .flatMap(suoritus -> suoritusrekisteriAsyncResource.deleteSuoritus(suoritus.getId())
                    .doOnError(e -> onError.accept(String.format("Suorituksen %s poistaminen Suoritusrekisteristä epäonnistui", suoritus), e))));

            return Observable.merge(suoritustenTallennukset.flatMap(tallennaArvosana), suoritustenPoistot).materialize().map(x -> true);
        });
    }

    private Observable<String> tallennaAdditionalInfoHakemuksille(String hakuOid, String hakukohdeOid, List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                                                  String username, ValintaperusteetOperation auditLogOperation, BiConsumer<String, Throwable> onError) {
        return applicationAsyncResource.putApplicationAdditionalData(hakuOid, hakukohdeOid, pistetiedotHakemukselle)
            .doOnError(e -> onError.accept("Lisätietojen tallennus hakemukselle epäonnistui", e))
            .materialize()
            .doOnCompleted(() ->
                pistetiedotHakemukselle.forEach(p -> AUDIT.log(builder()
                    .id(username)
                    .hakuOid(hakuOid)
                    .hakukohdeOid(hakukohdeOid)
                    .hakijaOid(p.getPersonOid())
                    .hakemusOid(p.getOid())
                    .addAll(p.getAdditionalData())
                    .setOperaatio(auditLogOperation)
                    .build())
                )).map(x -> "ok");
    }

    private Observable<List<Oppija>> haeOppijatSuresta(String hakuOid, String hakukohdeOid) {
        return suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid)
                .onErrorResumeNext(t -> Observable.error(new IllegalStateException(String.format(
                        "Oppijoiden haku Suoritusrekisteristä haun %s hakukohteelle %s epäonnistui",
                        hakuOid,
                        hakukohdeOid
                ), t)));
    }

    private Observable<String> findMyontajaOid(String hakukohdeOid) {
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
                    AtomicReference<String> myontajaRef = new AtomicReference<>();
                    etsiOppilaitosHierarkiasta(tarjoajaOid.get(), hierarkia.getOrganisaatiot(), myontajaRef);
                    if (isEmpty(myontajaRef.get())) {
                        return Observable.error(new IllegalStateException(String.format(
                                "Hakukohteen %s suoritukselle ei löytynyt myöntäjää, tarjoaja on %s ja sillä %s organisaatiota.",
                                hakukohdeOid,
                                tarjoajaOid,
                                hierarkia.getOrganisaatiot().size()
                        )));
                    }
                    return Observable.just(myontajaRef.get());
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

    public static class SingleKielikoeTulos {
        public final String kokeenTunnus; // kielikoe_fi , kielikoe_sv
        public final String arvioArvosana; // "true", "false", ""
        public final Date valmistuminen;

        public SingleKielikoeTulos(String kokeenTunnus, String arvioArvosana, Date valmistuminen) {
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
