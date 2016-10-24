package fi.vm.sade.valinta.kooste.hakemukset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SureenMigroitavatAmmatillisenKielikoeSuoritukset {
    private static final Logger LOG = LoggerFactory.getLogger(SureenMigroitavatAmmatillisenKielikoeSuoritukset.class);
    private static Map<String,Date> VALMISTUMIS_DATES_BY_HAKU_OID = new HashMap<>();
    static {
        VALMISTUMIS_DATES_BY_HAKU_OID.put("1.2.246.562.29.90697286251", new LocalDate(2015, 4, 10).toDate()); // "Yhteishaku ammatilliseen ja lukioon, kevät 2015",2015,"kausi_k#1"
        VALMISTUMIS_DATES_BY_HAKU_OID.put("1.2.246.562.29.80306203979", new LocalDate(2015, 10, 14).toDate()); // "Ammatillisen koulutuksen ja lukiokoulutuksen syksyn 2015 yhteishaku",2015,"kausi_s#1"
        VALMISTUMIS_DATES_BY_HAKU_OID.put("1.2.246.562.29.14662042044", new LocalDate(2016, 4, 11).toDate()); // "Yhteishaku ammatilliseen ja lukioon, kevät 2016",2016,"kausi_k#1"
        VALMISTUMIS_DATES_BY_HAKU_OID.put("1.2.246.562.29.98929669087", new LocalDate(2016, 9, 1).toDate()); // "Lisähaku kevään 2016 ammatillisen ja lukiokoulutuksen yhteishaussa vapaaksi jääneille opiskelupaikoille",2016,"kausi_k#1"
    }
    
    private final Map<String, Map<String, String>> kielikoeTuloksetHakemuksiltaHakemuksenJaKielikoodinMukaan = new HashMap<>();
    final Map<String,YhdenHakukohteenTallennettavatTiedot> tallennettavatTiedotHakukohdeOidinMukaan = new HashMap<>();

    public static SureenMigroitavatAmmatillisenKielikoeSuoritukset create(List<ValintakoeOsallistuminenDTO> valintakoeOsallistuminenDTOs) {
        SureenMigroitavatAmmatillisenKielikoeSuoritukset t = new SureenMigroitavatAmmatillisenKielikoeSuoritukset();
        valintakoeOsallistuminenDTOs.forEach(t::lisaaKielikoeTulos);
        return t;
    }

    private SureenMigroitavatAmmatillisenKielikoeSuoritukset() {
        ObjectMapper mapper = new ObjectMapper();
        String hakuAppExportLocationInClasspath = "ammatillisenKielikoeSuorituksetHakuAppista.json";
        try {
            File hakuAppExportFile = new ClassPathResource(hakuAppExportLocationInClasspath, getClass()).getFile();
            if (!hakuAppExportFile.canRead()) {
                throw new IllegalStateException(String.format("Ei voida lukea tiedostoa \"%s\"", hakuAppExportFile.getAbsolutePath()));
            }
            byte[] hakuAppResultsBytes = FileUtils.readFileToByteArray(hakuAppExportFile);
            JsonNode parsedHakuAppResults = mapper.reader().readTree(new ByteArrayInputStream(hakuAppResultsBytes));
            parsedHakuAppResults.iterator().forEachRemaining(n -> {
/*
	{
		"additionalInfo" : {
			"kielikoe_fi" : "false",
			"kielikoe_fi-OSALLISTUMINEN" : "OSALLISTUI"
		},
		"oid" : "1.2.246.562.11.00000012522"
	},
* */
                String hakemusOid = n.get("oid").asText();
                kielikoeTuloksetHakemuksiltaHakemuksenJaKielikoodinMukaan.compute(hakemusOid, (k, kielikoeResultsFromHakemus) -> {
                    if (kielikoeResultsFromHakemus == null) {
                        kielikoeResultsFromHakemus = new HashMap<>();
                    }
                    JsonNode additionalInfoNode = n.get("additionalInfo");
                    copyAdditionalInfoFieldsToMap(additionalInfoNode, kielikoeResultsFromHakemus);
                    return kielikoeResultsFromHakemus;
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(String.format("Ongelma haku-appista exportoitujen tulosten lukemisessa tiedostosta %s", hakuAppExportLocationInClasspath), e);
        }
    }

    private static void copyAdditionalInfoFieldsToMap(JsonNode additionalInfoNode, Map<String, String> kielikoeResultsFromHakemus) {
        additionalInfoNode.fields().forEachRemaining(additionalInfoField ->
            kielikoeResultsFromHakemus.put(additionalInfoField.getKey(), additionalInfoField.getValue().asText()));
    }

    private static Date resolveValmistusPaivamaara(String hakuOid, String hakemusOid, Date createdAt, String hakukohdeOid, Date haunMukainenKielikoePvm) {
        if (haunMukainenKielikoePvm != null) {
            return haunMukainenKielikoePvm;
        } else {
            LOG.warn(String.format("Tallennetaan kielikoepäivämääräksi %s hakemukselle %s kohteeseen %s haussa %s",
                createdAt, hakemusOid, hakukohdeOid, hakuOid));
            return createdAt;
        }
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
        Date kielikoePvm = VALMISTUMIS_DATES_BY_HAKU_OID.get(hakuOid);
        if (kielikoePvm == null) {
            LOG.warn(String.format("Ei löydy kielikoepäivämäärää haulle %s, joten käytetään ValintakoeOsallistuminen -olioiden mukaisia päivämääriä", hakuOid));
        }
        tallennettavatTiedotHakukohdeOidinMukaan.compute(hakukohdeOid, (k, kohteenTiedot) -> {
            if (kohteenTiedot == null) {
                kohteenTiedot = new YhdenHakukohteenTallennettavatTiedot(hakuOid, hakukohdeOid, kielikoeTuloksetHakemuksiltaHakemuksenJaKielikoodinMukaan);
            }
            Date valmistumisPvmToUse = resolveValmistusPaivamaara(hakuOid, hakemusOid, createdAt, hakukohdeOid, kielikoePvm);
            kohteenTiedot.lisaaTulos(hakemusOid, hakijaOid, kielikoetuloksenSisaltavaHakutoive, valmistumisPvmToUse);
            return kohteenTiedot;
        });
    }

    static class YhdenHakukohteenTallennettavatTiedot {
        final String hakuOid;
        final String hakukohdeOid;
        private Map<String, Map<String, String>> kielikoeResultsByHakemusOidAndKielikoeTunniste;
        final Map<String, List<AbstractPistesyottoKoosteService.SingleKielikoeTulos>> kielikoeTuloksetHakemuksittain = new HashMap<>();
        final List<ApplicationAdditionalDataDTO> hakemusJaPersonOidit = new LinkedList<>();

        YhdenHakukohteenTallennettavatTiedot(String hakuOid, String hakukohdeOid, Map<String, Map<String, String>> kielikoeResultsByHakemusOidAndKielikoeTunniste) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
            this.kielikoeResultsByHakemusOidAndKielikoeTunniste = kielikoeResultsByHakemusOidAndKielikoeTunniste;
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

            Map<String, String> kielikoeResultsOfHakemusByTunniste = kielikoeResultsByHakemusOidAndKielikoeTunniste.get(hakemusOid);
            String hyvaksytty;
            if (kielikoeResultsOfHakemusByTunniste == null) {
                LOG.warn("Haku-app-export-JSONista ladatuista " + kielikoeResultsByHakemusOidAndKielikoeTunniste.size() +
                    " tietueesta ei löytynyt tulosta hakemukselle " + hakemusOid + " - laitetaan sille tyhjä tulos.");
                hyvaksytty = "";
            } else {
                hyvaksytty = kielikoeResultsOfHakemusByTunniste.get(tunniste);
            }
            return new AbstractPistesyottoKoosteService.SingleKielikoeTulos(tunniste, hyvaksytty, createdAt);
        }
    }
}
