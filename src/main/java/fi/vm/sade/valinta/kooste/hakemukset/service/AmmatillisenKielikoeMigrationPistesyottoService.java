package fi.vm.sade.valinta.kooste.hakemukset.service;

import static fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation.PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.hakemukset.service.AmmatillisenKielikoeMigrationService.Result;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import rx.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmmatillisenKielikoeMigrationPistesyottoService extends AbstractPistesyottoKoosteService {
    @Autowired
    public AmmatillisenKielikoeMigrationPistesyottoService(ApplicationAsyncResource applicationAsyncResource,
                                                           SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                                           TarjontaAsyncResource tarjontaAsyncResource,
                                                           OrganisaatioAsyncResource organisaatioAsyncResource,
                                                           ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        super(applicationAsyncResource, suoritusrekisteriAsyncResource, tarjontaAsyncResource, organisaatioAsyncResource, valintalaskentaValintakoeAsyncResource);

    }


    public Subscription save(List<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTOs, Result r,
                             Consumer<Result> onSuccess, BiConsumer<String, Throwable> onError, String username) {
        SureenMigroitavatAmmatillisenKielikoeSuoritukset tallennettavatTiedot = SureenMigroitavatAmmatillisenKielikoeSuoritukset.create(valintakoeOsallistuminenDTOs);

        Stream<Observable<Result>> resultStream = tallennettavatTiedot.tallennettavatTiedotHakukohdeOidinMukaan.values().stream().map(kohteenTiedot ->
            Observable.create((Observable.OnSubscribe<Result>) subscriber -> {
                String hakuOid = kohteenTiedot.hakuOid;
                String hakukohdeOid = kohteenTiedot.hakukohdeOid;
                List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle = kohteenTiedot.hakemusJaPersonOidit;
                Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen = kohteenTiedot.kielikoeTuloksetHakemuksittain;
                Result result = new Result(r.startingFrom);
                kielikoetuloksetSureen.values().forEach(hakijanTulokset ->
                        hakijanTulokset.forEach(tulos ->
                                result.add(tulos.kokeenTunnus, tulos.arvioArvosana)));
                tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen,
                        username, PISTETIEDOT_AMMATTILLISEN_KIELIKOKEEN_MIGRAATIO, false)
                        .subscribe((a) -> {
                            subscriber.onNext(result);
                            subscriber.onCompleted();
                        }, (e) -> {
                            subscriber.onError(e);
                        });
            }));
        return Observable.zip(resultStream.collect(Collectors.toList()), resultsList -> {
            Stream<Result> stream = Arrays.stream(resultsList).map(x -> ((Result) x));
            return stream.reduce(new Result(r.startingFrom), Result::plus);
        }).subscribe(onSuccess::accept, e -> onError.accept("Pistetietojen tallennus epäonnistui", e));
    }
}
