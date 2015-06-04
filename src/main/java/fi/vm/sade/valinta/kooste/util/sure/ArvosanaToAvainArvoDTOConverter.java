package fi.vm.sade.valinta.kooste.util.sure;

import com.codepoetics.protonpack.StreamUtils;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.apache.commons.lang.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * @author Jussi Jartamo
 *
 * Prefiksi PK_
 *
 */
public class ArvosanaToAvainArvoDTOConverter {
    private static final Logger LOG = LoggerFactory.getLogger(ArvosanaToAvainArvoDTOConverter.class);
    private static final String SUORITUSMERKINTA = "S";
    private static final String OPPIAINE = "_OPPIAINE";
    private static final String SUORITETTU = "_SUORITETTU";
    private static final String VALINNAINEN = "_VAL";

    public static List<AvainArvoDTO> convert(List<SuoritusJaArvosanat> suoritukset, String prefix, String suffix, String oppijaOid) {
        tarkistaOppiaineetSuorituksissa(suoritukset, prefix, suffix, oppijaOid);
        return Stream.concat(
                arvosanaToAvainArvo(
                        bestArvosanaInValinnaisetArvosanat(
                                valinnaisetAineittain(suoritukset)), prefix, suffix)
                ,
                ryhmitaSamatArvosanatKeskenaan(arvosanatIlmanValinnaisiaArvosanoja(suoritukset))
                        .flatMap(arvosanat -> arvosanaToAvainArvo(bestArvosana(arvosanat), prefix, suffix)))
                .collect(Collectors.toList());
    }

    private static void tarkistaOppiaineetSuorituksissa(List<SuoritusJaArvosanat> suoritukset, String prefix, String suffix, String oppijaOid) {
        ryhmitaSamatArvosanatKeskenaan(arvosanatIlmanValinnaisiaArvosanoja(suoritukset)).forEach(
                arvosanaJoukko -> {
                    Set<String> oppiaineet =
                    arvosanaJoukko.stream().map(a -> Optional.ofNullable(a.getLisatieto()).orElse("")).collect(Collectors.toSet());
                    // Onko oppiaineita enemmän kuin yksi
                    if(oppiaineet.size() > 1) {
                        LOG.warn("Oppijalla {} oli aineelle {}{}{} useita oppiaineita {}",
                                oppijaOid,
                                prefix, arvosanaJoukko.iterator().next().getAine(), suffix,
                                Arrays.toString(oppiaineet.toArray()));
                    }
                }
        );
    }

    private static Optional<Fraction> keskiarvo(List<Arvosana> arvosanat) {
        List<Arvosana> numeeriset = filtteroiVainNumeerisetArvot(arvosanat);
        if (numeeriset.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Fraction.getFraction(
                numeeriset.stream().mapToInt(a -> Integer.parseInt(a.getArvio().getArvosana())).sum(),
                numeeriset.size()));
    }

    private static Stream<Arvosana> valinnaistenArvosanojenParasArvosanaSekvenssi(List<List<Arvosana>> vaihtoehdot) {
        return vaihtoehdot.stream()
                .sorted((v, w) -> {
                    Fraction minusOne = Fraction.ONE.negate();
                    int r = keskiarvo(w).orElse(minusOne).compareTo(keskiarvo(v).orElse(minusOne));
                    // jos keskiarvot samat, tai kumpaakaan ei voitu laskea, valitse enemmän suorituksia
                    return r == 0 ? Integer.compare(w.size(), v.size()) : r;
                })
                .findFirst().get().stream();
    }

    private static List<Arvosana> filtteroiVainNumeerisetArvot(List<Arvosana> l) {
        return l.stream()
                .filter(a -> !SUORITUSMERKINTA.equalsIgnoreCase(a.getArvio().getArvosana()))
                .collect(Collectors.toList());
    }

    private static Stream<Arvosana> bestArvosanaInValinnaisetArvosanat(
            Stream<List<List<Arvosana>>> valinnaisetArvosanat) {
        return valinnaisetArvosanat.flatMap(ArvosanaToAvainArvoDTOConverter::valinnaistenArvosanojenParasArvosanaSekvenssi);
    }

    private static Stream<List<List<Arvosana>>> valinnaisetAineittain(List<SuoritusJaArvosanat> suoritukset) {
        return suoritukset.stream()
                .flatMap(s -> s.getArvosanat().stream()
                        .filter(Arvosana::isValinnainen)
                        .collect(Collectors.groupingBy(Arvosana::getAine))
                        .values().stream().map(a -> {
                            // Normalisoi indeksin
                            StreamUtils.zipWithIndex(a.stream()
                            .sorted((a0,a1) -> a0.getJarjestys().compareTo(a1.getJarjestys())))
                            .forEach(zip -> zip.getValue().setJarjestys((int)zip.getIndex() + 1));
                            return a;
                        }))
                .collect(Collectors.groupingBy(l -> l.iterator().next().getAine()))
                .values().stream();
    }

    private static Stream<List<Arvosana>> ryhmitaSamatArvosanatKeskenaan(Stream<Arvosana> suoritukset) {
        return suoritukset
                .collect(Collectors.groupingBy(Arvosana::getAine)).values().stream();
    }
    private static Stream<Arvosana> arvosanat(Stream<SuoritusJaArvosanat> suoritukset) {
        return suoritukset
                .flatMap(s -> s.getArvosanat().stream());
    }
    private static Stream<Arvosana> arvosanatIlmanValinnaisiaArvosanoja(List<SuoritusJaArvosanat> suoritukset) {
        return arvosanat(suoritukset.stream()).filter(a -> !a.isValinnainen());
    }

    private static Stream<AvainArvoDTO> arvosanaToAvainArvo(Stream<Arvosana> arvosanat, String prefix, String suffix) {
        return arvosanat.flatMap(arvosana -> {
            AvainArvoDTO a0;
            if (arvosana.isValinnainen()) {
                a0 = new AvainArvoDTO(prefix + arvosana.getAine() + VALINNAINEN + arvosana.getJarjestys() + suffix,
                        arvosana.getArvio().getArvosana());
            } else {
                a0 = new AvainArvoDTO(prefix + arvosana.getAine() + suffix,
                        arvosana.getArvio().getArvosana());
            }
            if (SUORITUSMERKINTA.equals(a0.getArvo())) {
                a0.setAvain(a0.getAvain() + SUORITETTU);
                a0.setArvo("true");
            }
            if (arvosana.getLisatieto() != null) {
                return Stream.of(a0, new AvainArvoDTO(a0.getAvain() + OPPIAINE, arvosana.getLisatieto()));
            }
            return Stream.of(a0);
        });
    }

    private static Stream<Arvosana> bestArvosana(List<Arvosana> arvosanat) {
        TreeSet<Arvosana> arvosanaSet = new TreeSet<>((c0, c1) -> {
            if (!ofNullable(c0.getArvio().getAsteikko()).equals(ofNullable(c1.getArvio().getAsteikko()))) {
                RuntimeException asteikotEiTasmaa =
                        new RuntimeException(String.format("Asteikot ei täsmää: %s %s",
                                c0.getArvio().getAsteikko(),
                                c1.getArvio().getAsteikko()));
                LOG.error("", asteikotEiTasmaa);
                throw asteikotEiTasmaa;
            }
            if (SUORITUSMERKINTA.equals(c0.getArvio().getArvosana())) {
                return 1;
            }
            if (SUORITUSMERKINTA.equals(c1.getArvio().getArvosana())) {
                return -1;
            }
            Integer i0 = 0;
            Integer i1 = 0;
            try {
                i0 = Integer.parseInt(c0.getArvio().getArvosana());
                i1 = Integer.parseInt(c1.getArvio().getArvosana());
            } catch (NumberFormatException ignored) {}
            return i1.compareTo(i0);
        });
        arvosanaSet.addAll(arvosanat);
        return Stream.of(arvosanaSet.first());
    }
}
