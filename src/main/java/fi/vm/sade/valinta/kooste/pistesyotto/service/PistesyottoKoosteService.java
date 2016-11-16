package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.PistetietoDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoKoosteService extends AbstractPistesyottoKoosteService {

    @Autowired
    public PistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                    SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                    TarjontaAsyncResource tarjontaAsyncResource,
                                    OhjausparametritAsyncResource ohjausparametritAsyncResource,
                                    OrganisaatioAsyncResource organisaatioAsyncResource,
                                    ValintaperusteetAsyncResource valintaperusteetAsyncResource,
                                    ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        super(applicationAsyncResource,
                suoritusrekisteriAsyncResource,
                tarjontaAsyncResource,
                ohjausparametritAsyncResource,
                organisaatioAsyncResource,
                valintaperusteetAsyncResource,
                valintalaskentaValintakoeAsyncResource);
    }

    public Observable<List<PistetietoDTO>> koostaOsallistujienPistetiedot(String hakuOid, String hakukohdeOid, List<String> hakemusOidit) {
        return Observable.zip(
                applicationAsyncResource.getApplicationAdditionalData(hakemusOidit),
                valintaperusteetAsyncResource.findAvaimet(hakukohdeOid),
                valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid)
                        .map(vs -> vs.stream().collect(Collectors.toMap(v -> v.getHakemusOid(), v -> v))),
                suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohdeOid, hakuOid)
                        .map(os -> os.stream().collect(Collectors.toMap(o -> o.getOppijanumero(), o -> o))),
                ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid),
                (additionalDatat, valintaperusteet, valintakokeet, oppijat, ohjausparametrit) -> {
                    return additionalDatat.stream().map(additionalData ->
                            new PistetietoDTO(
                                    additionalData,
                                    Pair.of(hakukohdeOid, valintaperusteet),
                                    valintakokeet.get(additionalData.getOid()),
                                    oppijat.get(additionalData.getPersonOid()),
                                    ohjausparametrit
                            )
                    ).collect(Collectors.toList());
                }
        );
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
            if (kielikoePistetiedot.isEmpty()) {
                return applicationAsyncResource.putApplicationAdditionalData(hakuOid, Collections.singletonList(pistetietoDTO))
                        .map(a -> null);
            } else {
                return Observable.merge(kielikoePistetiedot.keySet().stream().map(kielikoetunniste -> {
                    poistaKielikoepistetiedot(pistetietoDTO);
                    pistetietoDTO.getAdditionalData().put(kielikoetunniste, kielikoePistetiedot.get(kielikoetunniste));
                    return tallennaKoostetutPistetiedot(
                            hakuOid, kh.get(kielikoetunniste).getHakukohdeOid(),
                            Collections.singletonList(pistetietoDTO), username
                    );
                }).collect(Collectors.toList()));
            }
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
            siirraKielikoepistetiedotKielikoetulosMapiin(valmistuminen, kielikoetuloksetSureen, hakemusOid, additionalData);
            return Stream.of(pistetieto);
        }).filter(a -> !a.getAdditionalData().isEmpty()).collect(Collectors.toList());

        return tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, username, ValintaperusteetOperation.PISTETIEDOT_KAYTTOLIITTYMA);
    }
}
