package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoKoosteService extends AbstractPistesyottoKoosteService {

    @Autowired
    public PistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                    SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                    TarjontaAsyncResource tarjontaAsyncResource,
                                    OrganisaatioAsyncResource organisaatioAsyncResource) {
        super(applicationAsyncResource, suoritusrekisteriAsyncResource, tarjontaAsyncResource, organisaatioAsyncResource);
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
            List<ApplicationAdditionalDataDTO> pistetietoDTOs,
            String username,
            Consumer<String> onSuccess,
            BiConsumer<String, Throwable> onError) {

        Map<String, List<AbstractPistesyottoKoosteService.SingleKielikoeTulos>> kielikoetuloksetSureen = new HashMap<>();
        Date valmistuminen = new Date();
        List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle = pistetietoDTOs.stream().flatMap(pistetieto -> {
            String hakemusOid = pistetieto.getOid();
            Map<String, String> additionalData = pistetieto.getAdditionalData();

            List<String> kielikoeAvaimet = additionalData.keySet().stream().filter(a -> a.matches(PistesyottoExcel.KIELIKOE_REGEX)).collect(Collectors.toList());
            if (0 < kielikoeAvaimet.size()) {
                kielikoetuloksetSureen.put(hakemusOid, kielikoeAvaimet.stream().map(avain ->
                    new SingleKielikoeTulos(avain, additionalData.get(avain), valmistuminen)).collect(Collectors.toList()));
            }
            kielikoeAvaimet.forEach(additionalData::remove);
            return Stream.of(pistetieto);
        }).filter(a -> !a.getAdditionalData().isEmpty()).collect(Collectors.toList());

        tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, onSuccess, onError, username, ValintaperusteetOperation.PISTETIEDOT_KAYTTOLIITTYMA, true);
    }



    private Map<String,String> toAdditionalData(List<Arvosana> arvosanat) {
        Map<String, List<Arvosana>> groupedByKieli = arvosanat.stream().collect(Collectors.groupingBy(Arvosana::getLisatieto));
        return groupedByKieli.keySet().stream().collect(Collectors.toMap(
            kieli -> KIELIKOE_KEY_PREFIX + kieli.toLowerCase(),
            kieli -> groupedByKieli.get(kieli).stream().anyMatch(a -> "TRUE".equalsIgnoreCase(a.getArvio().getArvosana())) ? "true" : "false"
        ));
    }
}
