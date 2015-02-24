package fi.vm.sade.valinta.kooste.util.sure;

import com.codepoetics.protonpack.StreamUtils;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Jussi Jartamo
 *
 * Prefiksi PK_
 *
 */
public class ArvosanaToAvainArvoDTOConverter {

    private final String prefiksi;

    public ArvosanaToAvainArvoDTOConverter(String prefiksi) {
        this.prefiksi = prefiksi;
    }

    public Stream<AvainArvoDTO> convert(
            Stream<SuoritusJaArvosanat> pkSuoritus) {
        return pkSuoritus
                //
                //.filter(s -> new SuoritusJaArvosanatWrapper(s).isPerusopetus())
                //
                .flatMap(s -> s.getArvosanat().stream())
                //
                .collect(Collectors.groupingBy(a -> ((Arvosana) a).getAine(),
                        Collectors.mapping(a -> a, Collectors.<Arvosana>toList()))).entrySet().stream()
                        //
                .flatMap(e ->  Stream.concat(e.getValue().stream().filter(a -> !a.isValinnainen()).flatMap(
                                    arvosanaToAvainArvo(prefiksi)
                            ), Stream.of(e.getValue().stream().filter(a -> a.isValinnainen()).collect(Collectors.toList())).flatMap(
                                a ->  StreamUtils.zipWithIndex(a.stream()).flatMap(ax -> {
                                    String suffiksi = new StringBuilder("_VAL").append(
                                            ax.getIndex() + 1 // valinnaiset esitetaan yksi alkuisella jarjestysnumerolla (ei nolla alkuisella)
                                    ).toString();
                                    return valinnainenToAvainArvo(prefiksi, suffiksi).apply(ax.getValue());
                                }))
                  ));
    }

    private static Function<Arvosana, Stream<AvainArvoDTO>> arvosanaToAvainArvo(String p) {
        return a -> {
            AvainArvoDTO a0 = new AvainArvoDTO();
            a0.setArvo(a.getArvio().getArvosana());
            a0.setAvain(new StringBuilder(p).append(a.getAine()).toString());
            if (a.getLisatieto() != null) {
                AvainArvoDTO a1 = new AvainArvoDTO();
                a1.setArvo(a.getLisatieto());
                a1.setAvain(new StringBuilder(a0.getAvain()).append("_OPPIAINE").toString());
                return Stream.of(a0, a1);
            }
            return Stream.of(a0);
        };
    }
    // Ei oppiainetta valinnaiselle
    private static Function<Arvosana, Stream<AvainArvoDTO>> valinnainenToAvainArvo(String p, String s) {
        return a -> {
            AvainArvoDTO a0 = new AvainArvoDTO();
            a0.setArvo(a.getArvio().getArvosana());
            a0.setAvain(new StringBuilder(p).append(a.getAine()).append(Optional.ofNullable(s).orElse("")).toString());
            return Stream.of(a0);
        };
    }
}
