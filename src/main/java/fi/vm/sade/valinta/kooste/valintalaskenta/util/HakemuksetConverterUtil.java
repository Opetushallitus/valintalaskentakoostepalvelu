package fi.vm.sade.valinta.kooste.valintalaskenta.util;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.wrap;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import com.google.common.collect.Maps;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainSuoritustietoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.Lisapistekoulutus;
import fi.vm.sade.valintalaskenta.domain.dto.PohjakoulutusToinenAste;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("HakemuksetConverterUtil")
public class HakemuksetConverterUtil {
    public static final String PK_PAATTOTODISTUSVUOSI = "PK_PAATTOTODISTUSVUOSI";
    public static final String LK_PAATTOTODISTUSVUOSI = "lukioPaattotodistusVuosi";
    public static final String PERUSOPETUS_KIELI = "perusopetuksen_kieli";
    public static final String LUKIO_KIELI = "lukion_kieli";
    public static final String POHJAKOULUTUS = "POHJAKOULUTUS";
    public static final String ENSIKERTALAINEN = "ensikertalainen";
    public static final String PREFERENCE_PREFIX = "preference";
    public static final String DISCRETIONARY_POSTFIX = "discretionary";
    public static final String KOHDEJOUKKO_AMMATILLINEN_JA_LUKIO = "haunkohdejoukko_11";

    private static final Logger LOG = LoggerFactory.getLogger(HakemuksetConverterUtil.class);

    private final LocalDateTime abienPohjaKoulutusPaattelyLeikkuriPvm;

    public HakemuksetConverterUtil(
            @Value("${valintalaskentakoostepalvelu.abi.pohjakoulutus.paattely.leikkuripvm:2020-06-01}") String abienPohjaKoulutusPaattelyLeikkuriPvm) {
        this.abienPohjaKoulutusPaattelyLeikkuriPvm = LocalDate.parse(abienPohjaKoulutusPaattelyLeikkuriPvm).atStartOfDay();
    }

    private void tryToMergeKeysOfOppijaAndHakemus(HakuV1RDTO haku, String hakukohdeOid, ParametritDTO parametritDTO, Boolean fetchEnsikertalaisuus, Map<String, Exception> errors, Map<String, Oppija> personOidToOppija, Map<String, Boolean> hasHetu, HakemusDTO h) {
        try {
            String personOid = h.getHakijaOid();
            if (personOidToOppija.containsKey(personOid)) {
                Oppija oppija = personOidToOppija.get(personOid);
                mergeKeysOfOppijaAndHakemus(hasHetu.get(h.getHakemusoid()), haku, hakukohdeOid, parametritDTO, errors, oppija, h, fetchEnsikertalaisuus);
            } else {
                LOG.warn(String.format("BUG-2034 : Oppijatietoa ei löytynyt oppijanumerolla %s.", personOid));
            }
        } catch (Exception e) {
            errors.put(h.getHakemusoid(), e);
        }
    }

    public List<HakemusDTO> muodostaHakemuksetDTOfromHakemukset(HakuV1RDTO haku, String hakukohdeOid,
                                                                       Map<String, List<String>> hakukohdeRyhmasForHakukohdes,
                                                                       List<HakemusWrapper> hakemukset,
                                                                       List<Valintapisteet> valintapisteet,
                                                                       List<Oppija> oppijat,
                                                                       ParametritDTO parametritDTO, Boolean fetchEnsikertalaisuus) {
        ensurePersonOids(hakemukset, hakukohdeOid);
        List<HakemusDTO> hakemusDtot = hakemuksetToHakemusDTOs(hakukohdeOid, hakemukset, ofNullable(valintapisteet).orElse(emptyList()), hakukohdeRyhmasForHakukohdes);
        Map<String, Boolean> hasHetu = hakemukset.stream().collect(toMap(HakemusWrapper::getOid, HakemusWrapper::hasHenkilotunnus));
        Map<String, Exception> errors = Maps.newHashMap();
        return getHakemusDTOS(haku, hakukohdeOid, oppijat, parametritDTO, fetchEnsikertalaisuus, hakemusDtot, hasHetu, errors);
    }

    private List<HakemusDTO> getHakemusDTOS(HakuV1RDTO haku,
                                                   String hakukohdeOid,
                                                   List<Oppija> oppijat,
                                                   ParametritDTO parametritDTO,
                                                   Boolean fetchEnsikertalaisuus,
                                                   List<HakemusDTO> hakemusDtot,
                                                   Map<String, Boolean> hasHetu,
                                                   Map<String, Exception> errors) {
        try {
            if (oppijat != null) {
                LOG.info(String.format("Got %d oppijat is in getHakemusDTOS for haku %s (\"%s\"), hakukohde %s for %d applications.",
                        oppijat.size(), haku.getOid(), haku.getNimi(), hakukohdeOid, hakemusDtot.size()));
                Map<String, Oppija> personOidToOppija = oppijat.stream().collect(toMap(Oppija::getOppijanumero, Function.identity()));
                hakemusDtot.forEach(h -> tryToMergeKeysOfOppijaAndHakemus(haku, hakukohdeOid, parametritDTO, fetchEnsikertalaisuus, errors, personOidToOppija, hasHetu, h));
            } else {
                LOG.warn(String.format("oppijat is null when calling getHakemusDTOS for haku %s (\"%s\"), hakukohde %s for %d applications.",
                        haku.getOid(), haku.getNimi(), hakukohdeOid, hakemusDtot.size()));
            }
        } catch (Exception e) {
            LOG.error("SURE arvosanojen konversiossa (hakukohde=" + hakukohdeOid + ") odottamaton virhe", e);
            throw e;
        }
        if (!errors.isEmpty()) {
            errors.forEach((key, value) -> LOG.error(String.format("SURE arvosanojen konversiossa (hakukohde=%s, hakemus=%s) odottamaton virhe", hakukohdeOid, key), value));
            throw new RuntimeException(errors.entrySet().iterator().next().getValue());
        }
        return hakemusDtot;
    }

    public void mergeKeysOfOppijaAndHakemus(boolean hakijallaOnHenkilotunnus, HakuV1RDTO haku, String hakukohdeOid,
                                                   ParametritDTO parametritDTO, Map<String, Exception> errors, Oppija oppija,
                                                   HakemusDTO hakemusDTO, Boolean fetchEnsikertalaisuus) {
        hakemusDTO.setAvainMetatiedotDTO(YoToAvainSuoritustietoDTOConverter.convert(oppija));
        Map<String, AvainArvoDTO> hakemuksenArvot = toAvainMap(hakemusDTO.getAvaimet(), hakemusDTO.getHakemusoid(), hakukohdeOid, errors);
        Map<String, AvainArvoDTO> surenArvosanat = toAvainMap(OppijaToAvainArvoDTOConverter.convert(oppija.getOppijanumero(), oppija.getSuoritukset(), hakemusDTO, parametritDTO), hakemusDTO.getHakemusoid(), hakukohdeOid, errors);
        Map<String, AvainArvoDTO> ammatillisenKielikokeetSuresta = toAvainMap(AmmatillisenKielikoetuloksetSurestaConverter.convert(oppija.getSuoritukset(), parametritDTO, hakemusDTO), hakemusDTO.getHakemusoid(), hakukohdeOid, errors);

        Map<String, AvainArvoDTO> merge = Maps.newHashMap();
        merge.putAll(hakemuksenArvot);
        if (fetchEnsikertalaisuus)
            ensikertalaisuus(hakijallaOnHenkilotunnus, haku, hakukohdeOid, oppija, hakemusDTO, merge);
        merge.putAll(suoritustenTiedot(haku, hakemusDTO, oppija.getSuoritukset()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new AvainArvoDTO(e.getKey(), e.getValue()))));
        merge.putAll(surenArvosanat);
        merge.putAll(ammatillisenKielikokeetSuresta);
        hakemusDTO.setAvaimet(merge.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
    }

    private void ensikertalaisuus(boolean hakijallaOnHenkilotunnus, HakuV1RDTO haku, String hakukohdeOid, Oppija oppija, HakemusDTO hakemusDTO, Map<String, AvainArvoDTO> merge) {
        // Vain korkeakouluhauille
        if (ofNullable(haku.getKohdejoukkoUri()).filter(u -> u.startsWith("haunkohdejoukko_12")).isPresent()) {
            if (oppija.isEnsikertalainen() == null) {
                if (!hakijallaOnHenkilotunnus) {
                    return; // Henkilötunnuksettomilla hakijoilla ensikertalaisuuden tiedon puuttuminen on laillinen tila
                }
                LOG.error("Hakijalta {} (hakemusOid={}) puuttui ensikertalaisuustieto hakukohteen {} laskennassa.", hakemusDTO.getHakijaOid(), hakemusDTO.getHakemusoid(), hakukohdeOid);
                throw new RuntimeException("Hakijalta " + hakemusDTO.getHakijaOid() + " (hakemusOid=" + hakemusDTO.getHakemusoid() + ") puuttui ensikertalaisuustieto hakukohteen " + hakukohdeOid + " laskennassa.");
            }
            merge.put(ENSIKERTALAINEN, new AvainArvoDTO(ENSIKERTALAINEN, String.valueOf(oppija.isEnsikertalainen())));
        }
    }

    private List<HakemusDTO> hakemuksetToHakemusDTOs(String hakukohdeOid, List<HakemusWrapper> hakemukset, List<Valintapisteet> valintapisteet, Map<String, List<String>> hakukohdeRyhmasForHakukohdes) {
        List<HakemusDTO> hakemusDtot;
        Map<String, Valintapisteet> hakemusOIDtoValintapisteet = valintapisteet.stream().collect(Collectors.toMap(Valintapisteet::getHakemusOID, v -> v));
        Map<String, Exception> epaonnistuneetKonversiot = Maps.newConcurrentMap();
        hakemusDtot = getHakemusDTOS(hakukohdeOid, hakemukset, hakukohdeRyhmasForHakukohdes, hakemusOIDtoValintapisteet, epaonnistuneetKonversiot);
        if (!epaonnistuneetKonversiot.isEmpty()) {
            RuntimeException e = new RuntimeException(
                    String.format("Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle %s ja hakemuksille %s. Esimerkiksi %s!",
                            hakukohdeOid, Arrays.toString(epaonnistuneetKonversiot.keySet().toArray()), epaonnistuneetKonversiot.values().iterator().next().getMessage()));
            LOG.error("hakemuksetToHakemusDTOs", e);
            throw e;
        }
        return hakemusDtot;
    }

    private List<HakemusDTO> getHakemusDTOS(String hakukohdeOid, List<HakemusWrapper> hakemukset, Map<String, List<String>> hakukohdeRyhmasForHakukohdes, Map<String, Valintapisteet> hakemusOIDtoValintapisteet, Map<String, Exception> epaonnistuneetKonversiot) {
        List<HakemusDTO> hakemusDtot;
        try {
            hakemusDtot = hakemukset.parallelStream()
                    .filter(Objects::nonNull)
                    .map(h -> {
                        try {
                            return h.toHakemusDto(hakemusOIDtoValintapisteet.get(h.getOid()), hakukohdeRyhmasForHakukohdes); // TODO there maybe come null pisteet here
                        } catch (Exception e) {
                            epaonnistuneetKonversiot.put(h.getOid(), e);
                            return null;
                        }
                    })
                    .collect(toList());
        } catch (Exception e) {
            LOG.error(String.format("Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle %s ja null hakemukselle.", hakukohdeOid), e);
            throw e;
        }
        return hakemusDtot;
    }

    private void ensurePersonOids(List<HakemusWrapper> hakemukset, String hakukohdeOid) {
        final List<HakemusWrapper> noPersonOid = hakemukset.stream()
                .filter(h -> StringUtils.isBlank(h.getPersonOid()))
                .collect(toList());
        if (!noPersonOid.isEmpty()) {
            String hakemusOids = noPersonOid.stream().map(HakemusWrapper::getOid).collect(Collectors.joining(", "));
            RuntimeException e = new RuntimeException(
                    String.format("Hakukohteessa %s hakemuksilta %s puuttui personOid! Jalkikasittely ehka tekematta! Tarkista hakemusten tiedot!",
                            hakukohdeOid, hakemusOids));
            LOG.error("ensurePersonOids", e);
            throw e;
        }
    }

    public Map<String, AvainArvoDTO> toAvainMap(List<AvainArvoDTO> avaimet, String hakemusOid, String hakukohdeOid, Map<String, Exception> poikkeukset) {
        return ofNullable(avaimet)
                .orElse(emptyList()).stream().filter(Objects::nonNull).filter(a -> StringUtils.isNotBlank(a.getAvain()))
                .collect(groupingBy(a -> a.getAvain(), mapping(a -> a, toList())))
                .entrySet().stream()
                .map(a -> {
                    if (a.getValue().size() != 1) {
                        RuntimeException e = new RuntimeException(
                                String.format("Duplikaattiavain (avain=%s) hakemuksella tai suoritusrekisterin arvosanassa (hakemusOid=%s) hakukohteessa (hakukohdeOid=%s). Jos kyseessä on osakokeen tunnus niin tarkista ettei samalla suorituksella/arvosanalla ole osakoeduplikaatteja.",
                                        a.getKey(), hakemusOid, hakukohdeOid));
                        LOG.error("toAvainMap", e);
                        poikkeukset.put(hakemusOid, e);
                    }
                    return a.getValue().iterator().next();
                })
                .collect(toMap(a -> a.getAvain(), a -> a));
    }

    public List<SuoritusJaArvosanat> filterUnrelevantSuoritukset(HakuV1RDTO haku, HakemusDTO hakemus, List<SuoritusJaArvosanat> suoritukset) {
        return suoritukset.stream()
                .map(SuoritusJaArvosanatWrapper::wrap)
                .filter(s -> !(s.isSuoritusMistaSyntyyPeruskoulunArvosanoja() && !s.isVahvistettu() && !s.onTaltaHakemukselta(hakemus)))
                .filter(s -> !(s.isSuoritusMistaSyntyyPeruskoulunArvosanoja() && s.isVahvistettu() && !hakukaudella(haku, s)))
                .filter(s -> !(s.isPerusopetus() && s.isKeskeytynyt() && !hakukaudella(haku, s)))
                .filter(s -> !(s.isLukio() && !s.isVahvistettu() && !s.onTaltaHakemukselta(hakemus)))
                .filter(s -> !(s.isLukio() && s.isKeskeytynyt()))
                .filter(s -> !(s.isYoTutkinto() && (s.isKesken() || s.isKeskeytynyt())))
                .map(SuoritusJaArvosanatWrapper::getSuoritusJaArvosanat)
                .collect(toList());
    }

    public Optional<String> pohjakoulutus(HakuV1RDTO haku, HakemusDTO hakemusDTO, List<SuoritusJaArvosanat> suoritukset) {
        Optional<String> pk = hakemusDTO.getAvaimet().stream()
                .filter(a -> POHJAKOULUTUS.equals(a.getAvain()))
                .map(AvainArvoDTO::getArvo)
                .findFirst();
        if (!pk.isPresent()) {
            return empty();
        }
        String pohjakoulutusHakemukselta = pk.get();
        List<SuoritusJaArvosanatWrapper> suorituksetRekisterista = suoritukset.stream()
                .map(SuoritusJaArvosanatWrapper::wrap)
                .collect(toList());

        if (suorituksetRekisterista.stream()
                .anyMatch(s -> (s.isLukio() && s.isValmis()) ||
                        (s.isLukio() && s.isKesken() && hakukaudella(haku, s)) ||
                        (s.isYoTutkinto() && s.isVahvistettu() && s.isValmis()))) {
            return of(PohjakoulutusToinenAste.YLIOPPILAS);
        }
        Predicate<SuoritusJaArvosanatWrapper> vahvistettuKeskeytynytPerusopetus = s -> s.isPerusopetus() && s.isVahvistettu() && s.isKeskeytynyt();
        if (suorituksetRekisterista.stream().anyMatch(vahvistettuKeskeytynytPerusopetus) &&
            suorituksetRekisterista.stream().filter(SuoritusJaArvosanatWrapper::isPerusopetus).allMatch(vahvistettuKeskeytynytPerusopetus)) {
            return of(PohjakoulutusToinenAste.KESKEYTYNYT);
        }
        if (PohjakoulutusToinenAste.YLIOPPILAS.equals(pohjakoulutusHakemukselta)) {
            if (LocalDateTime.now().isBefore(abienPohjaKoulutusPaattelyLeikkuriPvm)
                    || !isHakijaAbiturientti(haku, hakemusDTO)) {
                return of(PohjakoulutusToinenAste.YLIOPPILAS);
            }
            LOG.warn("Hakemuksella {} pohjakoulutus lukio, mutta valmista ja vahvistettua lukiosuoritusta ei löydy suoritusrekisteristä. Palautetaan pohjakoulutus PERUSKOULU.", hakemusDTO.getHakemusoid());
            return of(PohjakoulutusToinenAste.PERUSKOULU);
        }
        Optional<SuoritusJaArvosanatWrapper> perusopetus = suorituksetRekisterista.stream()
                .filter(s -> s.isPerusopetus() && s.isVahvistettu() && !s.isKeskeytynyt())
                .findFirst();
        if (perusopetus.isPresent()) {
            return of(paattelePerusopetuksenPohjakoulutus(perusopetus.get()));
        }
        Optional<SuoritusJaArvosanatWrapper> perusopetusVahvistamaton = suorituksetRekisterista.stream()
                .filter(s -> s.isPerusopetus() && !s.isVahvistettu())
                .findFirst();
        if (perusopetusVahvistamaton.isPresent()) {
            return of(paattelePerusopetuksenPohjakoulutus(perusopetusVahvistamaton.get()));
        }
        if (PohjakoulutusToinenAste.PERUSKOULU.equals(pohjakoulutusHakemukselta) &&
                suorituksetRekisterista.stream().anyMatch(s -> s.isUlkomainenKorvaava() && s.isVahvistettu() && s.isValmis())) {
            LOG.warn("Hakija {} ilmoittanut peruskoulun, mutta löytyi vahvistettu ulkomainen korvaava suoritus. " + "Käytetään hakemuksen pohjakoulutusta {}.",
                    hakemusDTO.getHakijaOid(), pohjakoulutusHakemukselta);
            return of(pohjakoulutusHakemukselta);
        }
        if (PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO.equals(pohjakoulutusHakemukselta) ||
                suorituksetRekisterista.stream().anyMatch(s -> s.isUlkomainenKorvaava() && s.isVahvistettu() && s.isValmis())) {
            return of(PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO);
        }
        LOG.warn("Hakijan {} pohjakoulutusta ei voitu päätellä, käytetään hakemuksen pohjakoulutusta {}.",
                hakemusDTO.getHakijaOid(), pohjakoulutusHakemukselta);
        return of(pohjakoulutusHakemukselta);
    }

    private boolean isHakijaAbiturientti(HakuV1RDTO haku, HakemusDTO hakemusDTO) {
        return hakemusDTO.getAvaimet().stream().anyMatch(
                dto -> LK_PAATTOTODISTUSVUOSI.equals(dto.getAvain())
                        && Integer.toString(haku.getHakukausiVuosi()).equals(dto.getArvo()));
    }

    private String paattelePerusopetuksenPohjakoulutus(SuoritusJaArvosanatWrapper perusopetus) {
        switch (perusopetus.getSuoritusJaArvosanat().getSuoritus().getYksilollistaminen()) {
            case "Kokonaan":
                return PohjakoulutusToinenAste.YKSILOLLISTETTY;
            case "Osittain":
                return PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY;
            case "Alueittain":
                return PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY;
            default:
                return PohjakoulutusToinenAste.PERUSKOULU;
        }
    }

    public List<SuoritusJaArvosanat> pohjakoulutuksenSuoritukset(String pohjakoulutus, List<SuoritusJaArvosanat> suoritukset) {
        if (pohjakoulutus.equals(PohjakoulutusToinenAste.YLIOPPILAS)) {
            return suoritukset.stream()
                    .filter(s -> wrap(s).isLukio() || wrap(s).isYoTutkinto())
                    .collect(toList());
        }
        if (pohjakoulutus.equals(PohjakoulutusToinenAste.PERUSKOULU) ||
                pohjakoulutus.equals(PohjakoulutusToinenAste.YKSILOLLISTETTY) ||
                pohjakoulutus.equals(PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY) ||
                pohjakoulutus.equals(PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY)) {
            return suoritukset.stream()
                    .filter(s -> wrap(s).isSuoritusMistaSyntyyPeruskoulunArvosanoja())
                    .collect(toList());
        }
        if (pohjakoulutus.equals(PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO)) {
            return suoritukset.stream()
                    .filter(s -> wrap(s).isUlkomainenKorvaava())
                    .collect(toList());
        }
        throw new RuntimeException(String.format("Tuntematon pohjakoulutus %s", pohjakoulutus));
    }

    public Map<String, String> suoritustenTiedot(HakuV1RDTO haku,
                                                        HakemusDTO hakemus,
                                                        List<SuoritusJaArvosanat> sureSuoritukset) {
        final Map<String, Predicate<SuoritusJaArvosanat>> predicates =
                new HashMap<String, Predicate<SuoritusJaArvosanat>>() {{
                    put("PK", s -> wrap(s).isPerusopetus());
                    put("AM", s -> wrap(s).isAmmatillinen());
                    put("LK", s -> wrap(s).isLukio());
                    put("YO", s -> wrap(s).isYoTutkinto());
                }};
        final Map<String, String> tiedot = new HashMap<>();
        final List<SuoritusJaArvosanat> suoritukset = filterUnrelevantSuoritukset(haku, hakemus, sureSuoritukset);
        sort(suoritukset);

        Optional<String> pohjakoulutus = pohjakoulutus(haku, hakemus, suoritukset);
        pohjakoulutus.ifPresent(pk -> tiedot.put(POHJAKOULUTUS, pk));
        pkPaattotodistusvuosi(hakemus, suoritukset).ifPresent(vuosi -> tiedot.put(PK_PAATTOTODISTUSVUOSI, String.valueOf(vuosi)));
        pkOpetuskieli(hakemus, suoritukset).ifPresent(kieli -> tiedot.put(PERUSOPETUS_KIELI, kieli));
        lukioOpetuskieli(hakemus, suoritukset).ifPresent(kieli -> tiedot.put(LUKIO_KIELI, kieli));
        pohjakoulutus.ifPresent(pk -> tiedot.putAll(automaticDiscretionaryOptions(pk, haku, hakemus)));
        suoritustilat(predicates, suoritukset).entrySet().stream().forEach(e -> tiedot.put(e.getKey(), String.valueOf(e.getValue())));
        suoritusajat(predicates, suoritukset).entrySet().stream().forEach(e -> tiedot.put(e.getKey(), String.valueOf(e.getValue())));
        pohjakoulutus.ifPresent(pk -> lisapistekoulutukset(pk, haku, hakemus, suoritukset).entrySet().stream().forEach(e -> tiedot.put(e.getKey().name(), String.valueOf(e.getValue()))));
        return tiedot;
    }

    private boolean isDiscretionaryInUse(HakuV1RDTO haku) {
        return KOHDEJOUKKO_AMMATILLINEN_JA_LUKIO.equals(haku.getKohdejoukkoUri().split("#")[0]);
    }

    private Map<String, String> automaticDiscretionaryOptions(String pohjakoulutus, HakuV1RDTO haku, HakemusDTO hakemus) {
        Map<String, String> tiedot = new HashMap<>();
        if (isDiscretionaryInUse(haku) &&
            (PohjakoulutusToinenAste.KESKEYTYNYT.equals(pohjakoulutus) ||
            PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO.equals(pohjakoulutus))) {
            for (int preferenceIndex = 1; preferenceIndex <= hakemus.getHakukohteet().size();  preferenceIndex++) {
                String discretionaryQuestionId = PREFERENCE_PREFIX + preferenceIndex + "-" + DISCRETIONARY_POSTFIX;
                tiedot.put(discretionaryQuestionId, "true");
                tiedot.put(discretionaryQuestionId + "-follow-up", "todistustenpuuttuminen");
            }
        }
        return tiedot;
    }

    private Map<String, Integer> suoritusajat(Map<String, Predicate<SuoritusJaArvosanat>> predicates,
                                                     List<SuoritusJaArvosanat> suoritukset) {
        Map<String, Integer> vuodet = new HashMap<>();
        predicates.entrySet().stream()
                .forEach(e -> {
                    String prefix = e.getKey();
                    Predicate<SuoritusJaArvosanat> p = e.getValue();
                    suoritukset.stream()
                            .filter(s -> p.test(s) && wrap(s).isValmis())
                            .map(s -> wrap(s).getValmistuminenAsDateTime())
                            .findFirst()
                            .ifPresent(date -> {
                                vuodet.put(prefix + "_SUORITUSVUOSI", date.getYear());
                                vuodet.put(prefix + "_SUORITUSLUKUKAUSI", suorituskausi(date));
                            });
                });
        return vuodet;
    }

    private Integer suorituskausi(DateTime valmistumisPvm) {
        if (valmistumisPvm.isBefore(SuoritusJaArvosanatWrapper.VALMISTUMIS_DTF.parseDateTime("01.08." + valmistumisPvm.getYear()))) {
            return 2;
        } else {
            return 1;
        }
    }

    private Map<String, Boolean> suoritustilat(Map<String, Predicate<SuoritusJaArvosanat>> predicates, List<SuoritusJaArvosanat> suoritukset) {
        return predicates.keySet().stream()
                .collect(toMap(prefix -> prefix + "_TILA", prefix -> suoritukset.stream()
                                .anyMatch(s -> predicates.get(prefix).test(s) && wrap(s).isValmis() && (wrap(s).isVahvistettu() || wrap(s).isLukio()))));
    }

    private Map<Lisapistekoulutus, Boolean> lisapistekoulutukset(String pohjakoulutus, HakuV1RDTO haku, HakemusDTO hakemus, List<SuoritusJaArvosanat> suoritukset) {
        if (PohjakoulutusToinenAste.KESKEYTYNYT.equals(pohjakoulutus) ||
                PohjakoulutusToinenAste.YLIOPPILAS.equals(pohjakoulutus) ||
                PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO.equals(pohjakoulutus)) {
            return Arrays.stream(Lisapistekoulutus.values())
                    .collect(toMap(Function.identity(), lpk -> false));
        }
        return Arrays.stream(Lisapistekoulutus.values())
                .collect(toMap(Function.identity(),
                        lpk -> suoritukset.stream()
                                .filter(s -> lpk.komoOid.equals(s.getSuoritus().getKomo()))
                                .filter(s -> !(wrap(s).isKeskeytynyt() && !hakukaudella(haku, wrap(s))))
                                .findFirst()
                                .map(s -> !wrap(s).isKeskeytynyt())
                                .orElse(hakemus.getAvaimet().stream()
                                        .filter(a -> lpk.name().equals(a.getAvain()))
                                        .map(a -> Boolean.valueOf(a.getArvo()))
                                        .findFirst().orElse(false))
                ));
    }

    private Optional<Integer> pkPaattotodistusvuosi(HakemusDTO hakemus, List<SuoritusJaArvosanat> suoritukset) {
        return Stream.concat(
                suoritukset.stream()
                        .filter(s -> wrap(s).isPerusopetus() && (wrap(s).isValmis() || wrap(s).isKesken()))
                        .map(s -> wrap(s).getValmistuminenAsDateTime().getYear()),
                hakemus.getAvaimet().stream()
                        .filter(a -> PK_PAATTOTODISTUSVUOSI.equals(a.getAvain()))
                        .map(a -> Integer.valueOf(a.getArvo())))
                .findFirst();
    }

    private Optional<String> pkOpetuskieli(HakemusDTO hakemus, List<SuoritusJaArvosanat> suoritukset) {
        return Stream.concat(
                suoritukset.stream()
                        .filter(s -> wrap(s).isPerusopetus() && (wrap(s).isValmis() || wrap(s).isKesken()))
                        .map(s -> wrap(s).getSuoritusJaArvosanat().getSuoritus().getSuoritusKieli())
                        .filter(s -> !StringUtils.isEmpty(s)),
                hakemus.getAvaimet().stream()
                        .filter(a -> PERUSOPETUS_KIELI.equals(a.getAvain()))
                        .map(a -> a.getArvo()))
                .findFirst();
    }

    private Optional<String> lukioOpetuskieli(HakemusDTO hakemus, List<SuoritusJaArvosanat> suoritukset) {
        return Stream.concat(
                suoritukset.stream()
                        .filter(s -> wrap(s).isLukio() && (wrap(s).isValmis() || wrap(s).isKesken()))
                        .map(s -> wrap(s).getSuoritusJaArvosanat().getSuoritus().getSuoritusKieli())
                        .filter(s -> !StringUtils.isEmpty(s)),
                hakemus.getAvaimet().stream()
                        .filter(a -> LUKIO_KIELI.equals(a.getAvain()))
                        .map(a -> a.getArvo()))
                .findFirst();
    }

    private boolean hakukaudella(HakuV1RDTO haku, SuoritusJaArvosanatWrapper s) {
        DateTime valmistuminen = s.getValmistuminenAsDateTime();
        int hakuvuosi = haku.getHakukausiVuosi();
        DateTime kStart = new DateTime(hakuvuosi, 1, 1, 0, 0).minus(1);
        DateTime kEnd = new DateTime(hakuvuosi, 7, 31, 0, 0).plusDays(1);
        DateTime sStart = new DateTime(hakuvuosi, 8, 1, 0, 0).minus(1);
        DateTime sEnd = new DateTime(hakuvuosi, 12, 31, 0, 0).plusDays(1);
        switch (haku.getHakukausiUri()) {
            case "kausi_k#1":
                return valmistuminen.isAfter(kStart) && valmistuminen.isBefore(kEnd);
            case "kausi_s#1":
                return valmistuminen.isAfter(sStart) && valmistuminen.isBefore(sEnd);
            default:
                throw new RuntimeException(String.format("Tuntematon hakukausi %s", haku.getHakukausiUri()));
        }
    }
}
