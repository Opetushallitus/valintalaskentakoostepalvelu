package fi.vm.sade.valinta.kooste.hakemukset.service;

import static fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation.PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.service.AmmatillisenKielikoeMigrationService.Result;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import rx.Subscription;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmmatillisenKielikoeMigrationPistesyottoService extends AbstractPistesyottoKoosteService {

    @Autowired
    public AmmatillisenKielikoeMigrationPistesyottoService(ApplicationAsyncResource applicationAsyncResource,
                                                           SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                                           TarjontaAsyncResource tarjontaAsyncResource,
                                                           OrganisaatioAsyncResource organisaatioAsyncResource) {
        super(applicationAsyncResource, suoritusrekisteriAsyncResource, tarjontaAsyncResource, organisaatioAsyncResource);
    }


    public Subscription save(List<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTOs, Result r,
                             Consumer<Result> onSuccess, BiConsumer<String, Throwable> onError, String username) {
        SureenTallennettavatTiedot tallennettavatTiedot = SureenTallennettavatTiedot.create(valintakoeOsallistuminenDTOs);

        Stream<Observable<Result>> resultStream = tallennettavatTiedot.tallennettavatTiedotHakukohdeOidinMukaan.values().stream().map(kohteenTiedot ->
            Observable.create((Observable.OnSubscribe<Result>) subscriber -> {
                String hakuOid = kohteenTiedot.hakuOid;
                String hakukohdeOid = kohteenTiedot.hakukohdeOid;
                List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle = kohteenTiedot.hakemusJaPersonOidit;
                Map<String, Map<String, String>> kielikoetuloksetSureen = kohteenTiedot.kielikoeTuloksetHakemuksittain;
                Result result = new Result(r.startingFrom);
                Consumer<String> successHandler = message -> {
                    kielikoetuloksetSureen.values().forEach(tulos ->
                        tulos.forEach((tunniste, arvo) -> result.add(tunniste, Boolean.TRUE.toString().equals(arvo))));
                    subscriber.onNext(result);
                };
                tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen,
                    successHandler, onError, username, PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO);
            }));

        return Observable.combineLatest(resultStream.collect(Collectors.toList()), resultsList -> {
            Stream<Result> stream = Arrays.stream(resultsList).map(x -> ((Result) x));
            return stream.reduce(new Result(r.startingFrom), Result::plus);
        }).subscribe(onSuccess::accept);
    }

    private static Predicate<HakutoiveDTO> containsKielikoeResult() {
        return h -> h.getValinnanVaiheet().stream().anyMatch(containsKielikoeParticipation());
    }

    private static Predicate<ValintakoeValinnanvaiheDTO> containsKielikoeParticipation() {
        return vaihe -> vaihe.getValintakokeet().stream().anyMatch(isKielikoeParticipation());
    }

    private static Predicate<ValintakoeDTO> isKielikoeParticipation() {
        return koe -> Osallistuminen.OSALLISTUU.equals(koe.getOsallistuminenTulos().getOsallistuminen()) &&
            Arrays.asList("kielikoe_fi", "kielikoe_sv").contains(koe.getValintakoeTunniste());
    }

    private static class SureenTallennettavatTiedot {
        final Map<String,YhdenHakukohteenTallennettavatTiedot> tallennettavatTiedotHakukohdeOidinMukaan = new HashMap<>();

        private SureenTallennettavatTiedot() {
        }

        private void lisaaKielikoeTulos(ValintakoeOsallistuminenDTO valintakoeOsallistuminen) {
            List<HakutoiveDTO> hakutoiveetWithKielikoeResults = valintakoeOsallistuminen.getHakutoiveet().stream().filter(containsKielikoeResult()).collect(Collectors.toList());
            if (hakutoiveetWithKielikoeResults.size() != 1) {
                throw new IllegalStateException(String.format("Odotettiin täsämälleen yhtä toivetta, jossa on kielikokeeseen osallistuminen, " +
                    "mutta löytyi %s kpl: %s", hakutoiveetWithKielikoeResults.size(), hakutoiveetWithKielikoeResults));
            }

            HakutoiveDTO kielikoetuloksenSisaltavaHakutoive = hakutoiveetWithKielikoeResults.get(0);
            String hakukohdeOid = kielikoetuloksenSisaltavaHakutoive.getHakukohdeOid();
            String hakuOid = valintakoeOsallistuminen.getHakuOid();

            tallennettavatTiedotHakukohdeOidinMukaan.compute(hakukohdeOid, (k, kohteenTiedot) -> {
                if (kohteenTiedot == null) {
                    kohteenTiedot = new YhdenHakukohteenTallennettavatTiedot(hakuOid, hakukohdeOid);
                    kohteenTiedot.lisaaTulos(valintakoeOsallistuminen.getHakemusOid(), valintakoeOsallistuminen.getHakijaOid(), kielikoetuloksenSisaltavaHakutoive);
                }
                return kohteenTiedot;
            });
        }

        public static SureenTallennettavatTiedot create(List<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTOs) {
            SureenTallennettavatTiedot t = new SureenTallennettavatTiedot();
            valintakoeOsallistuminenDTOs.forEach(t::lisaaKielikoeTulos);
            return t;
        }
    }

    private static class YhdenHakukohteenTallennettavatTiedot {
        final String hakuOid;
        final String hakukohdeOid;
        final Map<String,Map<String,String>> kielikoeTuloksetHakemuksittain = new HashMap<>();
        final List<ApplicationAdditionalDataDTO> hakemusJaPersonOidit = new LinkedList<>();

        YhdenHakukohteenTallennettavatTiedot(String hakuOid, String hakukohdeOid) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
        }

        void lisaaTulos(String hakemusOid, String hakijaOid, HakutoiveDTO kielikoetuloksenSisaltavaHakutoive) {
            kielikoeTuloksetHakemuksittain.put(hakemusOid, extractKielikoeTulos(kielikoetuloksenSisaltavaHakutoive, hakemusOid));
            hakemusJaPersonOidit.add(new ApplicationAdditionalDataDTO(hakemusOid, hakijaOid, null, null, null));
        }

        private Map<String, String> extractKielikoeTulos(HakutoiveDTO kielikoetuloksenSisaltavaHakutoive, String hakemusOid) {
            Stream<ValintakoeValinnanvaiheDTO> kielikoetuloksenSisaltavatVaiheet = kielikoetuloksenSisaltavaHakutoive.getValinnanVaiheet().stream()
                .filter(containsKielikoeParticipation());
            List<ValintakoeDTO> kielikoeDtos = kielikoetuloksenSisaltavatVaiheet.flatMap(vaihe ->
                vaihe.getValintakokeet().stream()
                    .filter(isKielikoeParticipation())).collect(Collectors.toList());
            if (kielikoeDtos.size() != 1) {
                throw new IllegalStateException(String.format("Yhdestä poikkeava määrä %s kielikoetuloksia hakemukselle %s, vaikuttaa bugilta: %s",
                    kielikoeDtos.size(), hakemusOid, kielikoeDtos));
            }
            ValintakoeDTO kielikoeDto = kielikoeDtos.get(0);
            String tunniste = kielikoeDto.getValintakoeTunniste();
            boolean hyvaksytty = kielikoeDto.getOsallistuminenTulos().getLaskentaTulos();
            return Collections.singletonMap(tunniste, Boolean.toString(hyvaksytty));
        }
    }
}
