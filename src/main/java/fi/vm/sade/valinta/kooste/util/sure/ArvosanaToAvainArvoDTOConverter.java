package fi.vm.sade.valinta.kooste.util.sure;

import com.codepoetics.protonpack.StreamUtils;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 *         Prefiksi PK_
 */
public class ArvosanaToAvainArvoDTOConverter {
    private static final Logger LOG = LoggerFactory.getLogger(ArvosanaToAvainArvoDTOConverter.class);
    private static final String SUORITUSMERKINTA = "S";
    private static final String OPPIAINE = "_OPPIAINE";
    private static final String SUORITETTU = "_SUORITETTU";
    private static final String VALINNAINEN = "_VAL";

    public static Set<AvainArvoDTO> convert(List<SuoritusJaArvosanat> suoritukset, String prefix, String suffix, String oppijaOid) {
        tarkistaOppiaineetSuorituksissa(suoritukset, prefix, suffix, oppijaOid);
        return Stream.concat(
                aineidenValinnaisetArvosanatSuorituksittain(suoritukset.stream()).values().stream()
                        .flatMap(aineenArvosanatSuorituksittain -> parhaanSuorituksenArvosanat(aineenArvosanatSuorituksittain))
                        .flatMap(a -> arvosanaToAvainArvo(a, prefix, suffix)),
                arvosanatAineittain(varsinaisetArvosanat(suoritukset.stream())).values().stream()
                        .flatMap(aineenArvosanat -> arvosanaToAvainArvo(parasArvosana(aineenArvosanat), prefix, suffix)))
                .collect(Collectors.toSet());
    }

    private static void tarkistaOppiaineetSuorituksissa(List<SuoritusJaArvosanat> suoritukset, String prefix, String suffix, String oppijaOid) {
        arvosanatAineittain(varsinaisetArvosanat(suoritukset.stream())).values().forEach(
                arvosanaJoukko -> {
                    Set<String> oppiaineet = arvosanaJoukko.stream().map(a -> Optional.ofNullable(a.getLisatieto()).orElse("")).collect(Collectors.toSet());
                    if (oppiaineet.size() > 1) {
                        LOG.warn("Oppijalla {} oli aineelle {}{}{} useita oppiaineita {}", oppijaOid, prefix, arvosanaJoukko.iterator().next().getAine(), suffix, Arrays.toString(oppiaineet.toArray()));
                    }
                }
        );
    }

    private static Optional<Fraction> keskiarvo(List<Arvosana> arvosanat) {
        List<Integer> numeeriset = numeerisetArvosanat(arvosanat);
        if (numeeriset.isEmpty()) {
            return Optional.empty();
        }
        int summa = numeeriset.stream().mapToInt(i -> i).sum();
        return Optional.of(Fraction.getFraction(summa, numeeriset.size()));
    }

    private static Stream<Arvosana> parhaanSuorituksenArvosanat(List<List<Arvosana>> suoritustenArvosanat) {
        return suoritustenArvosanat.stream()
                .sorted((v, w) -> {
                    Fraction minusOne = Fraction.ONE.negate();
                    int r = keskiarvo(w).orElse(minusOne).compareTo(keskiarvo(v).orElse(minusOne));
                    // jos keskiarvot samat, tai kumpaakaan ei voitu laskea, valitse enemmän suorituksia
                    return r == 0 ? Integer.compare(w.size(), v.size()) : r;
                })
                .findFirst().get().stream();
    }

    private static List<Integer> numeerisetArvosanat(List<Arvosana> l) {
        return l.stream()
                .filter(a -> !SUORITUSMERKINTA.equalsIgnoreCase(a.getArvio().getArvosana()))
                .map(a -> Integer.parseInt(a.getArvio().getArvosana()))
                .collect(Collectors.toList());
    }

    private static Function<Arvosana, String> groupArvosanat(final List<Arvosana> arvosanat) {
        return a -> {
            String aine = a.getAine();
            String lisatieto = a.getLisatieto();
            // BUG-856 yhdistä kielet, esim B1 ja B12, jos samalla kielikoodilla löytyy monta arvosanaa samantasoisilla ainekoodeilla
            if (aine != null && lisatieto != null && aine.matches("[AB][123]+") && arvosanat.stream().filter(arvosana ->
                    left(aine, 2).equals(left(arvosana.getAine(), 2)) &&
                            StringUtils.equals(lisatieto, arvosana.getLisatieto())).collect(Collectors.toList()).size() > 1) {
                return left(defaultString(a.getAine()), 2);
            } else {
                return a.getAine();
            }
        };
    }

    private static Map<String, List<List<Arvosana>>> aineidenValinnaisetArvosanatSuorituksittain(Stream<SuoritusJaArvosanat> suoritukset) {
        return suoritukset
                .flatMap(s -> {
                    Map<String, List<Arvosana>> aineittain = arvosanatAineittain(valinnaisetArvosanat(s));
                    aineittain.values().forEach(aineenArvosanat -> normalisoiJarjestys(aineenArvosanat));
                    return aineittain.entrySet().stream();
                })
                .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.mapping(e -> e.getValue(), Collectors.toList())));
    }

    private static void normalisoiJarjestys(List<Arvosana> arvosanat) {
        StreamUtils.zipWithIndex(arvosanat.stream().sorted((a0, a1) -> a0.getJarjestys().compareTo(a1.getJarjestys())))
                .forEach(zip -> zip.getValue().setJarjestys((int) zip.getIndex() + 1));
    }

    private static Map<String, List<Arvosana>> arvosanatAineittain(Stream<Arvosana> arvosanatS) {
        List<Arvosana> arvosanat = arvosanatS.collect(Collectors.toList());
        return arvosanat.stream().collect(Collectors.groupingBy(groupArvosanat(arvosanat)));
    }

    private static Stream<Arvosana> varsinaisetArvosanat(Stream<SuoritusJaArvosanat> suoritukset) {
        return suoritukset.flatMap(s -> s.getArvosanat().stream()).filter(a -> !a.isValinnainen());
    }

    private static Stream<Arvosana> valinnaisetArvosanat(SuoritusJaArvosanat suoritus) {
        return suoritus.getArvosanat().stream().filter(Arvosana::isValinnainen);
    }

    private static Stream<AvainArvoDTO> arvosanaToAvainArvo(Arvosana arvosana, String prefix, String suffix) {
        AvainArvoDTO a;
        if (arvosana.isValinnainen()) {
            a = new AvainArvoDTO(prefix + arvosana.getAine() + VALINNAINEN + arvosana.getJarjestys() + suffix, arvosana.getArvio().getArvosana());
        } else {
            a = new AvainArvoDTO(prefix + arvosana.getAine() + suffix, arvosana.getArvio().getArvosana());
        }
        if (SUORITUSMERKINTA.equals(a.getArvo())) {
            a.setAvain(a.getAvain() + SUORITETTU);
            a.setArvo("true");
        }
        if (arvosana.getLisatieto() != null) {
            return Stream.of(a, new AvainArvoDTO(prefix + arvosana.getAine() + suffix + OPPIAINE, arvosana.getLisatieto()));
        }
        return Stream.of(a);
    }

    private static Arvosana parasArvosana(List<Arvosana> arvosanat) {
        return arvosanat.stream().sorted((c0, c1) -> {
            varmistaYhteensopivatAsteikot(c0, c1);
            if (SUORITUSMERKINTA.equals(c0.getArvio().getArvosana())) {
                return 1;
            }
            if (SUORITUSMERKINTA.equals(c1.getArvio().getArvosana())) {
                return -1;
            }
            Integer i0 = Integer.parseInt(c0.getArvio().getArvosana());
            Integer i1 = Integer.parseInt(c1.getArvio().getArvosana());
            return i1.compareTo(i0);
        }).findFirst().get();
    }

    private static void varmistaYhteensopivatAsteikot(Arvosana c0, Arvosana c1) {
        if (!StringUtils.equals(c0.getArvio().getAsteikko(), c1.getArvio().getAsteikko())) {
            String msg = String.format("Asteikot ei täsmää: %s %s", c0.getArvio().getAsteikko(), c1.getArvio().getAsteikko());
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
    }
}
