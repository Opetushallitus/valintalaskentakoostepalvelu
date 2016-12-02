package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoKoosteService extends AbstractPistesyottoKoosteService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoKoosteService.class);

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

    public Observable<List<HakemuksenKoetulosYhteenveto>> koostaOsallistujienPistetiedot(String hakuOid, String hakukohdeOid) {
        try {
            return Observable.zip(
                applicationAsyncResource.getApplicationAdditionalData(hakuOid, hakukohdeOid),
                valintaperusteetAsyncResource.findAvaimet(hakukohdeOid),
                valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid)
                    .map(vs -> vs.stream().collect(Collectors.toMap(v -> v.getHakemusOid(), v -> v))),
                suoritusrekisteriAsyncResource.getOppijatByHakukohdeWithoutEnsikertalaisuus(hakukohdeOid, hakuOid)
                    .map(os -> os.stream().collect(Collectors.toMap(o -> o.getOppijanumero(), o -> o))),
                ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid),
                (additionalDatat, valintaperusteet, valintakokeet, oppijat, ohjausparametrit) ->
                    additionalDatat.stream().map(additionalData ->
                        new HakemuksenKoetulosYhteenveto(
                            additionalData,
                            Pair.of(hakukohdeOid, valintaperusteet),
                            valintakokeet.get(additionalData.getOid()),
                            oppijat.get(additionalData.getPersonOid()),
                            ohjausparametrit
                        )
                    ).collect(Collectors.toList())
            );
        } catch (Exception e) {
            LOG.error(String.format("Ongelma koostettaessa haun %s kohteen %s pistetietoja", hakuOid, hakukohdeOid), e);
            return Observable.error(e);
        }
    }

    public Observable<Map<String, HakemuksenKoetulosYhteenveto>> koostaOsallistujanPistetiedot(String hakemusOid) {
        return applicationAsyncResource.getApplication(hakemusOid).flatMap(hakemus -> {
            String hakuOid = hakemus.getApplicationSystemId();
            HakemusDTO hakemusDTO = Converter.hakemusToHakemusDTO(hakemus);
            Observable<ValintakoeOsallistuminenDTO> koeO = valintalaskentaValintakoeAsyncResource.haeHakemukselle(hakemusOid);
            Observable<Oppija> oppijaO = suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(hakemus.getPersonOid());
            Observable<ParametritDTO> parametritO = ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid);
            return Observable.merge(hakemusDTO.getHakukohteet().stream()
                .map(hakukohdeDTO -> {
                    String hakukohdeOid = hakukohdeDTO.getOid();
                    return Observable.zip(valintaperusteetAsyncResource.findAvaimet(hakukohdeOid),
                        koeO,
                        oppijaO,
                        parametritO,
                        (valintaperusteet, valintakoeOsallistuminen, oppija, ohjausparametrit) ->
                            Pair.of(
                                hakukohdeOid,
                                new HakemuksenKoetulosYhteenveto(
                                    new ApplicationAdditionalDataDTO(hakemusOid, hakemus.getPersonOid(), hakemusDTO.getEtunimi(), hakemusDTO.getSukunimi(), hakemus.getAdditionalInfo()),
                                    Pair.of(hakukohdeOid, valintaperusteet),
                                    valintakoeOsallistuminen,
                                    oppija,
                                    ohjausparametrit)));
                }).collect(Collectors.toList())
            ).toMap(Pair::getLeft, Pair::getRight);
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

    private static ApplicationAdditionalDataDTO poistaKielikoepistetiedot(ApplicationAdditionalDataDTO pistetietoDTO) {
        ApplicationAdditionalDataDTO a = new ApplicationAdditionalDataDTO(
                pistetietoDTO.getOid(),
                pistetietoDTO.getPersonOid(),
                pistetietoDTO.getFirstNames(),
                pistetietoDTO.getLastName(),
                new HashMap<>()
        );
        pistetietoDTO.getAdditionalData().entrySet().stream()
                .filter(e -> !e.getKey().matches(PistesyottoExcel.KIELIKOE_REGEX))
                .forEach(e -> a.getAdditionalData().put(e.getKey(), e.getValue()));
        return a;
    }

    public Observable<Void> tallennaKoostetutPistetiedotHakemukselle(ApplicationAdditionalDataDTO pistetietoDTO,
                                                                     String username) {
        return valintalaskentaValintakoeAsyncResource.haeHakemukselle(pistetietoDTO.getOid()).flatMap(vo -> {
            String hakuOid = vo.getHakuOid();
            Map<String, HakutoiveDTO> kh = kielikokeidenHakukohteet(vo);
            Map<String, String> kielikoePistetiedot = pistetietoDTO.getAdditionalData().entrySet().stream()
                    .filter(e -> e.getKey().matches(PistesyottoExcel.KIELIKOE_REGEX))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (kielikoePistetiedot.isEmpty()) {
                return applicationAsyncResource.putApplicationAdditionalData(hakuOid, Collections.singletonList(pistetietoDTO))
                        .map(a -> null);
            } else {
                return Observable.merge(kielikoePistetiedot.keySet().stream().map(kielikoetunniste -> {
                    ApplicationAdditionalDataDTO a = poistaKielikoepistetiedot(pistetietoDTO);
                    a.getAdditionalData().put(kielikoetunniste, kielikoePistetiedot.get(kielikoetunniste));
                    return tallennaKoostetutPistetiedot(
                            hakuOid, kh.get(kielikoetunniste).getHakukohdeOid(),
                            Collections.singletonList(a), username
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
        List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle;
        try {
            pistetiedotHakemukselle = createAdditionalDataAndPopulateKielikoetulokset(pistetietoDTOs, kielikoetuloksetSureen);
            return tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, username, ValintaperusteetOperation.PISTETIEDOT_KAYTTOLIITTYMA);
        } catch (Exception e) {
            LOG.error(String.format("Ongelma käsiteltäessä pistetietoja haun %s kohteelle %s , käyttäjä %s ", hakuOid, hakukohdeOid, username), e);
            return Observable.error(e);
        }
    }

    private List<ApplicationAdditionalDataDTO> createAdditionalDataAndPopulateKielikoetulokset(List<ApplicationAdditionalDataDTO> pistetietoDTOs, Map<String, List<SingleKielikoeTulos>> kielikoetuloksetSureen) {
        Date valmistuminen = new Date();
        return pistetietoDTOs.stream().flatMap(pistetieto -> {
            String hakemusOid = pistetieto.getOid();
            Map<String, String> additionalData = pistetieto.getAdditionalData();
            siirraKielikoepistetiedotKielikoetulosMapiin(valmistuminen, kielikoetuloksetSureen, hakemusOid, additionalData);
            return Stream.of(pistetieto);
        }).filter(a -> !a.getAdditionalData().isEmpty()).collect(Collectors.toList());
    }
}
