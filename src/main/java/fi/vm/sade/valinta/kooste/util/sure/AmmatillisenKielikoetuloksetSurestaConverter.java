package fi.vm.sade.valinta.kooste.util.sure;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.wrap;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.util.OppijaToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
            if (containsSuoritusWithValue(oppijanKielikoesuoritukset, kieli, "true")) {
                relevanttiArvoKullekinKielelle.add(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, "true"));
            } else if (containsSuoritusWithValue(oppijanKielikoesuoritukset, kieli, "false")) {
                relevanttiArvoKullekinKielelle.add(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, "false"));
            } else {
                relevanttiArvoKullekinKielelle.add(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, ""));
            }
        });
        return relevanttiArvoKullekinKielelle;
    }

    private static boolean containsSuoritusWithValue(List<SuoritusJaArvosanat> oppijanSuoritusJaArvosanaParit, String kieli, String expectedValue) {
        return oppijanSuoritusJaArvosanaParit.stream().anyMatch(s -> hasValueInAnyArvosana(s, kieli, expectedValue));
    }

    private static boolean hasValueInAnyArvosana(SuoritusJaArvosanat suoritusJaArvosanat, String kieli, String expectedValue) {
        return suoritusJaArvosanat.getArvosanat().stream().anyMatch(a -> {
            Arvio arvio = a.getArvio();
            if (!SURE_ASTEIKKO_HYVAKSYTTY.equals(arvio.getAsteikko())) {
                throw new IllegalArgumentException(String.format("Suorituksen %s arvosanan %s asteikko on '%s'", suoritusJaArvosanat.getSuoritus(), a, arvio.getAsteikko()));
            }
            return kieli.equals(a.getLisatieto()) && expectedValue.equals(arvio.getArvosana());
        });
    }

    private static AvainArvoDTO createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(String kieli, String valueForAvainArvoDto) {
        return new AvainArvoDTO("kielikoe_" + kieli.toLowerCase(), valueForAvainArvoDto);
    }
}
