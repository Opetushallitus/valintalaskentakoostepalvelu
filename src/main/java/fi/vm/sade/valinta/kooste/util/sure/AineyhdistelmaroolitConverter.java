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

    private static final Predicate<ArvosanaJaAvainArvo> PAKOLLINEN = arvo -> !arvo.getArvosana().isValinnainen();
    private static final Predicate<ArvosanaJaAvainArvo> YLIMAARAINEN = arvo -> arvo.getArvosana().isValinnainen();
    private static final Predicate<ArvosanaJaAvainArvo> AIDINKIELI = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isAidinkieli();
    private static final Predicate<ArvosanaJaAvainArvo> AIDINKIELI_JA_EI_ENGLANTI = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isAidinkieli()
            &&
            // NOT ENGLANTI
            !"J".equals(arvo.getArvosana().getAine());
    private static final Predicate<ArvosanaJaAvainArvo> SUOMI_RUOTSI_TOISENA_KIELENA = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isSuomiRuotsiToisenaKielena();
    private static final Predicate<ArvosanaJaAvainArvo> TOINEN_KOTIMAINEN = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isToinenKotimainenKieli();
    private static final Predicate<ArvosanaJaAvainArvo> VIERASKIELI = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isVieraskieli();
    private static final Predicate<ArvosanaJaAvainArvo> MATEMATIIKKA = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isMatematiikka();
    private static final Predicate<ArvosanaJaAvainArvo> REAALI = arvo -> new ArvosanaWrapper(arvo.getArvosana()).isReaali();

    public static Stream<ArvosanaJaAvainArvo> konvertoi(List<ArvosanaJaAvainArvo> mukaanTulevatArvot) {
        final List<ArvosanaJaAvainArvo> ylimaaraiset = mukaanTulevatArvot.stream().filter(YLIMAARAINEN).collect(Collectors.toList());
        final List<ArvosanaJaAvainArvo> pakolliset = mukaanTulevatArvot.stream().filter(PAKOLLINEN).collect(Collectors.toList());


        Stream<ArvosanaJaAvainArvo> aidinkielenRooli =
                mukaanTulevatArvot.stream()
                        .filter(AIDINKIELI)
                        .flatMap(
                                a -> {
                                    if (PAKOLLINEN.test(a)) {
                                        AvainArvoDTO aa = new AvainArvoDTO();
                                        aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                        //LOG.error("AIDINKIELI {}", new ArvosanaWrapper(a.getArvosana()).isSaame());
                                        if (new ArvosanaWrapper(a.getArvosana()).isSaame()) {
                                            aa.setArvo("12");
                                        } else if (new ArvosanaWrapper(a.getArvosana()).isEnglanti()) {
                                            aa.setArvo("13"); // KYPSYYSNÄYTE
                                        } else {
                                            aa.setArvo("11");
                                        }
                                        return Stream.of(new ArvosanaJaAvainArvo(a.getArvosana(), aa));
                                    } else {
                                        if (new ArvosanaWrapper(a.getArvosana()).isEnglanti()) {
                                            return Stream.empty();
                                        } else {
                                            AvainArvoDTO aa = new AvainArvoDTO();
                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                            aa.setArvo("60");
                                            return Stream.of(new ArvosanaJaAvainArvo(a.getArvosana(), aa));
                                        }
                                    }
                                    });

                                    Stream<ArvosanaJaAvainArvo> toinenAidinkielenRooli =
                                            mukaanTulevatArvot.stream()
                                                    //
                                                    .filter(SUOMI_RUOTSI_TOISENA_KIELENA)
                                                    .map(a -> {
                                                        AvainArvoDTO aa = new AvainArvoDTO();
                                                        aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                        aa.setArvo("14");
                                                        return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                    });


                                    Stream<ArvosanaJaAvainArvo> matematiikka =
                                            mukaanTulevatArvot.stream()
                                                    //
                                                    .filter(MATEMATIIKKA)
                                                    .map(a -> {
                                                        if (PAKOLLINEN.test(a)) {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("42"); // Pakollinen matematiikka
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        } else {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("81"); // Ylimääräinen matematiikka
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        }
                                                    });

                                    Stream<ArvosanaJaAvainArvo> reaali =
                                            mukaanTulevatArvot.stream()
                                                    //
                                                    .filter(REAALI)
                                                    .map(a -> {
                                                        if (PAKOLLINEN.test(a)) {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("41"); // Pakollinen reaali
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        } else {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("71"); // Ylimääräinen reaali
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        }
                                                    });

                                    Stream<ArvosanaJaAvainArvo> toinenKotimainen =
                                            mukaanTulevatArvot.stream()
                                                    //
                                                    .filter(TOINEN_KOTIMAINEN)
                                                    .map(a -> {
                                                        if (PAKOLLINEN.test(a)) {

                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("22"); // Pakollinen toinen kotimainen
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        } else {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("62"); // Ylimääräinen toinen kotimainen
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        }
                                                    });

                                    Stream<ArvosanaJaAvainArvo> vieraskieli =
                                            mukaanTulevatArvot.stream()
                                                    //
                                                    .filter(VIERASKIELI)
                                                    .map(a -> {
                                                        if (PAKOLLINEN.test(a)) {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("31"); // Pakollinen vieraskieli
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        } else {
                                                            AvainArvoDTO aa = new AvainArvoDTO();
                                                            aa.setAvain(a.getArvosana().getAine() + "_ROOLI");
                                                            aa.setArvo("61"); // Ylimääräinen vieraskieli
                                                            return new ArvosanaJaAvainArvo(a.getArvosana(), aa);
                                                        }
                                                    });

                                    return Stream.of(toinenKotimainen, vieraskieli, matematiikka, reaali, aidinkielenRooli, toinenAidinkielenRooli).flatMap(a -> a);

                                }
    }
