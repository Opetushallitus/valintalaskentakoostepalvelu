package fi.vm.sade.valinta.kooste.util.sure;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.ArvosanaWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jussi Jartamo
 */
public class AineyhdistelmaroolitConverter {

    private static final Logger LOG = LoggerFactory.getLogger(AineyhdistelmaroolitConverter.class);

    private static final Predicate<Arvosana> PAKOLLINEN = arvo -> !arvo.isValinnainen();
    private static final Predicate<Arvosana> YLIMAARAINEN = arvo -> arvo.isValinnainen();
    private static final Predicate<Arvosana> AIDINKIELI = arvo -> new ArvosanaWrapper(arvo).isAidinkieli();
    private static final Predicate<ArvosanaJaAvainArvo> AIDINKIELI_JA_EI_ENGLANTI = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isAidinkieli()
            &&
            // NOT ENGLANTI
            !"J".equals(arvo.getArvosana().getAine());
    private static final Predicate<Arvosana> SUOMI_RUOTSI_TOISENA_KIELENA = arvo -> new ArvosanaWrapper(arvo).isSuomiRuotsiToisenaKielena();
    private static final Predicate<Arvosana> TOINEN_KOTIMAINEN = arvo -> new ArvosanaWrapper(arvo).isToinenKotimainenKieli();
    private static final Predicate<Arvosana> VIERASKIELI = arvo -> new ArvosanaWrapper(arvo).isVieraskieli();
    private static final Predicate<Arvosana> MATEMATIIKKA = arvo -> new ArvosanaWrapper(arvo).isMatematiikka();
    private static final Predicate<Arvosana> REAALI = arvo -> new ArvosanaWrapper(arvo).isReaali();

    public static Optional<String> rooliFromArvosana(Arvosana arvosana) {
        return Stream.of(
                Stream.of(arvosana).filter(AIDINKIELI).map(a -> {
                    if (PAKOLLINEN.test(a)) {
                        if (new ArvosanaWrapper(a).isSaame()) {
                            return "12";
                        } else if (new ArvosanaWrapper(a).isEnglanti()) {
                            return "13"; // KYPSYYSNÄYTE
                        } else {
                            return "11";
                        }
                    } else {
                        if (new ArvosanaWrapper(a).isEnglanti()) {
                            return ""; //!!!
                        } else {
                            return "60";
                        }
                    }
                }),
                Stream.of(arvosana).filter(SUOMI_RUOTSI_TOISENA_KIELENA).map(a -> "14"),
                Stream.of(arvosana).filter(MATEMATIIKKA)
                        .map(a -> {
                        if (PAKOLLINEN.test(a)) {
                            return "42";// Pakollinen matematiikka
                        } else {
                            return "81";// Ylimääräinen matematiikka
                        }
                }),
                Stream.of(arvosana).filter(REAALI)
                        .map(a -> {
                            if (PAKOLLINEN.test(a)) {
                                return "41";// Pakollinen reaali
                            } else {
                                return "71";// Ylimääräinen reaali
                            }
                        }),
                Stream.of(arvosana).filter(TOINEN_KOTIMAINEN)
                        .map(a -> {
                            if (PAKOLLINEN.test(a)) {
                                return "22";// Pakollinen tk
                            } else {
                                return "62";// Ylimääräinen tk
                            }
                        }),
                Stream.of(arvosana).filter(VIERASKIELI)
                        .map(a -> {
                            if (PAKOLLINEN.test(a)) {
                                return "31";// Pakollinen vk
                            } else {
                                return "61";// Ylimääräinen vk
                            }
                        })
                        ).flatMap(Function.identity()).findAny();
    }

}
