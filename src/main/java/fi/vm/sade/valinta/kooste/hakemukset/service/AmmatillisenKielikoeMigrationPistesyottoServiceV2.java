package fi.vm.sade.valinta.kooste.hakemukset.service;

import fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1ResourceWrapper;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AmmatillisenKielikoeMigrationPistesyottoServiceV2 {
    private ApplicationAsyncResource applicationAsyncResource;
    private SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private TarjontaAsyncResource tarjontaAsyncResource;
    private final HakukohdeV1ResourceWrapper hakukohdeResource;
    private OrganisaatioAsyncResource organisaatioAsyncResource;

    @Autowired
    public AmmatillisenKielikoeMigrationPistesyottoServiceV2(ApplicationAsyncResource applicationAsyncResource,
                                                             SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                                             TarjontaAsyncResource tarjontaAsyncResource,
                                                             HakukohdeV1ResourceWrapper hakukohdeResource,
                                                             OrganisaatioAsyncResource organisaatioAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
        this.hakukohdeResource = hakukohdeResource;
        this.organisaatioAsyncResource = organisaatioAsyncResource;
    }

    public SingleSaveResult save(ValintakoeOsallistuminenDTO valintakoeOsallistuminen) {
        List<HakutoiveDTO> hakutoiveetWithKielikoeResults = valintakoeOsallistuminen.getHakutoiveet().stream().filter(containsKielikoeResult()).collect(Collectors.toList());
        if (hakutoiveetWithKielikoeResults.size() != 1) {
            throw new IllegalStateException(String.format("Odotettiin täsämälleen yhtä toivetta, jossa on kielikokeeseen osallistuminen, " +
                "mutta löytyi %s kpl: %s", hakutoiveetWithKielikoeResults.size(), hakutoiveetWithKielikoeResults));
        }

        HakutoiveDTO toive = hakutoiveetWithKielikoeResults.get(0);
        String hakukohdeOid = toive.getHakukohdeOid();
        ResultV1RDTO<HakukohdeValintaperusteetV1RDTO> hakukohdeSearchResult = hakukohdeResource.findValintaperusteetByOid(hakukohdeOid);
        if (hakukohdeSearchResult.hasErrors()) {
            throw new RuntimeException(String.format("Ongelma haettaessa hakukohteen %s tietoja tarjonnasta: %s", hakukohdeOid, hakukohdeSearchResult.getErrors()));
        }

        HakukohdeValintaperusteetV1RDTO hakukohde = hakukohdeSearchResult.getResult();
        String tarjoajaOid = hakukohde.getTarjoajaOid();

        AtomicReference<String> myontajaRef = new AtomicReference<>(null);

        /*organisaatioAsyncResource.haeOrganisaationTyyppiHierarkia(tarjoajaOid).subscribe(organisaatioTyyppiHierarkia -> {
            etsiOppilaitosHierarkiasta(tarjoajaOid, organisaatioTyyppiHierarkia.getOrganisaatiot(), myontajaRef);
            String oppilaitosOid = myontajaRef.get();
            if (oppilaitosOid == null || oppilaitosOid.equals("")) {
                throw new IllegalArgumentException(String.format("Ei löytynyt myöntäjäorganisaatiota tarjoajalle %s", tarjoajaOid));
            }

        });
        */
/*
        AtomicInteger laskuri = new AtomicInteger(kielikoetuloksetSureen.values().stream().mapToInt(map -> map.size()).sum());
        AtomicReference<String> myontajaRef = new AtomicReference<>();

        Supplier<Void> tallennaAdditionalInfo = () -> {
            applicationAsyncResource.putApplicationAdditionalData(
                hakuOid, hakukohdeOid, pistetiedot).subscribe(response -> {
                pistetiedot.forEach(p ->
                        AUDIT.log(builder()
                                .id(username)
                                .hakuOid(hakuOid)
                                .hakukohdeOid(hakukohdeOid)
                                .hakijaOid(p.getPersonOid())
                                .hakemusOid(p.getOid())
                                .addAll(p.getAdditionalData())
                                .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO)
                                .build())
                );
                onSuccess.accept("ok");
            }, error -> onError.accept("Lisätietojen tallennus hakemukselle epäonnistui", error));
            return null;
        };

        Supplier<Void> tallennaKielikoetulokset = () -> {
            kielikoetuloksetSureen.keySet().stream().forEach(hakemusOid ->
            {
                String valmistuminen = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(null);
                String personOid = pistetiedot.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
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
                                    .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO)
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
                    organisaatioAsyncResource.haeOrganisaationTyyppiHierarkia(tarjoajaOid).subscribe(hierarkia ->
                    {
                        etsiOppilaitosHierarkiasta(tarjoajaOid, hierarkia.getOrganisaatiot(), myontajaRef);
                        if(isEmpty(myontajaRef.get())) {
                            onError.accept("Hakukohteen " + hakukohdeOid + " suoritukselle ei löytynyt myöntäjää.",
                                    new Exception("Hakukohteen " + hakukohdeOid + " suoritukselle ei löytynyt myöntäjää."));
                        }
                        tallennaKielikoetulokset.get();
                    }, error -> onError.accept("Myöntäjän etsiminen kielikoesuoritukseen epäonnistui", error));
            }, error -> onError.accept("Hakukohteen haku Tarjonnasta epäonnistui", error));
        }*/
        return new SingleSaveResult("foo", false);
    }

    private Predicate<HakutoiveDTO> containsKielikoeResult() {
        return h -> h.getValinnanVaiheet().stream().anyMatch(vaihe -> vaihe.getValintakokeet().stream().anyMatch(isKielikoeParticipation()));
    }

    private Predicate<ValintakoeDTO> isKielikoeParticipation() {
        return koe -> Osallistuminen.OSALLISTUU.equals(koe.getOsallistuminenTulos().getOsallistuminen()) &&
            Arrays.asList("kielikoe_fi", "kielikoe_sv").contains(koe.getValintakoeTunniste());
    }

    public static class SingleSaveResult {
        public final String tunniste;
        public final boolean hyvaksytty;

        public SingleSaveResult(String tunniste, boolean hyvaksytty) {
            this.tunniste = tunniste;
            this.hyvaksytty = hyvaksytty;
        }
    }
}
