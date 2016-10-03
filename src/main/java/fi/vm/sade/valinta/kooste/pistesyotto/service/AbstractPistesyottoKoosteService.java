package fi.vm.sade.valinta.kooste.pistesyotto.service;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import rx.Observable;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static org.jasig.cas.client.util.CommonUtils.isNotEmpty;

public abstract class AbstractPistesyottoKoosteService {

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
                                                Map<String, Map<String, String>> kielikoetuloksetSureen,
                                                Consumer<String> onSuccess,
                                                BiConsumer<String, Throwable> onError,
                                                String username,
                                                ValintaperusteetOperation auditLogOperation) {

        AtomicInteger laskuri = new AtomicInteger(kielikoetuloksetSureen.values().stream().mapToInt(map -> map.size()).sum());
        AtomicReference<String> myontajaRef = new AtomicReference<>();

        Supplier<Void> tallennaAdditionalInfo = () -> {
            applicationAsyncResource.putApplicationAdditionalData(
                    hakuOid, hakukohdeOid, pistetiedotHakemukselle).subscribe(response -> {
                pistetiedotHakemukselle.forEach(p ->
                        AUDIT.log(builder()
                                .id(username)
                                .hakuOid(hakuOid)
                                .hakukohdeOid(hakukohdeOid)
                                .hakijaOid(p.getPersonOid())
                                .hakemusOid(p.getOid())
                                .addAll(p.getAdditionalData())
                                .setOperaatio(auditLogOperation)
                                .build())
                );
                onSuccess.accept("ok");
            }, error -> onError.accept("Lisätietojen tallennus hakemukselle epäonnistui", error));
            return null;
        };

        Supplier<Void> tallennaKielikoetulokset = () -> {
            kielikoetuloksetSureen.keySet().stream().forEach(hakemusOid ->
            {
                String valmistuminen = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(new Date());
                String personOid = pistetiedotHakemukselle.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
                Map<String, String> kielikoetulokset = kielikoetuloksetSureen.get(hakemusOid);
                kielikoetulokset.keySet().stream().filter(t -> isNotEmpty(kielikoetulokset.get(t))).forEach(tunnus -> {
                    String kieli = tunnus.substring(9);

                    Suoritus suoritus = new Suoritus();
                    suoritus.setTila(KIELIKOE_SUORITUS_TILA);
                    suoritus.setYksilollistaminen(KIELIKOE_SUORITUS_YKSILOLLISTAMINEN);
                    suoritus.setHenkiloOid(personOid);
                    suoritus.setVahvistettu(true);
                    suoritus.setSuoritusKieli(kieli.toUpperCase());
                    suoritus.setMyontaja(myontajaRef.get());
                    suoritus.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
                    suoritus.setValmistuminen(valmistuminen);

                    suoritusrekisteriAsyncResource.postSuoritus(suoritus).subscribe( tallennettuSuoritus -> {
                        String arvioArvosana = kielikoetulokset.get(tunnus).toLowerCase();

                        Arvosana arvosana = new Arvosana();
                        arvosana.setAine(KIELIKOE_ARVOSANA_AINE);
                        arvosana.setLisatieto(kieli.toUpperCase());
                        arvosana.setArvio(new Arvio(arvioArvosana, AmmatillisenKielikoetuloksetSurestaConverter.SURE_ASTEIKKO_HYVAKSYTTY, null));
                        arvosana.setSuoritus(tallennettuSuoritus.getId());
                        arvosana.setMyonnetty(valmistuminen);

                        suoritusrekisteriAsyncResource.postArvosana(arvosana).subscribe(arvosanaResponse -> {
                            AUDIT.log(builder()
                                    .id(username)
                                    .hakuOid(hakuOid)
                                    .hakukohdeOid(hakukohdeOid)
                                    .hakijaOid(personOid)
                                    .hakemusOid(hakemusOid)
                                    .addAll(ImmutableMap.of(KIELIKOE_KEY_PREFIX + kieli.toLowerCase(), arvioArvosana))
                                    .setOperaatio(auditLogOperation)
                                    .build());
                            if(0 == laskuri.decrementAndGet()) {
                                tallennaAdditionalInfo.get();
                            }
                        }, error -> onError.accept("Arvosanan tallennus Suoritusrekisteriin epäonnistui", error));
                    }, error -> onError.accept("Arvosanan tallennus Suoritusrekisteriin epäonnistui", error));
                });
            });
            return null;
        };

        if(0 == laskuri.get()) {
            tallennaAdditionalInfo.get();
        } else {
            tarjontaAsyncResource.haeHakukohde(hakukohdeOid).subscribe(hakukohde -> {
                String tarjoajaOid = hakukohde.getTarjoajaOids().stream().findFirst().orElse("");
                organisaatioAsyncResource.haeOrganisaationTyyppiHierarkia(tarjoajaOid).subscribe(
                   hierarkia -> etsiOppilaitosHierarkiasta(tarjoajaOid, hierarkia.getOrganisaatiot(), myontajaRef),
                   error -> onError.accept("Myöntäjän etsiminen kielikoesuoritukseen epäonnistui", error));
                tallennaKielikoetulokset.get();
            }, error -> onError.accept("Hakukohteen haku Tarjonnasta epäonnistui", error));
        }
    }

    public AtomicReference<String> etsiOppilaitosHierarkiasta(String tarjoajaOid, List<OrganisaatioTyyppi> taso, AtomicReference<String> oppilaitosRef) {
        Optional<OrganisaatioTyyppi> oppilaitos = taso.stream().filter(ot -> ot.getOrganisaatiotyypit().contains("OPPILAITOS")).findFirst();
        if(oppilaitos.isPresent()) {
            oppilaitosRef.set(oppilaitos.get().getOid());
        }
        Optional<OrganisaatioTyyppi> tarjoaja = taso.stream().filter(ot -> ot.getOid().equals(tarjoajaOid)).findFirst();
        if(tarjoaja.isPresent()) {
            return oppilaitosRef;
        } else {
            List<OrganisaatioTyyppi> seuraavaTaso = taso.stream().map(OrganisaatioTyyppi::getChildren).flatMap(Collection::stream).collect(Collectors.toList());
            if(seuraavaTaso.size() == 0) {
                return oppilaitosRef;
            }
            return etsiOppilaitosHierarkiasta(tarjoajaOid, seuraavaTaso, oppilaitosRef);
        }
    }

    public static Map<String, List<Arvosana>> ammatillisenKielikoeArvosanat(List<Oppija> oppijat) {
        return oppijat.stream().collect(
                Collectors.toMap(Oppija::getOppijanumero,
                        o -> o.getSuoritukset().stream()
                                .filter(SuoritusJaArvosanatWrapper::isAmmatillisenKielikoe).map(SuoritusJaArvosanat::getArvosanat).flatMap(List::stream)
                                .filter(a -> KIELIKOE_ARVOSANA_AINE.equalsIgnoreCase(a.getAine())).collect(Collectors.toList()))
        );
    }
}
