package fi.vm.sade.valinta.kooste.util.sure;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valintalaskenta.domain.dto.AvainMetatiedotDTO;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.*;

public class YoToAvainSuoritustietoDTOConverter {
    private static final Logger LOG = LoggerFactory.getLogger(YoToAvainSuoritustietoDTOConverter.class);
    private static final String OSAKOE_ASTEIKKO = "OSAKOE";
    private static final Set<String> OSAKOETUNNUS_WHITELIST = Collections.unmodifiableSet(Sets.newHashSet(Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10")));

    private static void lisaaSuorituspvm(Arvosana a, Map<String, String> x) {
        /*
        PK_SUORITUSLUKUKAUSI = 1/2
        1.1. - 31.7. ->  2
        1.8. -> 31.12. -> 1
                */
        //SUORITUSVUOSI: 2012,
        //SUORITUSLUKUKAUSI: 1,
        if (!StringUtils.isBlank(a.getMyonnetty())) {
            DateTime dt = new ArvosanaWrapper(a).getMyonnettyAsDateTime();
            x.put("SUORITUSVUOSI", "" + dt.getYear());
            LocalDate ld = new LocalDate(dt.getYear(), 8, 1);
            DateTime dt0 = ld.toDateTime(LocalTime.MIDNIGHT);
            if (!(dt.isEqual(dt0) || dt.isAfter(dt0))) {
                x.put("SUORITUSLUKUKAUSI", "1");
            } else {
                x.put("SUORITUSLUKUKAUSI", "2");
            }
        }
    }

    private static final Function<Arvosana, Map<String, String>> mapArvosana = a -> {
        Map<String, String> x = Maps.newHashMap();
        Optional.ofNullable(a.getArvio().getPisteet()).ifPresent(pisteet -> {
            x.put("PISTEET", "" + pisteet);
        });
        x.put("ARVO", "" + a.getArvio().getArvosana());
        a.getAineyhdistelmarooli().ifPresent(rooli -> {
            x.put("ROOLI", "" + rooli);
        });
        lisaaSuorituspvm(a, x);
        return x;
    };
    private static final Function<Arvosana, Map<String, String>> mapArvosanaWithLisatieto = a -> {
        Map<String, String> x = Maps.newHashMap();
        x.put("LISATIETO", "" + a.getLisatieto());
        Optional.ofNullable(a.getArvio().getPisteet()).ifPresent(pisteet -> {
            x.put("PISTEET", "" + pisteet);
        });
        x.put("ARVO", "" + a.getArvio().getArvosana());
        lisaaSuorituspvm(a, x);
        a.getAineyhdistelmarooli().ifPresent(rooli -> {
            x.put("ROOLI", "" + rooli);
        });
        return x;
    };

    public static List<AvainMetatiedotDTO> convert(Oppija oppija) {
        if (oppija == null || oppija.getSuoritukset() == null) {
            return Collections.emptyList();
        }
        List<SuoritusJaArvosanat> yoSuoritukset = oppija.getSuoritukset().stream().filter(s ->
                new SuoritusJaArvosanatWrapper(s).isYoTutkinto() && !new SuoritusJaArvosanatWrapper(s).isItseIlmoitettu()).collect(Collectors.toList());
        if (yoSuoritukset.isEmpty()) {
            return Collections.emptyList();
        }
        List<Arvosana> yoArvosanat = yoSuoritukset.iterator().next().getArvosanat().stream().flatMap(a -> normalisoi(a)).collect(Collectors.toList());
        Map<String, List<Arvosana>> arvosanaGrouping = yoArvosanat.stream().collect(Collectors.groupingBy(a -> ((Arvosana) a).getAine(), Collectors.mapping(identity(), Collectors.toList())));
        return Stream.of(
                // arvosanat
                arvosanaGrouping.entrySet().stream().map(
                        a -> new AvainMetatiedotDTO(a.getKey(), a.getValue().stream().map(mapArvosana).collect(Collectors.toList()))),
                // ainereaali
                arvosanaGroup("AINEREAALI", yoArvosanat.stream().filter(find(
                        "UE", "UO", "ET", "FF", "PS",
                        "HI", "FY", "KE", "BI", "GE",
                        "TE", "YH"))),
                arvosanaGroup("REAALI", yoArvosanat.stream().filter(find(
                        "RR", "RO", "RY"))),
                arvosanaGroup("PITKA_KIELI", yoArvosanat.stream().filter(find(
                        "EA", "FA", "GA", "HA", "PA",
                        "SA", "TA", "VA", "S9"))),
                arvosanaGroup("KESKIPITKA_KIELI", yoArvosanat.stream().filter(find(
                        "EB", "FB", "GB", "HB", "PB", "SB", "TB", "VB"))),
                arvosanaGroup("LYHYT_KIELI", yoArvosanat.stream().filter(find(
                        "EC", "FC", "GC", "L1", "PC",
                        "SC", "TC", "VC", "KC", "L7"))),
                arvosanaGroup("AIDINKIELI", yoArvosanat.stream().filter(find(
                        "O", "A", "I", "W", "Z", "O5",
                        "A5")))).flatMap(identity()).collect(Collectors.toList());
    }

    private static Predicate<Arvosana> find(String... arvosana) {
        final Set<String> aa = Sets.newHashSet(arvosana);
        return a -> aa.contains(a.getAine());
    }

    private static Stream<AvainMetatiedotDTO> arvosanaGroup(String group, Stream<Arvosana> arvosanat) {
        List<Arvosana> ar = arvosanat.collect(Collectors.toList());
        if (ar.isEmpty()) {
            return Stream.empty();
        } else {
            return Stream.of(new AvainMetatiedotDTO(group, ar.stream().map(mapArvosanaWithLisatieto).collect(Collectors.toList())));
        }
    }

    private static Stream<Arvosana> normalisoi(Arvosana a) {
        if (OSAKOE_ASTEIKKO.equals(a.getArvio().getAsteikko())) {
            String osakoe = a.getAine().split("_")[1];
            if (!OSAKOETUNNUS_WHITELIST.contains(osakoe)) {
                return Stream.empty(); // ei olla kiinnostuttu koodista
            }
            return Stream.of(new Arvosana(a.getId(), a.getSuoritus(),
                    osakoe, a.getValinnainen(), a.getMyonnetty(), a.getSource(),
                    a.getLahdeArvot(), a.getArvio(), a.getLisatieto()));
        } else {
            return Stream.of(new Arvosana(a.getId(), a.getSuoritus(),
                    aineMapper(a), a.getValinnainen(), a.getMyonnetty(), a.getSource(),
                    a.getLahdeArvot(), a.getArvio(), a.getLisatieto()));
        }
    }

    private static String aineMapper(Arvosana a) {
        return a.getKoetunnus().map(YoToAvainSuoritustietoDTOConverter::koetunnusMapper).orElseGet(() -> {
            String aine = aineMapper(a.getAine(), a.getLisatieto());
            LOG.warn("No koetunnus in YO arvosana: mapped aine '" + a.getAine() + "' and lisatieto '" + a.getLisatieto() + "' to aine " + aine);
            return aine;
        });
    }

    private static String koetunnusMapper(String koetunnus) {
        switch (koetunnus) {
            case "E1":
                return "EA";
            case "E2":
                return "EB";
            case "F1":
                return "FA";
            case "F2":
                return "FB";
            case "G1":
                return "GA";
            case "G2":
                return "GB";
            case "H1":
                return "HA";
            case "H2":
                return "HB";
            case "P1":
                return "PA";
            case "P2":
                return "PB";
            case "S1":
                return "SA";
            case "S2":
                return "SB";
            case "T1":
                return "TA";
            case "T2":
                return "TB";
            case "V1":
                return "VA";
            case "V2":
                return "VB";
            default:
                return koetunnus;
        }
    }

    private static String aineMapper(String aine, String lisatieto) {
        if (aine.equals("AINEREAALI")) {
            return lisatieto;
        } else if (aine.equals("REAALI")) { // Vanha reaali
            switch (lisatieto) {
                case "ET":
                    return "RY";
                case "UO":
                    return "RO";
                case "UE":
                    return "RR";
                default:
                    return "REAALI_" + lisatieto;
            }

        } else if (aine.equals("A")) { // Pitkät kielit
            switch (lisatieto) {
                case "SA":
                    return "SA";
                case "EN":
                    return "EA";
                case "UN":
                    return "HA";
                case "VE":
                    return "VA";
                case "IT":
                    return "TA";
                case "RA":
                    return "FA";
                case "FI":
                    return "CA";
                case "RU":
                    return "BA";
                case "ES":
                    return "PA";
                case "PG":
                    return "GA";
                default:
                    return "A_" + lisatieto;
            }
        } else if (aine.equals("B")) { // Keskipitkät kielet
            switch (lisatieto) {
                case "SA":
                    return "SB";
                case "EN":
                    return "EB";
                case "UN":
                    return "HB";
                case "VE":
                    return "VB";
                case "IT":
                    return "TB";
                case "RA":
                    return "FB";
                case "FI":
                    return "CB";
                case "RU":
                    return "BB";
                case "ES":
                    return "PB";
                case "PG":
                    return "GB";
                default:
                    return "B_" + lisatieto;
            }
        } else if (aine.equals("C")) { // Lyhyet kielet
            switch (lisatieto) {
                case "SA":
                    return "SC";
                case "EN":
                    return "EC";
                case "UN":
                    return "HC";
                case "VE":
                    return "VC";
                case "IT":
                    return "TC";
                case "RA":
                    return "FC";
                case "FI":
                    return "CC";
                case "RU":
                    return "BC";
                case "ES":
                    return "PC";
                case "PG":
                    return "GC";
                case "IS":
                    return "IC";
                case "ZA":
                    return "DC";
                case "KR":
                    return "KC";
                case "QS":
                    return "QC";
                case "LA":
                    return "L7";
                default:
                    return "C_" + lisatieto;
            }
        } else if (aine.equals("AI")) { // Äidinkielet
            switch (lisatieto) {
                case "IS":
                    return "I";
                case "RU":
                    return "O";
                case "FI":
                    return "A";
                case "ZA":
                    return "Z";
                case "QS":
                    return "W";
                default:
                    return "AI_" + lisatieto;
            }
        } else if (aine.equals("VI2")) { // Suomi/Ruotsi toisena kielenä
            switch (lisatieto) {
                case "RU":
                    return "O5";
                case "FI":
                    return "A5";
                default:
                    return "VI2_" + lisatieto;
            }
        } else if (aine.equals("PITKA")) { // Pitkä matematiikka
            switch (lisatieto) {
                case "MA":
                    return "M";
                default:
                    return "PITKA_" + lisatieto;
            }
        } else if (aine.equals("LYHYT")) { // Lyhyt matematiikka
            switch (lisatieto) {
                case "MA":
                    return "N";
                default:
                    return "LYHYT_" + lisatieto;
            }
        } else if (aine.equals("SAKSALKOUL")) { // Saksalaisen koulun saksan kielen koe
            switch (lisatieto) {
                case "SA":
                    return "S9";
                default:
                    return "SAKSALKOUL_" + lisatieto;
            }
        } else if (aine.equals("KYPSYYS")) { // Englanninkielinen kypsyyskoe
            switch (lisatieto) {
                case "EN":
                    return "J";
                default:
                    return "KYPSYYS_" + lisatieto;
            }
        } else if (aine.equals("D")) { // Latina
            switch (lisatieto) {
                case "LA":
                    return "L1";
                default:
                    return "D_" + lisatieto;
            }
        } else {
            return "XXX";
        }
    }
}
