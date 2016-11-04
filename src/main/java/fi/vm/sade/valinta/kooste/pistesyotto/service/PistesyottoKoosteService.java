package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Hakutoive;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
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
                                    OrganisaatioAsyncResource organisaatioAsyncResource,
                                    ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        super(applicationAsyncResource,
                suoritusrekisteriAsyncResource,
                tarjontaAsyncResource,
                organisaatioAsyncResource,
                valintalaskentaValintakoeAsyncResource);
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

    private static Map<String, HakutoiveDTO> kielikokeidenHakukohteet(ValintakoeOsallistuminenDTO voDTO) {
        Map<String, HakutoiveDTO> kielikokeidenHakukohteet = new HashMap<>();
        voDTO.getHakutoiveet().forEach(h ->
            h.getValinnanVaiheet().stream()
                .flatMap(vaihe -> vaihe.getValintakokeet().stream())
                .filter(koe -> koe.getOsallistuminenTulos().getOsallistuminen() == Osallistuminen.OSALLISTUU)
                .filter(koe -> koe.getValintakoeTunniste().matches(PistesyottoExcel.KIELIKOE_REGEX))
                .forEach(koe -> {
                    String koetunniste = koe.getValintakoeTunniste();
                    if (kielikokeidenHakukohteet.containsKey(koetunniste)) {
                        throw new IllegalStateException(String.format(
                                "Hakemuksen %s hakija osallistunut kielikokeeseen %s useammassa kuin yhdessä hakukohteessa: %s, %s",
                                voDTO.getHakemusOid(),
                                koetunniste,
                                kielikokeidenHakukohteet.get(koetunniste),
                                h.getHakukohdeOid()
                        ));
                    }
                    kielikokeidenHakukohteet.put(koetunniste, h);
                }));
        return kielikokeidenHakukohteet;
    }

    private static void poistaKielikoepistetiedot(ApplicationAdditionalDataDTO pistetietoDTO) {
        Iterator<String> i = pistetietoDTO.getAdditionalData().keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            if (key.matches(PistesyottoExcel.KIELIKOE_REGEX)) {
                i.remove();
            }
        }
    }

    public Observable<Void> tallennaKoostetutPistetiedotHakemukselle(ApplicationAdditionalDataDTO pistetietoDTO,
                                                                     String username) {
        return valintalaskentaValintakoeAsyncResource.haeHakemukselle(pistetietoDTO.getOid()).flatMap(vo -> {
            String hakuOid = vo.getHakuOid();
            Map<String, HakutoiveDTO> kh = kielikokeidenHakukohteet(vo);
            Map<String, String> kielikoePistetiedot = pistetietoDTO.getAdditionalData().entrySet().stream()
                    .filter(e -> e.getKey().matches(PistesyottoExcel.KIELIKOE_REGEX))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            return Observable.merge(kielikoePistetiedot.keySet().stream().map(kielikoetunniste -> {
                poistaKielikoepistetiedot(pistetietoDTO);
                pistetietoDTO.getAdditionalData().put(kielikoetunniste, kielikoePistetiedot.get(kielikoetunniste));
                return tallennaKoostetutPistetiedot(
                        hakuOid, kh.get(kielikoetunniste).getHakukohdeOid(),
                        Collections.singletonList(pistetietoDTO), username
                );
            }).collect(Collectors.toList()));
        }).lastOrDefault(null);
    }

    public Observable<Void> tallennaKoostetutPistetiedot(String hakuOid,
                                                         String hakukohdeOid,
                                                         List<ApplicationAdditionalDataDTO> pistetietoDTOs,
                                                         String username) {
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

        return tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, username, ValintaperusteetOperation.PISTETIEDOT_KAYTTOLIITTYMA);
    }



    private Map<String,String> toAdditionalData(List<Arvosana> arvosanat) {
        Map<String, List<Arvosana>> groupedByKieli = arvosanat.stream().collect(Collectors.groupingBy(Arvosana::getLisatieto));
        return groupedByKieli.keySet().stream().collect(Collectors.toMap(
            kieli -> KIELIKOE_KEY_PREFIX + kieli.toLowerCase(),
            kieli -> groupedByKieli.get(kieli).stream().anyMatch(a -> "TRUE".equalsIgnoreCase(a.getArvio().getArvosana())) ? "true" : "false"
        ));
    }
}
