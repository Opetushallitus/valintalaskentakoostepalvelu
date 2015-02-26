package fi.vm.sade.valinta.kooste.util.sure;

import com.codepoetics.protonpack.StreamUtils;
import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
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
    private static final Logger LOG = LoggerFactory.getLogger(ArvosanaToAvainArvoDTOConverter.class);
    private final String prefiksi;
    private final String suffiksi;

    public ArvosanaToAvainArvoDTOConverter(String prefiksi) {
        this.prefiksi = prefiksi;
        this.suffiksi = "";
    }

    public ArvosanaToAvainArvoDTOConverter(String prefiksi, String suffiksi) {
        this.prefiksi = prefiksi;
        this.suffiksi = Optional.ofNullable(suffiksi).orElse("");
    }
    private Stream<AvainArvoDTO> suorituksenTila(SuoritusJaArvosanat suoritus) {
        AvainArvoDTO a = new AvainArvoDTO();
        a.setAvain(new StringBuilder(prefiksi).append("TILA").append(suffiksi).toString());
        if(new SuoritusJaArvosanatWrapper(suoritus).isValmis()) {
            a.setArvo("true");
        } else {
            a.setArvo("false");
        }
        return Stream.of(a);
    }
    public Stream<AvainArvoDTO> convert(
            Optional<SuoritusJaArvosanat> suoritusOption, DateTime pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan) {
        SuoritusJaArvosanat suoritus = suoritusOption.orElse(null);
        if(suoritus == null) {
            return Stream.empty();
        }
        return Stream.concat(suorituksenTila(suoritus), suoritus.getArvosanat().stream()
                //
                //.filter(s -> new SuoritusJaArvosanatWrapper(s).isPerusopetus())
                //
                //.flatMap(s -> s.getArvosanat().stream())
                //
                .filter(a -> a != null && new ArvosanaWrapper(a).onkoMyonnettyEnnen(pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan))
                        //
                .collect(Collectors.groupingBy(a -> ((Arvosana) a).getAine(),
                        Collectors.mapping(a -> a, Collectors.<Arvosana>toList()))).entrySet().stream()
                        //
                .flatMap(e -> {
                    return Stream.concat(
                            arvosanaToAvainArvo(prefiksi, suffiksi).apply(e.getValue()
                    ), Stream.of(e.getValue().stream().filter(a -> a.isValinnainen()).collect(Collectors.toList())).flatMap(
                            a -> StreamUtils.zipWithIndex(a.stream()).flatMap(ax -> {
                                String sfx = new StringBuilder("_VAL").append(
                                        ax.getIndex() + 1 // valinnaiset esitetaan yksi alkuisella jarjestysnumerolla (ei nolla alkuisella)
                                ).append(suffiksi).toString();
                                return valinnainenToAvainArvo(prefiksi, sfx).apply(ax.getValue());
                            })));
                }));

                        /*
                        Stream.concat(e.getValue().stream().filter(a -> !a.isValinnainen()).flatMap(
                                    arvosanaToAvainArvo(prefiksi)
                            ), Stream.of(e.getValue().stream().filter(a -> a.isValinnainen()).collect(Collectors.toList())).flatMap(
                                a ->  StreamUtils.zipWithIndex(a.stream()).flatMap(ax -> {
                                    String suffiksi = new StringBuilder("_VAL").append(
                                            ax.getIndex() + 1 // valinnaiset esitetaan yksi alkuisella jarjestysnumerolla (ei nolla alkuisella)
                                    ).toString();
                                    return valinnainenToAvainArvo(prefiksi, suffiksi).apply(ax.getValue());
                                }))
                  ));*/
    }

    private static Function<List<Arvosana>, Stream<AvainArvoDTO>> arvosanaToAvainArvo(String p, String s) {
        return a -> {
            List<Arvosana> eiValinnaisetArvosanat = a.stream().filter(ax -> !ax.isValinnainen()).collect(Collectors.toList());
            if(eiValinnaisetArvosanat.isEmpty()) {
                LOG.error("Valinnainen arvosana löytyy mutta arvosanaa ei löydy");
                throw new RuntimeException("Valinnainen arvosana löytyy mutta arvosanaa ei löydy");
            }
            Arvosana paras;
            if(eiValinnaisetArvosanat.size() > 1) {
            TreeSet<Arvosana> arvosanaSet = new TreeSet<Arvosana>((c0, c1) -> {

                String asteikko = ((String)(((Arvosana)c0).getArvio()).getAsteikko());
                if(!asteikko.equals(c1.getArvio().getAsteikko())) {
                    LOG.error("Asteikot ei täsmää: {} {}", new Gson().toJson(c0), new Gson().toJson(c1));
                    throw new RuntimeException("Asteikot ei täsmää: " + c0.getArvio().getAsteikko() + " " + c1.getArvio().getAsteikko());
                }
                Integer i0 = Integer.parseInt((String) c0.getArvio().getArvosana());
                Integer i1 = Integer.parseInt((String) c1.getArvio().getArvosana());
                return i1.compareTo(i0);
            });
            arvosanaSet.addAll(eiValinnaisetArvosanat);
            paras = arvosanaSet.first();
            } else {
                paras = eiValinnaisetArvosanat.iterator().next();
            }

            AvainArvoDTO a0 = new AvainArvoDTO();
            a0.setArvo(paras.getArvio().getArvosana());
            a0.setAvain(new StringBuilder(p).append(paras.getAine()).append(Optional.ofNullable(s).orElse("")).toString());
            if (paras.getLisatieto() != null) {
                AvainArvoDTO a1 = new AvainArvoDTO();
                a1.setArvo(paras.getLisatieto());
                a1.setAvain(new StringBuilder(a0.getAvain()).append("_OPPIAINE").append(Optional.ofNullable(s).orElse("")).toString());
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
