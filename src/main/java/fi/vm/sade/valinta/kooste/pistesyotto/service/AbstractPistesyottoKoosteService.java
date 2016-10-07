package fi.vm.sade.valinta.kooste.pistesyotto.service;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.stream.Collectors;

import static fi.vm.sade.auditlog.valintaperusteet.LogMessage.builder;
import static fi.vm.sade.valinta.kooste.KoosteAudit.AUDIT;
import static org.apache.commons.lang.StringUtils.isEmpty;
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
                                                ValintaperusteetOperation auditLogOperation, boolean saveApplicationAdditionalInfo) {

        AtomicInteger laskuri = new AtomicInteger(kielikoetuloksetSureen.values().stream().mapToInt(map -> map.size()).sum());
        AtomicReference<String> myontajaRef = new AtomicReference<>();
        AtomicReference<List<Oppija>> oppijatRef = new AtomicReference<>();

        Supplier<Void> tallennaAdditionalInfo = () -> {
            if (saveApplicationAdditionalInfo) {
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
            } else {
                onSuccess.accept("ok");
            }
            return null;
        };



        Supplier<Void> tallennaKielikoetulokset = () -> {
            kielikoetuloksetSureen.keySet().stream().forEach(hakemusOid ->
            {
                String valmistuminen = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(new Date());
                String personOid = pistetiedotHakemukselle.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
                Map<String, String> kielikoetulokset = kielikoetuloksetSureen.get(hakemusOid);

                List<String> lisattavatKielikoetulokset = kielikoetulokset.keySet().stream().filter(t -> isNotEmpty(kielikoetulokset.get(t))).collect(Collectors.toList());
                List<String> poistettavatKielikoetulokset = kielikoetulokset.keySet().stream().filter(t -> isEmpty(kielikoetulokset.get(t))).collect(Collectors.toList());

                lisattavatKielikoetulokset.forEach(tunnus -> {
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

                poistettavatKielikoetulokset.forEach(tunnus -> {
                    String kieli = tunnus.substring(9);
                    Function<SuoritusJaArvosanat, Boolean> isKielikoeArvosana = (suoritusJaArvosana) -> {
                        Suoritus suoritus = suoritusJaArvosana.getSuoritus();
                        return SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE.equals(suoritus.getKomo()) &&
                               myontajaRef.get().equals(suoritus.getMyontaja()) &&
                               suoritusJaArvosana.getArvosanat().stream().map(Arvosana::getLisatieto).anyMatch(k -> kieli.equalsIgnoreCase(k));
                    };
                    List<Suoritus> poistettavatSuoritukset = oppijatRef.get().stream().filter(o -> o.getOppijanumero().equals(personOid))
                      .map(o -> o.getSuoritukset()).flatMap(Collection::stream).filter(sa -> isKielikoeArvosana.apply(sa))
                      .map(SuoritusJaArvosanat::getSuoritus).collect(Collectors.toList());
                    AtomicInteger suoritusLaskuri = new AtomicInteger(poistettavatSuoritukset.size());
                    poistettavatSuoritukset.forEach(suoritus -> {
                        suoritusrekisteriAsyncResource.deleteSuoritus(suoritus.getId()).subscribe(poistettu -> {
                            if(0 == suoritusLaskuri.decrementAndGet()) {
                                if(0 == laskuri.decrementAndGet()) {
                                    tallennaAdditionalInfo.get();
                                }
                            }
                        }, error -> onError.accept("Suorituksen poistaminen epäonnistui", error));

                    });
                    if(0 == poistettavatSuoritukset.size()) {
                        if(0 == laskuri.decrementAndGet()) {
                            tallennaAdditionalInfo.get();
                        }
                    }
                });

            });
            return null;
        };

        Supplier<Void> haeOppijatPoistettaville = () -> {
            if(kielikoetuloksetSureen.values().stream().anyMatch(map -> map.values().stream().anyMatch(String::isEmpty))) {
                suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid).subscribe(oppijat -> {
                    oppijatRef.set(oppijat);
                    tallennaKielikoetulokset.get();
                }, error -> onError.accept("Oppijoiden hakeminen Suoritusrekisteristä epäonnistui", error));
            } else {
                tallennaKielikoetulokset.get();
            }
            return null;
        };

        if(0 == laskuri.get()) {
            tallennaAdditionalInfo.get();
        } else {
            tarjontaAsyncResource.haeHakukohde(hakukohdeOid).subscribe(hakukohde -> {
                String tarjoajaOid = hakukohde.getTarjoajaOids().stream().findFirst().orElse("");
                    organisaatioAsyncResource.haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(tarjoajaOid).subscribe(hierarkia ->
                    {
                        etsiOppilaitosHierarkiasta(tarjoajaOid, hierarkia.getOrganisaatiot(), myontajaRef);
                        if (isEmpty(myontajaRef.get())) {
                            String msg = String.format("Hakukohteen %s suoritukselle ei löytynyt myöntäjää, tarjoaja on %s ja sillä %s organisaatiota.",
                                hakukohdeOid, tarjoajaOid, hierarkia.getOrganisaatiot().size());
                            onError.accept(msg, new Exception(msg));
                        }
                        haeOppijatPoistettaville.get();
                    }, error -> onError.accept("Myöntäjän etsiminen kielikoesuoritukseen epäonnistui", error));
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
