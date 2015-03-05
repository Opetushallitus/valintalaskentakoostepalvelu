package fi.vm.sade.valinta.kooste.util.sure;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.ArvosanaWrapper;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jussi Jartamo
 *
 * Prefiksiton
 *
 */
public class YoToAvainArvoDTOConverter {
    private static final Logger LOG = LoggerFactory.getLogger(YoToAvainArvoDTOConverter.class);
    private static final String OSAKOE_ASTEIKKO = "OSAKOE";
    private static final String YO_ASTEIKKO = "YO";
    private static final String PREFIKSI = "YO_";
    private static final Set<String> OSAKOETUNNUS_WHITELIST =
            Collections.unmodifiableSet(Sets.newHashSet(Arrays.asList("01","02","03","04","05","06","07","08","09","10")));
    private static final List<String> YO_ORDER = Collections
            .unmodifiableList(Arrays.asList("L", "E", "M", "C", "B", "A", "I"));

    private static Stream<AvainArvoDTO> suorituksenTila(SuoritusJaArvosanat suoritus) {
        AvainArvoDTO a = new AvainArvoDTO();
        a.setAvain(new StringBuilder("YO_").append("TILA").toString());
        if(new SuoritusJaArvosanatWrapper(suoritus).isValmis()) {
            a.setArvo("true");
        } else {
            a.setArvo("false");
        }
        if(suoritus.getSuoritus().getValmistuminen() != null) {
            DateTime valmistumisPvm = new SuoritusJaArvosanatWrapper(suoritus).getValmistuminenAsDateTime();

            AvainArvoDTO as = new AvainArvoDTO();
            as.setAvain(new StringBuilder(PREFIKSI).append("SUORITUSVUOSI").toString());
            as.setArvo(""+valmistumisPvm.getYear());

            AvainArvoDTO asl = new AvainArvoDTO();
            asl.setAvain(new StringBuilder(PREFIKSI).append("SUORITUSLUKUKAUSI").toString());
            if(valmistumisPvm.isBefore(SuoritusJaArvosanatWrapper.VALMISTUMIS_DTF.parseDateTime("01.08." + valmistumisPvm.getYear()))) {
                asl.setArvo("2");
            } else {
                asl.setArvo("1");
            }
            return Stream.of(a,as,asl);
        }
        return Stream.of(a);
    }

    private static Arvosana normalisoi(Arvosana a) {
        if(OSAKOE_ASTEIKKO.equals(a.getArvio().getAsteikko())) {
            return new Arvosana(a.getId(), a.getSuoritus(),
                    aineMapper(a.getAine().split("_")[0], a.getLisatieto()), a.getValinnainen(), a.getMyonnetty(), a.getSource(),
                    a.getArvio(), a.getLisatieto());
        } else {
            return new Arvosana(a.getId(), a.getSuoritus(),
                    aineMapper(a.getAine(), a.getLisatieto()), a.getValinnainen(), a.getMyonnetty(), a.getSource(),
                    a.getArvio(), a.getLisatieto());
        }
    }
    public static Stream<AvainArvoDTO> convert(
            Optional<SuoritusJaArvosanat> suoritusOption) {
        SuoritusJaArvosanat yoSuoritus = suoritusOption.orElse(null);
        if(yoSuoritus == null) {
            return Stream.empty();
        }


        Stream<ArvosanaJaAvainArvo> osakoeArvosanat = Stream.of(yoSuoritus)
                //

                //
                .flatMap(s -> s.getArvosanat().stream())
                        //
                .filter(a -> a.getAine() != null && a.getArvio() != null
                                && a.getArvio().getArvosana() != null
                                && OSAKOE_ASTEIKKO.equals(a.getArvio().getAsteikko())
                                && a.getAine().split("_").length == 2
                                && OSAKOETUNNUS_WHITELIST.contains(a.getAine().split("_")[1])
                )
                //
                .map(a -> {
                    Arvosana normalisoituArvosana = normalisoi(a);
                    AvainArvoDTO a0 = new AvainArvoDTO();
                    a0.setArvo(a.getArvio().getArvosana());
                    String[] avainJaOsakoetunnus = a.getAine().split("_");
                    a0.setAvain(avainJaOsakoetunnus[1]);
                    return new ArvosanaJaAvainArvo(normalisoituArvosana.getAine(), normalisoituArvosana, a0);
                });

        Map<String, Arvosana> yoArvosanatMap = Stream.of(yoSuoritus)
                        //

                        //
                .flatMap(s -> s.getArvosanat().stream())
                        //
                .filter(a -> a.getAine() != null && a.getArvio() != null
                                && a.getArvio().getArvosana() != null
                                && YO_ASTEIKKO.equals(a.getArvio().getAsteikko())
                )
                        //
                .map(a -> normalisoi(a))
                        //
                .collect(Collectors.toMap(Arvosana::getAine, a -> a,
                        (s, a) -> max(Arrays.asList(s, a))));
        if(yoArvosanatMap.isEmpty()) {
            return suorituksenTila(yoSuoritus);
        } else {
            List<Arvosana> yoArvosanat = new ArrayList<>(yoArvosanatMap.values());

            // AINEREAALI = max(UE, UO, ET, FF, PS, HI, FY, KE, BI, GE, TE, YH)
            // <br>
            // REAALI = max(RR, RO, RY)
            // <br>
            // PITKA_KIELI = max(EA, FA, GA, HA, PA, SA, TA, VA)
            // <br>
            // KESKIPITKA_KIELI = max(EB, FB, GB, HB, PB, SB, TB, VB)
            // <br>
            // LYHYT_KIELI = max(EC, FC, GC, L1, PC, SC, TC, VC, KC)
            // <br>
            // AIDINKIELI = max(O, A, I, W, Z, O5, A5)
            Stream<AvainArvoDTO> aaa = Stream.concat(osakoeArvosanat,Stream.of(
                    convert("AINEREAALI",
                            max(find(yoArvosanat, "UE", "UO", "ET", "FF", "PS",
                                    "HI", "FY", "KE", "BI", "GE", "TE", "YH"))),
                    //
                    convert("REAALI", max(find(yoArvosanat, "RR", "RO", "RY"))),
                    //
                    convert("PITKA_KIELI",
                            max(find(yoArvosanat, "EA", "FA", "GA", "HA", "PA",
                                    "SA", "TA", "VA", "S9"))),
                    //
                    convert("KESKIPITKA_KIELI",
                            max(find(yoArvosanat, "EB", "FB", "GB", "HB", "PB",
                                    "SB", "TB", "VB"))),
                    //
                    convert("LYHYT_KIELI",
                            max(find(yoArvosanat, "EC", "FC", "GC", "L1", "PC",
                                    "SC", "TC", "VC", "KC", "L7"))),
                    //
                    convert("AIDINKIELI",
                            max(find(yoArvosanat, "O", "A", "I", "W", "Z", "O5",
                                    "A5"))),
                    yoArvosanat.stream()
                            .flatMap(a -> convert(a))).flatMap(a -> a))
                    // Poista ylimaaraiset osakokeet (myonnetty eri paivana kuin mukaan otettu YO-arvosana)
                    .collect(Collectors.groupingBy(a -> a.avain,
                            Collectors.mapping(a -> a, Collectors.toList()))).entrySet().stream()
                    .flatMap(a -> {
                                LOG.error("{}", a.getValue().stream().map(x -> x.avain + ":" + x.getAvainArvoDTO().getAvain()).collect(Collectors.joining(",")));
                                if (a.getValue().size() == 1) {
                                // yksi avain joten palautetaan se jos yo-arvosana
                                    return a.getValue().stream().filter(x0 -> YO_ASTEIKKO.equals(x0.getArvosana().getArvio().getAsteikko())).map(x0 -> x0.avainArvoDTO);
                                } else {

                                    Map<DateTime, List<ArvosanaJaAvainArvo>> myonnettyPvmMappingToArvosana =
                                    a.getValue().stream().collect(Collectors.groupingBy(x -> ArvosanaWrapper.ARVOSANA_DTF.parseDateTime(x.arvosana.getMyonnetty()),
                                            Collectors.mapping(x -> x, Collectors.toList()))).entrySet().stream()
                                            // filteroidaan pelkat osakoetunnukset pois (ilman yo-arvosanaa) näitä ei kyllä pitäisi olla
                                            .filter(x -> x.getValue().stream()
                                                    // Onko edes yksi YO-arvosana
                                                    .filter(x0 -> YO_ASTEIKKO.equals(x0.getArvosana().getArvio().getAsteikko())).findFirst().isPresent())
                                                    .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

                                    if(myonnettyPvmMappingToArvosana.size() > 1) {
                                        // duplikaatti yo-arvosanoja
                                        LOG.error("Duplikaatti YO-arvosanoja! {}");
                                    }
                                    Stream<AvainArvoDTO> s =
                                    myonnettyPvmMappingToArvosana.entrySet().stream().flatMap(x -> x.getValue().stream().map(x0 -> x0.getAvainArvoDTO()));
                                    return s;
                                }                            }
                    );
            /*
            List<AvainArvoDTO> avaimet = Lists.newArrayList(yoArvosanat.stream()
                    .flatMap(a -> convert(a)).collect(Collectors.toList()));
            */
            //avaimet.addAll(aaa);

            return Stream.concat(suorituksenTila(yoSuoritus), aaa.filter(Objects::nonNull));
        }
    }

    public static Arvosana max(List<Arvosana> arvosanat) {
        return arvosanat
                .stream()
                .reduce((a, b) -> {
                    // JOS SAMAT ARVOSANAT NIIN PISTEISSA SUUREMPI PALAUTETAAN
                    if (a.getArvio().getArvosana().equals(b.getArvio().getArvosana())) {
                        if (Optional.ofNullable(a.getArvio().getPisteet()).orElse(-1).compareTo(
                                Optional.ofNullable(b.getArvio().getPisteet()).orElse(-1)) == 1) {
                            return a;
                        } else {
                            return b;
                        }
                    }
                    // SUUREMPI ARVOSANA PALAUTETAAN
                    if (YO_ORDER.indexOf(a.getArvio().getArvosana()) < YO_ORDER
                            .indexOf(b.getArvio().getArvosana())) {
                        return a;
                    } else {
                        return b;
                    }
                }).orElse(null);
    }

    private static List<Arvosana> find(List<Arvosana> arvosanat,
                                       String... aineet) {
        Set<String> kohteet = Sets.newHashSet(aineet);
        return arvosanat.stream().filter(a -> kohteet.contains(a.getAine()))
                .collect(Collectors.toList());
    }

    private static Stream<ArvosanaJaAvainArvo> convert(String avain, Arvosana arvosana) {
        if (arvosana == null) {
            return Stream.empty();
        }
        AvainArvoDTO aa = new AvainArvoDTO();
        aa.setArvo(arvosana.getArvio().getArvosana());
        aa.setAvain(avain);
        if(arvosana.getArvio().getPisteet() == null) {
            return Stream.of(new ArvosanaJaAvainArvo(arvosana,aa));
        }else {
            AvainArvoDTO aaPisteet = new AvainArvoDTO();
            aaPisteet.setArvo("" + arvosana.getArvio().getPisteet());
            aaPisteet.setAvain(avain + "_PISTEET");
            return Stream.of(new ArvosanaJaAvainArvo(arvosana,aa), new ArvosanaJaAvainArvo(arvosana,aaPisteet));
        }
    }

    private static Stream<ArvosanaJaAvainArvo> convert(Arvosana arvosana) {
        AvainArvoDTO aa = new AvainArvoDTO();
        aa.setArvo(arvosana.getArvio().getArvosana());
        aa.setAvain(arvosana.getAine());
        if(arvosana.getArvio().getPisteet() == null) {
            return Stream.of(new ArvosanaJaAvainArvo(arvosana.getAine(),arvosana,aa));
        }
        AvainArvoDTO aaPisteet = new AvainArvoDTO();
        aaPisteet.setArvo("" + arvosana.getArvio().getPisteet());
        aaPisteet.setAvain(arvosana.getAine() + "_PISTEET");
        return Stream.of(new ArvosanaJaAvainArvo(arvosana.getAine(),arvosana,aa), new ArvosanaJaAvainArvo(arvosana,aaPisteet));
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
                    return "REAALI_"+lisatieto;
            }

        } else if(aine.equals("A")) { // Pitkät kielit
            switch (lisatieto) {
                case "SA": return "SA";
                case "EN": return "EA";
                case "UN": return "HA";
                case "VE": return "VA";
                case "IT": return "TA";
                case "RA": return "FA";
                case "FI": return "CA";
                case "RU": return "BA";
                case "ES": return "PA";
                case "PG": return "GA";
                default: return "A_"+lisatieto;
            }
        } else if(aine.equals("B")) { // Keskipitkät kielet
            switch (lisatieto) {
                case "SA": return "SB";
                case "EN": return "EB";
                case "UN": return "HB";
                case "VE": return "VB";
                case "IT": return "TB";
                case "RA": return "FB";
                case "FI": return "CB";
                case "RU": return "BB";
                case "ES": return "PB";
                case "PG": return "GB";
                default: return "B_"+lisatieto;
            }
        } else if(aine.equals("C")) { // Lyhyet kielet
            switch (lisatieto) {
                case "SA": return "SC";
                case "EN": return "EC";
                case "UN": return "HC";
                case "VE": return "VC";
                case "IT": return "TC";
                case "RA": return "FC";
                case "FI": return "CC";
                case "RU": return "BC";
                case "ES": return "PC";
                case "PG": return "GC";
                case "IS": return "IC";
                case "ZA": return "DC";
                case "KR": return "KC";
                case "QS": return "QC";
                case "LA": return "L7";
                default: return "C_"+lisatieto;
            }
        } else if(aine.equals("AI")) { // Äidinkielet
            switch (lisatieto) {
                case "IS": return "I";
                case "RU": return "O";
                case "FI": return "A";
                case "ZA": return "Z";
                case "QS": return "W";
                default: return "AI_"+lisatieto;
            }
        } else if(aine.equals("VI2")) { // Suomi/Ruotsi toisena kielenä
            switch (lisatieto) {
                case "RU": return "O5";
                case "FI": return "A5";
                default: return "VI2_"+lisatieto;
            }
        } else if(aine.equals("PITKA")) { // Pitkä matematiikka
            switch (lisatieto) {
                case "MA": return "M";
                default: return "PITKA_"+lisatieto;
            }
        } else if(aine.equals("LYHYT")) { // Lyhyt matematiikka
            switch (lisatieto) {
                case "MA": return "N";
                default: return "LYHYT_"+lisatieto;
            }
        } else if(aine.equals("SAKSALKOUL")) { // Saksalaisen koulun saksan kielen koe
            switch (lisatieto) {
                case "SA": return "S9";
                default: return "SAKSALKOUL_"+lisatieto;
            }
        } else if(aine.equals("KYPSYYS")) { // Englanninkielinen kypsyyskoe
            switch (lisatieto) {
                case "EN": return "J";
                default: return "KYPSYYS_"+lisatieto;
            }
        } else if(aine.equals("D")) { // Latina
            switch (lisatieto) {
                case "LA": return "L1";
                default: return "D_"+lisatieto;
            }
        } else {
            return "XXX";
        }

    }
    private static class ArvosanaJaAvainArvo implements Comparable<ArvosanaJaAvainArvo> {
        private final Arvosana arvosana;
        private final AvainArvoDTO avainArvoDTO;
        private final String avain;

        public ArvosanaJaAvainArvo(String avain, Arvosana arvosana, AvainArvoDTO avainArvoDTO) {
            this.arvosana = arvosana;
            this.avainArvoDTO = avainArvoDTO;
            this.avain = avain;
        }
        public ArvosanaJaAvainArvo(Arvosana arvosana, AvainArvoDTO avainArvoDTO) {
            this.arvosana = arvosana;
            this.avainArvoDTO = avainArvoDTO;
            this.avain = avainArvoDTO.getAvain();
        }
        @Override
        public boolean equals(Object obj) {
            if(avain == null || obj == null || !(obj instanceof ArvosanaJaAvainArvo)) {
                return false;
            } else {
                return avain.equals(((ArvosanaJaAvainArvo)obj).avain);
            }

        }

        @Override
        public int compareTo(ArvosanaJaAvainArvo o) {
            return avain.compareTo(o.avain);
        }

        public Arvosana getArvosana() {
            return arvosana;
        }

        public AvainArvoDTO getAvainArvoDTO() {
            return avainArvoDTO;
        }
    }
}
