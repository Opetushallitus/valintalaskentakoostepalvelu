package fi.vm.sade.valinta.kooste.pistesyotto.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
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
import rx.functions.Functions;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoKoosteService extends AbstractPistesyottoKoosteService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoKoosteService.class);

    @Autowired
    public PistesyottoKoosteService(ApplicationAsyncResource applicationAsyncResource,
                                    ValintapisteAsyncResource valintapisteAsyncResource,
                                    SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
                                    TarjontaAsyncResource tarjontaAsyncResource,
                                    OhjausparametritAsyncResource ohjausparametritAsyncResource,
                                    OrganisaatioAsyncResource organisaatioAsyncResource,
                                    ValintaperusteetAsyncResource valintaperusteetAsyncResource,
                                    ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        super(applicationAsyncResource,
                valintapisteAsyncResource,
                suoritusrekisteriAsyncResource,
                tarjontaAsyncResource,
                ohjausparametritAsyncResource,
                organisaatioAsyncResource,
                valintaperusteetAsyncResource,
                valintalaskentaValintakoeAsyncResource);
    }

    public Observable<List<HakemuksenKoetulosYhteenveto>> koostaOsallistujienPistetiedot(String hakuOid, String hakukohdeOid, AuditSession auditSession) {

        try {
            Observable<List<Valintapisteet>> valintapisteet = valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession);
            Observable<Map<String, ValintakoeOsallistuminenDTO>> osallistuminenByHakemus = valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid)
                    .map(vs -> vs.stream().collect(Collectors.toMap(v -> v.getHakemusOid(), v -> v)));
            Observable<Map<String, Oppija>> oppijaByPersonOID = suoritusrekisteriAsyncResource.getOppijatByHakukohdeWithoutEnsikertalaisuus(hakukohdeOid, hakuOid)
                    .map(os -> os.stream().collect(Collectors.toMap(o -> o.getOppijanumero(), o -> o)));
            return Observable.zip(
                    valintapisteet,
                valintaperusteetAsyncResource.findAvaimet(hakukohdeOid),
                    osallistuminenByHakemus,
                    oppijaByPersonOID,
                ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid),
                (additionalDatat, valintaperusteet, valintakokeet, oppijat, ohjausparametrit) ->
                    additionalDatat.stream().map(vps ->
                        new HakemuksenKoetulosYhteenveto(
                                vps,
                            Pair.of(hakukohdeOid, valintaperusteet),
                            valintakokeet.get(vps.getHakemusOID()),
                            oppijat.get(vps.getOppijaOID()),
                            ohjausparametrit
                        )
                    ).collect(Collectors.toList())
            );/*
            //Observable<List<ValintaperusteDTO>> avaimet = valintaperusteetAsyncResource.findAvaimet(hakukohdeOid);
            Observable<List<ValintaperusteDTO>> avaimet = valintaperusteetAsyncResource.findAvaimet(hakukohdeOid);

            Observable<Map<String, ValintakoeOsallistuminenDTO>> osallistumiset = valintalaskentaValintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid)
                    .map(vs -> vs.stream().collect(Collectors.toMap(v -> v.getHakemusOid(), v -> v)));
            Observable<Map<String, Valintapisteet>> valintapisteet = valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession)
                    .map(vs -> vs.stream().collect(Collectors.toMap(v -> v.getHakemusOID(), v -> v)));

            Observable.zip(avaimet, osallistumiset, valintapisteet, (a, o, v) -> v.entrySet().stream().map(
                    v0 -> mergeOsallistuminenWithPisteet.apply(Optional.ofNullable(o.get(v0.getKey())), v0.getValue())).collect(Collectors.toList()));

            return valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession);
            */
        } catch (Exception e) {
            LOG.error(String.format("Ongelma koostettaessa haun %s kohteen %s pistetietoja", hakuOid, hakukohdeOid), e);
            return Observable.error(e);
        }
    }

    public Observable<Map<String, HakemuksenKoetulosYhteenveto>> koostaOsallistujanPistetiedot(String hakemusOid, AuditSession auditSession) {
        Observable<Pair<Hakemus, Map<String, List<String>>>> hakemusAndTarjonta = applicationAsyncResource.getApplication(hakemusOid).flatMap(hakemus -> tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakemus.getApplicationSystemId()).map(tarjonta -> Pair.of(hakemus, tarjonta)));
        return hakemusAndTarjonta.map(ht -> {
            Hakemus hakemus = ht.getKey();
            String hakuOid = hakemus.getApplicationSystemId();
            Map<String, List<String>> hakukohdeRyhmasForHakukohdes = ht.getValue();
            HakemusDTO hakemusDTO = Converter.hakemusToHakemusDTO(hakemus, hakukohdeRyhmasForHakukohdes);
                    Observable<ValintakoeOsallistuminenDTO> koeO = valintalaskentaValintakoeAsyncResource.haeHakemukselle(hakemusOid);
                    Observable<Oppija> oppijaO = suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(hakemus.getPersonOid());
                    Observable<ParametritDTO> parametritO = ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid);
                    Observable<List<Valintapisteet>> valintapisteet = valintapisteAsyncResource.getValintapisteet(hakuOid, Arrays.asList(hakemusOid), auditSession);
                    Stream<Observable<Pair<String, HakemuksenKoetulosYhteenveto>>> tulokset = hakemusDTO.getHakukohteet().stream().map(hakukohde -> {
                        String hakukohdeOid = hakukohde.getOid();

                        return Observable.zip(valintaperusteetAsyncResource.findAvaimet(hakukohdeOid),
                                koeO,
                                oppijaO,
                                parametritO,
                                valintapisteet,
                                (valintaperusteet, valintakoeOsallistuminen, oppija, ohjausparametrit, pisteet) ->
                                        Pair.of(
                                                hakukohdeOid,
                                                new HakemuksenKoetulosYhteenveto(
                                                        pisteet.iterator().next(),
                                                        Pair.of(hakukohdeOid, valintaperusteet),
                                                        valintakoeOsallistuminen,
                                                        oppija,
                                                        ohjausparametrit)));

                    });

                    return Observable.merge(tulokset.collect(Collectors.toList())).toMap(Pair::getLeft, Pair::getRight);
                }
        ).flatMap(r -> r);
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
                                                                     String username, AuditSession auditSession) {
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
                            Collections.singletonList(a), username, auditSession
                    );
                }).collect(Collectors.toList()));
            }
        }).lastOrDefault(null);
    }

    public Observable<Void> tallennaKoostetutPistetiedot(String hakuOid,
                                                         String hakukohdeOid,
                                                         List<ApplicationAdditionalDataDTO> pistetietoDTOs,
                                                         String username, AuditSession auditSession) {
        Map<String, List<AbstractPistesyottoKoosteService.SingleKielikoeTulos>> kielikoetuloksetSureen = new HashMap<>();
        List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle;
        try {
            pistetiedotHakemukselle = createAdditionalDataAndPopulateKielikoetulokset(pistetietoDTOs, kielikoetuloksetSureen);
            return tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, pistetiedotHakemukselle, kielikoetuloksetSureen, username, ValintaperusteetOperation.PISTETIEDOT_KAYTTOLIITTYMA, auditSession);
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
