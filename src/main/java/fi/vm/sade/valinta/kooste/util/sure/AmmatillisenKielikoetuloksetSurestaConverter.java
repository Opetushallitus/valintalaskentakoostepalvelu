package fi.vm.sade.valinta.kooste.util.sure;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.wrap;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AmmatillisenKielikoetuloksetSurestaConverter {
    private static final String SURE_ASTEIKKO_HYVAKSYTTY = "HYVAKSYTTY";

    public static List<AvainArvoDTO> convert(List<SuoritusJaArvosanat> oppijanSuorituksetJaArvosanat) {
        if (oppijanSuorituksetJaArvosanat == null) {
            return Collections.emptyList();
        }

        Stream<SuoritusJaArvosanat> kielikoesuoritukset = oppijanSuorituksetJaArvosanat.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getSuoritus() != null)
                .filter(s -> wrap(s).isAmmatillisenKielikoe())
                .filter(s -> s.getSuoritus().isVahvistettu())
                .filter(s -> s.getArvosanat() != null);
        return suorituksetKielikohtaisiksiAvainArvoiksi(kielikoesuoritukset);
    }

    private static List<AvainArvoDTO> suorituksetKielikohtaisiksiAvainArvoiksi(Stream<SuoritusJaArvosanat> kielikoesuoritukset) {
        List<AvainArvoDTO> relevanttiArvoKullekinKielelle = new LinkedList<>();
        Map<String, List<SuoritusJaArvosanat>> arvosanatKielittain = kielikoesuoritukset.collect(Collectors.groupingBy(AmmatillisenKielikoetuloksetSurestaConverter::kieliArvosananLisatiedosta));
        for (String kieli : arvosanatKielittain.keySet()) {
            List<SuoritusJaArvosanat> kielenSuoritusArvosanaParit = arvosanatKielittain.get(kieli);
            if (containsSuoritusWithValue(kielenSuoritusArvosanaParit, "true")) {
                relevanttiArvoKullekinKielelle.add(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, "true"));
            } else if (containsSuoritusWithValue(kielenSuoritusArvosanaParit, "false")) {
                relevanttiArvoKullekinKielelle.add(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, "false"));
            } else {
                relevanttiArvoKullekinKielelle.add(createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(kieli, ""));
            }
        }
        return relevanttiArvoKullekinKielelle;
    }

    private static String kieliArvosananLisatiedosta(SuoritusJaArvosanat suoritusJaArvosanat) {
        List<Arvosana> arvosanat = suoritusJaArvosanat.getArvosanat();
        if (arvosanat.size() != 1) {
            throw new IllegalStateException(String.format("Suoritukselle %s löytyi yhdestä poikkeava määrä (%d) arvosanoja: %s", suoritusJaArvosanat.getSuoritus(), arvosanat.size(), arvosanat));
        }
        return arvosanat.get(0).getLisatieto();
    }

    private static boolean containsSuoritusWithValue(List<SuoritusJaArvosanat> kielenSuoritusArvosanaParit, String expectedValue) {
        return kielenSuoritusArvosanaParit.stream().anyMatch(s -> hasValueInOnlyArvosana(s, expectedValue));
    }

    private static boolean hasValueInOnlyArvosana(SuoritusJaArvosanat suoritusJaArvosanat, String expectedValue) {
        Arvosana onlyArvosana = suoritusJaArvosanat.getArvosanat().get(0);
        Arvio onlyArvio = onlyArvosana.getArvio();
        if (!SURE_ASTEIKKO_HYVAKSYTTY.equals(onlyArvio.getAsteikko())) {
            throw new IllegalArgumentException(String.format("Suorituksen %s arvosanan %s asteikko on '%s'", suoritusJaArvosanat.getSuoritus(), onlyArvosana, onlyArvio.getAsteikko()));
        }
        return expectedValue.equals(onlyArvio.getArvosana());
    }

    private static AvainArvoDTO createAmmatillisenKielikoeAvainArvoDtoCompatibleWithOldHakuAppData(String kieli, String valueForAvainArvoDto) {
        return new AvainArvoDTO("kielikoe_" + kieli.toLowerCase(), valueForAvainArvoDto);
    }
}
