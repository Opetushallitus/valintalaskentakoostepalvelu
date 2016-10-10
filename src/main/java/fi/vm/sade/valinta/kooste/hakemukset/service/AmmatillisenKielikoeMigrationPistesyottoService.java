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
import java.util.Date;
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
                Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen = kohteenTiedot.kielikoeTuloksetHakemuksittain;
                Result result = new Result(r.startingFrom);
                Consumer<String> successHandler = message -> {
                    kielikoetuloksetSureen.values().forEach(hakijanTulokset ->
                        hakijanTulokset.forEach(tulos -> result.add(tulos.kokeenTunnus, Boolean.TRUE.toString().equals(tulos.arvioArvosana))));
                    subscriber.onNext(result);
                };
                tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen,
                    successHandler, onError, username, PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO, false);
            }));

        return Observable.zip(resultStream.collect(Collectors.toList()), resultsList -> {
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
            if (hakutoiveetWithKielikoeResults.size() != 1 && hakutoiveetWithKielikoeResults.size() != 2) {
                List<String> hakukohdeOids = hakutoiveetWithKielikoeResults.stream().map(HakutoiveDTO::getHakukohdeOid).collect(Collectors.toList());
                throw new IllegalStateException(String.format("Odotettiin täsämälleen yhtä tai kahta toivetta, jossa on kielikokeeseen osallistuminen, " +
                    "mutta hakemukselle %s löytyi %s kpl: %s", valintakoeOsallistuminen.getHakemusOid(), hakutoiveetWithKielikoeResults.size(), hakukohdeOids));
            }

            String hakuOid = valintakoeOsallistuminen.getHakuOid();
            String hakemusOid = valintakoeOsallistuminen.getHakemusOid();
            String hakijaOid = valintakoeOsallistuminen.getHakijaOid();
            Date createdAt = valintakoeOsallistuminen.getCreatedAt();

            hakutoiveetWithKielikoeResults.forEach(kielikoetuloksenSisaltavaHakutoive ->
                lisaaKielikoeTulos(kielikoetuloksenSisaltavaHakutoive, hakuOid, hakemusOid, hakijaOid, createdAt));
        }

        private void lisaaKielikoeTulos(HakutoiveDTO kielikoetuloksenSisaltavaHakutoive, String hakuOid, String hakemusOid, String hakijaOid, Date createdAt) {
            String hakukohdeOid = kielikoetuloksenSisaltavaHakutoive.getHakukohdeOid();
            tallennettavatTiedotHakukohdeOidinMukaan.compute(hakukohdeOid, (k, kohteenTiedot) -> {
                if (kohteenTiedot == null) {
                    kohteenTiedot = new YhdenHakukohteenTallennettavatTiedot(hakuOid, hakukohdeOid);
                    kohteenTiedot.lisaaTulos(hakemusOid, hakijaOid, kielikoetuloksenSisaltavaHakutoive, createdAt);
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
        final Map<String, List<AbstractPistesyottoKoosteService.SingleKielikoeTulos>> kielikoeTuloksetHakemuksittain = new HashMap<>();
        final List<ApplicationAdditionalDataDTO> hakemusJaPersonOidit = new LinkedList<>();

        YhdenHakukohteenTallennettavatTiedot(String hakuOid, String hakukohdeOid) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
        }

        void lisaaTulos(String hakemusOid, String hakijaOid, HakutoiveDTO kielikoetuloksenSisaltavaHakutoive, Date createdAt) {
            kielikoeTuloksetHakemuksittain.compute(hakemusOid, (key, resultListOfHakemus) -> {
                if (resultListOfHakemus == null) {
                    resultListOfHakemus = new LinkedList<>();
                }
                resultListOfHakemus.add(extractKielikoeTulos(kielikoetuloksenSisaltavaHakutoive, hakemusOid, createdAt));
                return resultListOfHakemus;
            });
            hakemusJaPersonOidit.add(new ApplicationAdditionalDataDTO(hakemusOid, hakijaOid, null, null, null));
        }

        private AbstractPistesyottoKoosteService.SingleKielikoeTulos extractKielikoeTulos(HakutoiveDTO kielikoetuloksenSisaltavaHakutoive, String hakemusOid, Date createdAt) {
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
            return new SingleKielikoeTulos(tunniste, Boolean.toString(hyvaksytty), createdAt);
        }
    }
}
