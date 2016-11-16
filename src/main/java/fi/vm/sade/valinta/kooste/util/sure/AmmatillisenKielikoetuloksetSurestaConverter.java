package fi.vm.sade.valinta.kooste.util.sure;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.wrap;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.ei_osallistunut;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;

import fi.vm.sade.service.valintaperusteet.dto.model.Osallistuminen;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmmatillisenKielikoetuloksetSurestaConverter {
    public static final String SURE_ASTEIKKO_HYVAKSYTTY = "HYVAKSYTTY";

    public static List<AvainArvoDTO> convert(List<SuoritusJaArvosanat> oppijanSuorituksetJaArvosanat, ParametritDTO parametritDTO) {
        if (oppijanSuorituksetJaArvosanat == null) {
            return Collections.emptyList();
        }

        Stream<SuoritusJaArvosanat> suorituksetEnnenLaskennanAlkamistaMyonnettyjenArvosanojenKanssa =
            OppijaToAvainArvoDTOConverter.removeLaskennanAlkamisenJalkeenMyonnetytArvosanat(oppijanSuorituksetJaArvosanat.stream(), parametritDTO);
        List<SuoritusJaArvosanat> kielikoesuoritukset = suorituksetEnnenLaskennanAlkamistaMyonnettyjenArvosanojenKanssa
                .filter(Objects::nonNull)
                .filter(s -> s.getSuoritus() != null)
                .filter(s -> wrap(s).isAmmatillisenKielikoe())
                .filter(s -> s.getSuoritus().isVahvistettu())
                .filter(s -> s.getArvosanat() != null)
                .filter(s -> !s.getArvosanat().isEmpty()).collect(Collectors.toList());
        return suorituksetKielikohtaisiksiAvainArvoiksi(kielikoesuoritukset);
    }

    private static List<AvainArvoDTO> suorituksetKielikohtaisiksiAvainArvoiksi(List<SuoritusJaArvosanat> oppijanKielikoesuoritukset) {
        List<AvainArvoDTO> relevanttiArvoKullekinKielelle = new LinkedList<>();
        Stream<String> kaikkiKielet = oppijanKielikoesuoritukset.stream().map(SuoritusJaArvosanat::getArvosanat).flatMap(arvosanat ->
            arvosanat.stream().map(Arvosana::getLisatieto)).distinct();
        kaikkiKielet.forEach(kieli -> {
            if (containsSuoritusWithValue(oppijanKielikoesuoritukset, kieli, hyvaksytty)) {
                relevanttiArvoKullekinKielelle.addAll(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, hyvaksytty, Osallistuminen.OSALLISTUI));
            } else if (containsSuoritusWithValue(oppijanKielikoesuoritukset, kieli, hylatty)) {
                relevanttiArvoKullekinKielelle.addAll(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, hylatty, Osallistuminen.OSALLISTUI));
            } else if (containsSuoritusWithValue(oppijanKielikoesuoritukset, kieli, ei_osallistunut)) {
                relevanttiArvoKullekinKielelle.addAll(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, ei_osallistunut, Osallistuminen.EI_OSALLISTUNUT));
            } else {
                relevanttiArvoKullekinKielelle.addAll(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, null, null));
            }
        });
        return relevanttiArvoKullekinKielelle;
    }

    private static boolean containsSuoritusWithValue(List<SuoritusJaArvosanat> oppijanSuoritusJaArvosanaParit, String kieli, SureHyvaksyttyArvosana expectedValue) {
        return oppijanSuoritusJaArvosanaParit.stream().anyMatch(s -> hasValueInAnyArvosana(s, kieli, expectedValue));
    }

    private static boolean hasValueInAnyArvosana(SuoritusJaArvosanat suoritusJaArvosanat, String kieli, SureHyvaksyttyArvosana expectedValue) {
        return suoritusJaArvosanat.getArvosanat().stream().anyMatch(a -> {
            Arvio arvio = a.getArvio();
            if (!SURE_ASTEIKKO_HYVAKSYTTY.equals(arvio.getAsteikko())) {
                throw new IllegalArgumentException(String.format("Suorituksen %s arvosanan %s asteikko on '%s'", suoritusJaArvosanat.getSuoritus(), a, arvio.getAsteikko()));
            }
            return kieli.equals(a.getLisatieto()) && expectedValue.name().equals(arvio.getArvosana());
        });
    }

    private static List<AvainArvoDTO> createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(String kieli, SureHyvaksyttyArvosana valueForAvainArvoDto,
                                                                                                         Osallistuminen osallistuminen) {
        if (valueForAvainArvoDto == null && osallistuminen == null) {
            return Collections.singletonList(new AvainArvoDTO("kielikoe_" + kieli.toLowerCase(), ""));
        }
        return Arrays.asList(
            new AvainArvoDTO("kielikoe_" + kieli.toLowerCase(), valueForAvainArvoDto.arvoForHakuApp),
            new AvainArvoDTO("kielikoe_" + kieli.toLowerCase() + "-OSALLISTUMINEN", osallistuminen.name()));
    }

    public enum SureHyvaksyttyArvosana {
        hyvaksytty("true"), hylatty("false"), ei_osallistunut(""), tyhja("");

        public final String arvoForHakuApp;

        SureHyvaksyttyArvosana(String arvoForHakuApp) {
            this.arvoForHakuApp = arvoForHakuApp;
        }
    }
}
