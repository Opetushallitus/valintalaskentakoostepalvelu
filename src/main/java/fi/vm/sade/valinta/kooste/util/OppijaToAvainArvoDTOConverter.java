package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.ArvosanaWrapper;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.util.sure.ArvosanaToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.joda.time.DateTime;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.wrap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;

public class OppijaToAvainArvoDTOConverter {
    private static final String PK_PREFIX = "PK_";
    private static final String LK_PREFIX = "LK_";
    private static final String AM_PREFIX = "AM_";

    public static List<AvainArvoDTO> convert(Oppija oppija, ParametritDTO parametritDTO) {
        if (oppija == null || oppija.getSuoritukset() == null) {
            return Collections.emptyList();
        }

        final List<SuoritusJaArvosanat> oppijanSuoritukset = oppija.getSuoritukset().stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getSuoritus() != null)
                .filter(s -> s.getArvosanat() != null)
                .collect(Collectors.toList());

        final Stream<SuoritusJaArvosanat> lukio = oppijanSuoritukset.stream()
                .filter(s -> wrap(s).isLukio() && wrap(s).isValmis());

        final Stream<SuoritusJaArvosanat> ammatillinen = oppijanSuoritukset.stream()
                .filter(s -> wrap(s).isAmmatillinen() && wrap(s).isValmis());

        final Stream<SuoritusJaArvosanat> peruskoulu = oppijanSuoritukset.stream()
                .filter(s -> wrap(s).isPerusopetus() && wrap(s).isValmis());

        final Stream<SuoritusJaArvosanat> lisaopetus = oppijanSuoritukset.stream()
                .filter(s -> wrap(s).isLisapistekoulutus());

        final Stream<SuoritusJaArvosanat> yo = oppijanSuoritukset.stream()
                .filter(s -> wrap(s).isYoTutkinto() && wrap(s).isValmis() && wrap(s).isVahvistettu());

        return suorituksetAvainArvoiksi(oppija, removeLaskennanAlkamisenJalkeenMyonnetytArvosanat(
                concat(peruskoulu, concat(lukio, concat(lisaopetus, concat(ammatillinen, yo)))), parametritDTO
                ).collect(Collectors.toList()));
    }

    private static Stream<SuoritusJaArvosanat> removeLaskennanAlkamisenJalkeenMyonnetytArvosanat(
            Stream<SuoritusJaArvosanat> filtteroimattomat, ParametritDTO parametritDTO) {
        Optional<DateTime> date = valintalaskennanStartDate(parametritDTO);
        if (date.isPresent()) {
            return filtteroimattomat.map(s -> {
                s.setArvosanat(s.getArvosanat().stream()
                        .filter(a -> a.getMyonnetty() == null || new ArvosanaWrapper(a).onkoMyonnettyEnnen(date.get()))
                        .collect(Collectors.toList()));
                return s;
            });
        } else {
            return filtteroimattomat;
        }
    }

    private static List<AvainArvoDTO> suorituksetAvainArvoiksi(Oppija oppija, List<SuoritusJaArvosanat> suoritukset) {
        List<SuoritusJaArvosanat> peruskoulunkaltaisetSuoritukset = suoritukset.stream()
                .filter(s -> wrap(s).isSuoritusMistaSyntyyPeruskoulunArvosanoja())
                .collect(Collectors.toList());
        List<SuoritusJaArvosanat> lukioSuoritukset = suoritukset.stream()
                .filter(s -> wrap(s).isLukio())
                .collect(Collectors.toList());
        List<SuoritusJaArvosanat> ammatillisetSuoritukset = suoritukset.stream()
                .filter(s -> wrap(s).isAmmatillinen())
                .collect(Collectors.toList());
        return new ArrayList<AvainArvoDTO>() {{
            addAll(ArvosanaToAvainArvoDTOConverter.convert(peruskoulunkaltaisetSuoritukset, PK_PREFIX, "", oppija.getOppijanumero()));
            addAll(ArvosanaToAvainArvoDTOConverter.convert(lukioSuoritukset, LK_PREFIX, "", oppija.getOppijanumero()));
            addAll(ArvosanaToAvainArvoDTOConverter.convert(ammatillisetSuoritukset, AM_PREFIX, "", oppija.getOppijanumero()));
        }};
    }

    private static Optional<DateTime> valintalaskennanStartDate(ParametritDTO parametritDTO) {
        return ofNullable(parametritDTO)
                .flatMap(parametrit -> ofNullable(parametrit.getPH_VLS()))
                .flatMap(parametri -> ofNullable(parametri.getDateStart()))
                .map(DateTime::new);
    }
}
