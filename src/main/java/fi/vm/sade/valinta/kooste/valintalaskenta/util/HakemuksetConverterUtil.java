package fi.vm.sade.valinta.kooste.valintalaskenta.util;

import com.google.common.collect.Maps;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainSuoritustietoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.Lisapistekoulutus;
import fi.vm.sade.valintalaskenta.domain.dto.PohjakoulutusToinenAste;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.wrap;
import static java.util.stream.Collectors.*;

/**
 * @author Jussi Jartamo
 */
public class HakemuksetConverterUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HakemuksetConverterUtil.class);
    public static final String PK_PAATTOTODISTUSVUOSI = "PK_PAATTOTODISTUSVUOSI";
    public static final String POHJAKOULUTUS = "POHJAKOULUTUS";
    public static final String ENSIKERTALAINEN = "ensikertalainen";

    public static List<HakemusDTO> muodostaHakemuksetDTO(HakuV1RDTO haku,
                                                         String hakukohdeOid,
                                                         List<Hakemus> hakemukset,
                                                         List<Oppija> oppijat,
                                                         ParametritDTO parametritDTO) {
        ensurePersonOids(hakemukset, hakukohdeOid);
        List<HakemusDTO> hakemusDtot = hakemuksetToHakemusDTOs(hakukohdeOid, hakemukset);
        Map<String, Exception> errors = Maps.newHashMap();
        try {
            if (oppijat != null) {
                Map<String, Oppija> personOidToOppija = oppijat.stream()
                        .collect(toMap(Oppija::getOppijanumero, Function.<Oppija>identity()));
                hakemukset.stream().forEach(h -> {
                    try {
                        HakemusDTO hakemusDTO = Converter.hakemusToHakemusDTO(h);
                        String personOid = hakemusDTO.getHakijaOid();
                        if (personOidToOppija.containsKey(personOid)) {
                            Oppija oppija = personOidToOppija.get(personOid);
                            mergeKeysOfOppijaAndHakemus(new HakemusWrapper(h).hasHenkilotunnus(), haku, hakukohdeOid, parametritDTO, errors, oppija, hakemusDTO);
                        }
                    } catch (Exception e) {
                        errors.put(h.getOid(), e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.error("SURE arvosanojen konversiossa (hakukohde=" + hakukohdeOid + ") odottamaton virhe", e);
            throw e;
        }
        if (!errors.isEmpty()) {
            errors.entrySet().forEach(err -> {
                LOG.error(String.format("SURE arvosanojen konversiossa (hakukohde=%s, hakemus=%s) odottamaton virhe",
                                hakukohdeOid, err.getKey()),
                        err.getValue());
            });
            throw new RuntimeException(errors.entrySet().iterator().next().getValue());
        }
        return hakemusDtot;
    }

    public static void mergeKeysOfOppijaAndHakemus(boolean hakijallaOnHenkilotunnus,
                                                   HakuV1RDTO haku,
                                                   String hakukohdeOid,
                                                   ParametritDTO parametritDTO,
                                                   Map<String, Exception> errors,
                                                   Oppija oppija,
                                                   HakemusDTO hakemusDTO) {
        hakemusDTO.setAvainMetatiedotDTO(YoToAvainSuoritustietoDTOConverter.convert(oppija));
        Map<String, AvainArvoDTO> hakemuksenArvot =
                toAvainMap(hakemusDTO.getAvaimet(), hakemusDTO.getHakemusoid(), hakukohdeOid, errors);
        Map<String, AvainArvoDTO> surenArvot =
                toAvainMap(OppijaToAvainArvoDTOConverter.convert(oppija, parametritDTO),
                        hakemusDTO.getHakemusoid(),
                        hakukohdeOid,
                        errors);
        List<SuoritusJaArvosanat> suoritukset = filterUnrelevantSuoritukset(haku, oppija.getSuoritukset());
        Optional<String> pohjakoulutus = pohjakoulutus(haku, hakemusDTO, suoritukset);

        Map<String, AvainArvoDTO> merge = Maps.newHashMap();
        merge.putAll(hakemuksenArvot);
        ensikertalaisuus(hakijallaOnHenkilotunnus, haku, hakukohdeOid, oppija, hakemusDTO, merge);
        pohjakoulutus.ifPresent(pk -> merge.put(POHJAKOULUTUS, new AvainArvoDTO(POHJAKOULUTUS, pk)));
        merge.putAll(suoritustenTiedot(pohjakoulutus, hakemusDTO, suoritukset).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new AvainArvoDTO(e.getKey(), e.getValue()))));
        merge.putAll(surenArvot);
        hakemusDTO.setAvaimet(merge.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
    }

    private static void ensikertalaisuus(boolean hakijallaOnHenkilotunnus, HakuV1RDTO haku, String hakukohdeOid, Oppija oppija, HakemusDTO hakemusDTO, Map<String, AvainArvoDTO> merge) {
        // Vain korkeakouluhauille
        if (Optional.ofNullable(haku.getKohdejoukkoUri()).orElse("").startsWith("haunkohdejoukko_12")) {
            if (oppija.isEnsikertalainen() == null) {
                if(!hakijallaOnHenkilotunnus) {
                    return; // Henkilötunnuksettomilla hakijoilla ensikertalaisuuden tiedon puuttuminen on laillinen tila
                }
                LOG.error("Hakijalta {} (hakemusOid={}) puuttui ensikertalaisuustieto hakukohteen {} laskennassa.", hakemusDTO.getHakijaOid(), hakemusDTO.getHakemusoid(), hakukohdeOid);
                throw new RuntimeException("Hakijalta " + hakemusDTO.getHakijaOid() + " (hakemusOid=" + hakemusDTO.getHakemusoid() + ") puuttui ensikertalaisuustieto hakukohteen " + hakukohdeOid + " laskennassa.");
            }
            merge.put(ENSIKERTALAINEN, new AvainArvoDTO(ENSIKERTALAINEN, String.valueOf(oppija.isEnsikertalainen())));
        }
    }

    private static List<HakemusDTO> hakemuksetToHakemusDTOs(String hakukohdeOid, List<Hakemus> hakemukset) {
        List<HakemusDTO> hakemusDtot;
        Map<String, Exception> epaonnistuneetKonversiot = Maps.newConcurrentMap();
        try {
            hakemusDtot = hakemukset.parallelStream()
                    .filter(Objects::nonNull)
                    .map(h -> {
                        try {
                            return Converter.hakemusToHakemusDTO(h);
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
        if (!epaonnistuneetKonversiot.isEmpty()) {
            RuntimeException e = new RuntimeException(
                    String.format("Hakemukset to hakemusDTO mappauksessa virhe hakukohteelle %s ja hakemuksille %s. Esimerkiksi %s!",
                            hakukohdeOid,
                            Arrays.toString(epaonnistuneetKonversiot.keySet().toArray()),
                            epaonnistuneetKonversiot.values().iterator().next().getMessage()));
            LOG.error("hakemuksetToHakemusDTOs", e);
            throw e;
        }
        return hakemusDtot;
    }

    private static void ensurePersonOids(List<Hakemus> hakemukset, String hakukohdeOid) {
        final List<Hakemus> noPersonOid = hakemukset.stream()
                .filter(h -> StringUtils.isBlank(h.getPersonOid()))
                .collect(toList());
        if (!noPersonOid.isEmpty()) {
            RuntimeException e = new RuntimeException(
                    String.format("Hakukohteessa %s hakemuksilta %s puuttui personOid! Jalkikasittely ehka tekematta! Tarkista hakemusten tiedot!",
                            hakukohdeOid, Arrays.toString(noPersonOid.toArray())));
            LOG.error("ensurePersonOids", e);
            throw e;
        }
    }

    public static Map<String, AvainArvoDTO> toAvainMap(List<AvainArvoDTO> avaimet, String hakemusOid, String hakukohdeOid, Map<String, Exception> poikkeukset) {
        return Optional.ofNullable(avaimet)
                .orElse(Collections.emptyList()).stream().filter(Objects::nonNull).filter(a -> StringUtils.isNotBlank(a.getAvain()))
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

    public static List<SuoritusJaArvosanat> filterUnrelevantSuoritukset(HakuV1RDTO haku, List<SuoritusJaArvosanat> suoritukset) {
        return suoritukset.stream()
                .map(SuoritusJaArvosanatWrapper::wrap)
                .filter(s -> !(s.isPerusopetus() && !s.isVahvistettu()))
                .filter(s -> !(s.isPerusopetus() && s.isVahvistettu() && s.isKesken()))
                .filter(s -> !(s.isLisapistekoulutus() && !s.isVahvistettu()))
                .filter(s -> !(s.isPerusopetus() && s.isKeskeytynyt() && !hakukaudella(haku, s)))
                .filter(s -> !(s.isLukio() && s.isKeskeytynyt() && !hakukaudella(haku, s)))
                .filter(s -> !(s.isLisapistekoulutus() && s.isVahvistettu() && !hakukaudella(haku, s)))
                .map(SuoritusJaArvosanatWrapper::getSuoritusJaArvosanat)
                .collect(toList());
    }

    public static Optional<String> pohjakoulutus(HakuV1RDTO haku, HakemusDTO h, List<SuoritusJaArvosanat> suoritukset) {
        Optional<String> pk =  h.getAvaimet().stream()
                .filter(a -> POHJAKOULUTUS.equals(a.getAvain()))
                .map(AvainArvoDTO::getArvo)
                .findFirst();
        if (!pk.isPresent()){
            return Optional.empty();
        }
        String pohjakoulutusHakemukselta = pk.get();
        List<SuoritusJaArvosanatWrapper> suorituksetRekisterista = suoritukset.stream()
                .map(SuoritusJaArvosanatWrapper::wrap)
                .collect(toList());

        if (suorituksetRekisterista.isEmpty()) {
            return Optional.of(pohjakoulutusHakemukselta);
        }
        if (suorituksetRekisterista.stream()
                .anyMatch(s -> s.isLukio() && s.isKeskeytynyt() && hakukaudella(haku, s))) {
            LOG.error("Hakijan {} lukio keskeytynyt hakukaudella. Käytetään hakemuksen pohjakoulutusta {}.",
                    h.getHakijaOid(), pohjakoulutusHakemukselta);
            return Optional.of(pohjakoulutusHakemukselta);
        }
        if (suorituksetRekisterista.stream()
                .anyMatch(s -> (s.isLukio() && s.isValmis()) ||
                        (s.isLukio() && s.isKesken() && hakukaudella(haku, s)) ||
                        (s.isYoTutkinto() && s.isVahvistettu() && s.isValmis()))) {
            return Optional.of(PohjakoulutusToinenAste.YLIOPPILAS);
        }
        if (suorituksetRekisterista.stream()
                .anyMatch(s -> s.isPerusopetus() && s.isVahvistettu() && s.isKeskeytynyt() && hakukaudella(haku, s))) {
            return Optional.of(PohjakoulutusToinenAste.KESKEYTYNYT);
        }
        if (PohjakoulutusToinenAste.YLIOPPILAS.equals(pohjakoulutusHakemukselta)) {
            return Optional.of(PohjakoulutusToinenAste.YLIOPPILAS);
        }
        Optional<SuoritusJaArvosanatWrapper> perusopetus = suorituksetRekisterista.stream()
                .filter(s -> s.isPerusopetus() && s.isVahvistettu() && s.isValmis())
                .findFirst();
        if (perusopetus.isPresent()) {
            String yksilollistaminen = PohjakoulutusToinenAste.PERUSKOULU;
            switch (perusopetus.get().getSuoritusJaArvosanat().getSuoritus().getYksilollistaminen()) {
                case "Kokonaan":
                    yksilollistaminen = PohjakoulutusToinenAste.YKSILOLLISTETTY;
                    break;
                case "Osittain":
                    yksilollistaminen = PohjakoulutusToinenAste.OSITTAIN_YKSILOLLISTETTY;
                    break;
                case "Alueittain":
                    yksilollistaminen = PohjakoulutusToinenAste.ALUEITTAIN_YKSILOLLISTETTY;
                    break;
            }
            if (!yksilollistaminen.equals(pohjakoulutusHakemukselta)) {
                LOG.error("Hakijan {} ilmoittama perusopetus {} ei vastaa vahvistettua suoritusta {}. " +
                                "Käytetään hakemuksen pohjakoulutusta {}.",
                        h.getHakijaOid(), pohjakoulutusHakemukselta, yksilollistaminen, pohjakoulutusHakemukselta);
                return Optional.of(pohjakoulutusHakemukselta);
            } else {
                return Optional.of(yksilollistaminen);
            }
        }
        if (PohjakoulutusToinenAste.PERUSKOULU.equals(pohjakoulutusHakemukselta) &&
                suorituksetRekisterista.stream()
                        .anyMatch(s -> s.isUlkomainenKorvaava() && s.isVahvistettu() && s.isValmis())) {
            LOG.error("Hakija {} ilmoittanut peruskoulun, mutta löytyi vahvistettu ulkomainen korvaava suoritus. " +
                            "Käytetään hakemuksen pohjakoulutusta {}.",
                    h.getHakijaOid(), pohjakoulutusHakemukselta);
            return Optional.of(pohjakoulutusHakemukselta);
        }
        if (PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO.equals(pohjakoulutusHakemukselta) ||
                suorituksetRekisterista.stream()
                        .anyMatch(s -> s.isUlkomainenKorvaava() && s.isVahvistettu() && s.isValmis())) {
            return Optional.of(PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO);
        }
        return Optional.of(PohjakoulutusToinenAste.KESKEYTYNYT);
    }

    public static List<SuoritusJaArvosanat> pohjakoulutuksenSuoritukset(String pohjakoulutus,
                                                                        List<SuoritusJaArvosanat> suoritukset) {
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

    public static Map<String, String> suoritustenTiedot(Optional<String> pohjakoulutus,
                                                        HakemusDTO hakemus,
                                                        List<SuoritusJaArvosanat> suoritukset) {
        final Map<String, Predicate<SuoritusJaArvosanat>> predicates =
                new HashMap<String, Predicate<SuoritusJaArvosanat>>() {{
            put("PK", s -> wrap(s).isPerusopetus());
            put("AM", s -> wrap(s).isAmmatillinen());
            put("LK", s -> wrap(s).isLukio());
            put("YO", s -> wrap(s).isYoTutkinto());
        }};
        Map<String, String> tiedot = new HashMap<>();
        pkPaattotodistusvuosi(hakemus, suoritukset)
                .ifPresent(vuosi -> tiedot.put(PK_PAATTOTODISTUSVUOSI, String.valueOf(vuosi)));
        suoritustilat(predicates, suoritukset).entrySet().stream()
                .forEach(e -> tiedot.put(e.getKey(), String.valueOf(e.getValue())));
        suoritusajat(predicates, suoritukset).entrySet().stream()
                .forEach(e -> tiedot.put(e.getKey(), String.valueOf(e.getValue())));
        pohjakoulutus.ifPresent(pk ->
                lisapistekoulutukset(pk, hakemus, suoritukset).entrySet().stream()
                        .forEach(e -> tiedot.put(e.getKey().name(), String.valueOf(e.getValue()))));
        return tiedot;
    }

    private static Map<String, Integer> suoritusajat(Map<String, Predicate<SuoritusJaArvosanat>> predicates,
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

    private static Integer suorituskausi(DateTime valmistumisPvm) {
        if (valmistumisPvm.isBefore(SuoritusJaArvosanatWrapper.VALMISTUMIS_DTF.parseDateTime("01.08." + valmistumisPvm.getYear()))) {
            return 2;
        } else {
            return 1;
        }
    }

    private static Map<String, Boolean> suoritustilat(Map<String, Predicate<SuoritusJaArvosanat>> predicates,
                                                      List<SuoritusJaArvosanat> suoritukset) {
        return predicates.keySet().stream()
                .collect(toMap(prefix -> prefix + "_TILA",
                        prefix -> suoritukset.stream()
                                .anyMatch(s -> predicates.get(prefix).test(s) && wrap(s).isValmis() &&
                                        (wrap(s).isVahvistettu() || wrap(s).isLukio()))));
    }

    private static Map<Lisapistekoulutus, Boolean> lisapistekoulutukset(String pohjakoulutus,
                                                                        HakemusDTO hakemus,
                                                                        List<SuoritusJaArvosanat> suoritukset) {
        if (PohjakoulutusToinenAste.KESKEYTYNYT.equals(pohjakoulutus) ||
                PohjakoulutusToinenAste.YLIOPPILAS.equals(pohjakoulutus) ||
                PohjakoulutusToinenAste.ULKOMAINEN_TUTKINTO.equals(pohjakoulutus)) {
            return Arrays.stream(Lisapistekoulutus.values())
                    .collect(toMap(Function.<Lisapistekoulutus>identity(), lpk -> false));
        }
        return Arrays.stream(Lisapistekoulutus.values())
                .collect(toMap(Function.<Lisapistekoulutus>identity(),
                        lpk -> suoritukset.stream()
                                .filter(s -> lpk.komoOid.equals(s.getSuoritus().getKomo()))
                                .findFirst()
                                .map(s -> !wrap(s).isKeskeytynyt())
                                .orElse(hakemus.getAvaimet().stream()
                                        .filter(a -> lpk.name().equals(a.getAvain()))
                                        .map(a -> Boolean.valueOf(a.getArvo()))
                                        .findFirst().orElse(false))
                ));
    }

    private static Optional<Integer> pkPaattotodistusvuosi(HakemusDTO hakemus,
                                                           List<SuoritusJaArvosanat> suoritukset) {
        return Stream.concat(
                suoritukset.stream()
                        .filter(s -> wrap(s).isPerusopetus() && wrap(s).isVahvistettu() && wrap(s).isValmis())
                        .map(s -> wrap(s).getValmistuminenAsDateTime().getYear()),
                hakemus.getAvaimet().stream()
                        .filter(a -> PK_PAATTOTODISTUSVUOSI.equals(a.getAvain()))
                        .map(a -> Integer.valueOf(a.getArvo())))
                .findFirst();
    }

    private static boolean hakukaudella(HakuV1RDTO haku, SuoritusJaArvosanatWrapper s) {
        DateTime valmistuminen = s.getValmistuminenAsDateTime();
        int hakuvuosi = haku.getHakukausiVuosi();
        DateTime kStart = new DateTime().withDate(hakuvuosi, 1, 1).minusDays(1);
        DateTime kEnd = new DateTime().withDate(hakuvuosi, 7, 31).plusDays(1);
        DateTime sStart = new DateTime().withDate(hakuvuosi, 8, 1).minusDays(1);
        DateTime sEnd = new DateTime().withDate(hakuvuosi, 12, 31).plusDays(1);
        switch (haku.getHakukausiUri()) {
            case "kausi_k#1": return valmistuminen.isAfter(kStart) && valmistuminen.isBefore(kEnd);
            case "kausi_s#1": return valmistuminen.isAfter(sStart) && valmistuminen.isBefore(sEnd);
            default: throw new RuntimeException(String.format("Tuntematon hakukausi %s", haku.getHakukausiUri()));
        }
    }
}
