package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jasig.cas.client.util.CommonUtils.isNotEmpty;

public class PistesyottoKoosteService {
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired
    public PistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                    SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                    TarjontaAsyncResource tarjontaAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
    }

    public Observable<List<ApplicationAdditionalDataDTO>> koostaOsallistujienPistetiedot(String hakuOid, String hakukohdeOid, List<String> hakemusOidit) {
        Observable<List<Oppija>> oppijatObservable = suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid);
        Observable<List<ApplicationAdditionalDataDTO>> pistetiedotObservable = applicationAsyncResource.getApplicationAdditionalData(hakemusOidit);

        return pistetiedotObservable.zipWith(oppijatObservable, (pistetiedot, oppijat) -> {
            Map<String, List<Arvosana>> kielikoeArvosanat = ammatillisenKielikoeArvosanat(oppijat);

            pistetiedot.stream().filter(pt -> kielikoeArvosanat.keySet().contains(pt.getPersonOid())).forEach(pt ->
                    pt.getAdditionalData().putAll(toAdditionalData(kielikoeArvosanat.get(pt.getPersonOid())))
            );

            return pistetiedot;
        });
    }

    public void tallennaKoostetutPistetiedot(
            String hakuOid,
            String hakukohdeOid,
            List<ApplicationAdditionalDataDTO> pistetiedoDTOs,
            Consumer<String> onSuccess,
            BiConsumer<String, Throwable> onError) {

        Map<String, Map<String, String>> kielikoetuloksetSureen = new HashMap<>();
        List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle = pistetiedoDTOs.stream().flatMap(pistetieto -> {
            String hakemusOid = pistetieto.getOid();
            Map<String, String> additionalData = pistetieto.getAdditionalData();

            List<String> kielikoeAvaimet = additionalData.keySet().stream().filter(a -> a.matches(PistesyottoExcel.KIELIKOE_REGEX)).collect(Collectors.toList());
            if(0 < kielikoeAvaimet.size()) {
                kielikoetuloksetSureen.put(hakemusOid, kielikoeAvaimet.stream().filter(avain -> isNotEmpty(additionalData.get(avain))).collect(Collectors.toMap(
                        avain -> avain,
                        avain -> additionalData.get(avain)
                )));
            }
            kielikoeAvaimet.stream().forEach(a -> additionalData.remove(a));
            return Stream.of(pistetieto);
        }).filter(a -> !a.getAdditionalData().isEmpty()).collect(Collectors.toList());

        tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, onSuccess, onError);
    }

    private void tallennaKoostetutPistetiedot(String hakuOid,
                                              String hakukohdeOid,
                                              List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle,
                                              Map<String, Map<String, String>> kielikoetuloksetSureen,
                                              Consumer<String> onSuccess,
                                              BiConsumer<String, Throwable> onError) {

        AtomicInteger laskuri = new AtomicInteger(kielikoetuloksetSureen.values().stream().mapToInt(map -> map.size()).sum());
        AtomicReference<String> myontajaRef = new AtomicReference<>();

        Supplier<Void> tallennaAdditionalInfo = () -> {
            applicationAsyncResource.putApplicationAdditionalData(
                    hakuOid, hakukohdeOid, pistetiedotHakemukselle).subscribe(response -> {
                /*pistetiedotHakemukselle.forEach(p ->
                        AUDIT.log(builder()
                                .id(username)
                                .hakuOid(hakuOid)
                                .hakukohdeOid(hakukohdeOid)
                                .hakijaOid(p.getPersonOid())
                                .hakemusOid(p.getOid())
                                .addAll(p.getAdditionalData())
                                .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                                .build())
                );*/
                onSuccess.accept("ok");
            }, error -> onError.accept("Lisätietojen tallennus hakemukselle epäonnistui", error));
            return null;
        };

        Supplier<Void> tallennaKielikoetulokset = () -> {
            kielikoetuloksetSureen.keySet().stream().forEach(hakemusOid ->
            {
                String valmistuminen = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
                String personOid = pistetiedotHakemukselle.stream().filter(p -> p.getOid().equals(hakemusOid)).findFirst().get().getPersonOid();
                Map<String, String> kielikoetulokset = kielikoetuloksetSureen.get(hakemusOid);
                kielikoetulokset.keySet().stream().filter(t -> isNotEmpty(kielikoetulokset.get(t))).forEach(tunnus -> {
                    String kieli = tunnus.substring(9);

                    Suoritus suoritus = new Suoritus();
                    suoritus.setTila("VALMIS");
                    suoritus.setYksilollistaminen("Ei");
                    suoritus.setHenkiloOid(personOid);
                    suoritus.setVahvistettu(true);
                    suoritus.setSuoritusKieli(kieli.toUpperCase());
                    suoritus.setMyontaja(myontajaRef.get());
                    suoritus.setKomo("ammatillisenKielikoe");
                    suoritus.setValmistuminen(valmistuminen);

                    suoritusrekisteriAsyncResource.postSuoritus(suoritus).subscribe( tallennettuSuoritus -> {
                        String arvioArvosana = kielikoetulokset.get(tunnus).toUpperCase();

                        Arvosana arvosana = new Arvosana();
                        arvosana.setAine("KIELIKOE");
                        arvosana.setLisatieto(kieli.toUpperCase());
                        arvosana.setArvio(new Arvio(arvioArvosana, null, null));
                        arvosana.setSuoritus(tallennettuSuoritus.getId());

                        suoritusrekisteriAsyncResource.postArvosana(arvosana).subscribe(arvosanaResponse -> {
                            /*AUDIT.log(builder()
                                    .id(username)
                                    .hakuOid(hakuOid)
                                    .hakukohdeOid(hakukohdeOid)
                                    .hakijaOid(personOid)
                                    .hakemusOid(hakemusOid)
                                    .addAll(ImmutableMap.of("kielikoe_" + kieli.toLowerCase(), arvioArvosana))
                                    .setOperaatio(ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL)
                                    .build());*/
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
                myontajaRef.set(hakukohde.getTarjoajaOids().stream().findFirst().orElse(""));
                tallennaKielikoetulokset.get();
            }, error -> onError.accept("Hakukohteen haku Tarjonnasta epäonnistui", error));
        }
    }

    private Map<String,String> toAdditionalData(List<Arvosana> arvosanat) {
        Map<String, List<Arvosana>> groupedByKieli = arvosanat.stream().collect(Collectors.groupingBy(Arvosana::getLisatieto));
        return groupedByKieli.keySet().stream().collect(Collectors.toMap(
            kieli -> "kielikoe_" + kieli.toLowerCase(),
            kieli -> groupedByKieli.get(kieli).stream().anyMatch(a -> "TRUE".equalsIgnoreCase(a.getArvio().getArvosana())) ? "TRUE" : "FALSE"
        ));
    }

    private Map<String, List<Arvosana>> ammatillisenKielikoeArvosanat(List<Oppija> oppijat) {
        return oppijat.stream().collect(
                Collectors.toMap(Oppija::getOppijanumero,
                        o -> o.getSuoritukset().stream()
                                .filter(sa -> "ammatillisenKielikoe".equalsIgnoreCase(sa.getSuoritus().getKomo())).map(SuoritusJaArvosanat::getArvosanat).flatMap(List::stream)
                                .filter(a -> "kielikoe".equalsIgnoreCase(a.getAine())).collect(Collectors.toList()))
        );
    }
}
