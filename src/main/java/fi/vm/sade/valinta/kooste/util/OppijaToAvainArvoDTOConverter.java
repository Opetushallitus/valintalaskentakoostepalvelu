package fi.vm.sade.valinta.kooste.util;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;
import static java.util.Optional.ofNullable;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.*;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.util.sure.ArvosanaToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author jussi jartamo
 *
 */
public class OppijaToAvainArvoDTOConverter {
        private static final Logger LOG = LoggerFactory.getLogger(OppijaToAvainArvoDTOConverter.class);
        private static final ArvosanaToAvainArvoDTOConverter PERUSOPETUS = new ArvosanaToAvainArvoDTOConverter("PK_");
        private static final ArvosanaToAvainArvoDTOConverter LISAOPETUS = new ArvosanaToAvainArvoDTOConverter("PK_", "_10");
        //private static final ArvosanaToAvainArvoDTOConverter AMMATTISTARTTI = new ArvosanaToAvainArvoDTOConverter("AS_");
        //private static final ArvosanaToAvainArvoDTOConverter VALMENTAVA = new ArvosanaToAvainArvoDTOConverter("VA_");
        //private static final ArvosanaToAvainArvoDTOConverter AMMATILLISEEN_VALMENTAVA = new ArvosanaToAvainArvoDTOConverter("AV_");
        //private static final ArvosanaToAvainArvoDTOConverter ULKOMAINEN_KORVAAVA = new ArvosanaToAvainArvoDTOConverter("UK_");
        private static final ArvosanaToAvainArvoDTOConverter LUKIO = new ArvosanaToAvainArvoDTOConverter("LK_");
        //private static final ArvosanaToAvainArvoDTOConverter AMMATILLINEN = new ArvosanaToAvainArvoDTOConverter("AM_");

        public static List<AvainArvoDTO> convert(Oppija oppija, ParametritDTO parametritDTO) {
                if (oppija == null || oppija.getSuoritukset() == null) {
                return Collections.emptyList();
                }
                Stream<AvainArvoDTO> avainArvot = convert(oppija, oppija.getSuoritukset(),parametritDTO);
                AvainArvoDTO ensikertalaisuus = new AvainArvoDTO();
                ensikertalaisuus.setAvain("ensikertalainen");
                ensikertalaisuus.setArvo(String.valueOf(oppija.isEnsikertalainen()));
                //avainArvot.add(ensikertalaisuus);
                return concat(of(ensikertalaisuus), avainArvot).collect(Collectors.toList());
        }

        private static Stream<AvainArvoDTO> convert(
                Oppija oppija,
                List<SuoritusJaArvosanat> suorituksetJaArvosanat, ParametritDTO parametritDTO) {
                if (suorituksetJaArvosanat == null) {
                        return empty();
                }
                List<SuoritusJaArvosanat> suoritukset =
                        suorituksetJaArvosanat.stream()
                                .filter(Objects::nonNull)
                                        //
                                .filter(s -> s.getSuoritus() != null)
                                        //
                                .filter(s -> s.getArvosanat() != null)
                                //
                                .filter(s ->
                                        wrap(s).isLukio() || wrap(s).isYoTutkinto() || wrap(s).isPerusopetus() || wrap(s).isLisaopetus()
                                )
                                // EI ITSEILMOITETTUJA LASKENTAAN
                                .filter(s -> !new SuoritusJaArvosanatWrapper(s).isItseIlmoitettu())
                                .collect(Collectors.toList());
                if(suoritukset.isEmpty()) {
                        return empty();
                }

                final DateTime pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan =

                        ofNullable(ofNullable(ofNullable(parametritDTO).orElse(new ParametritDTO()).getPH_VLS()).orElse(new ParametriDTO()).getDateStart()).map(
                                startDate -> {
                                        if (startDate != null) {
                                                return new DateTime(startDate);
                                        } else {
                                                return null;
                                        }
                                }
                        ).orElse(null);

                suoritukset.stream().filter(s ->
                        // EI OLE KYMPPILUOKKA JA ON KESKEYTYNYT NIIN FILTTEROIDAAN POIS
                        !(!new SuoritusJaArvosanatWrapper(s).isLisaopetus()
                && new SuoritusJaArvosanatWrapper(s).isKeskeytynyt()))
                        .collect(Collectors.groupingBy(a -> ((SuoritusJaArvosanat) a).getSuoritus().getKomo(),
                        Collectors.mapping(a -> a, Collectors.<SuoritusJaArvosanat>toList()))).entrySet().stream()
                .forEach(s -> {
                        if(s.getValue().size() > 1) {
                                SuoritusJaArvosanat s0 = s.getValue().iterator().next();
                                String komo = new SuoritusJaArvosanatWrapper(s0).komoToString();
                                LOG.error("Sama suoritus löytyi moneen kertaan! Komo OID {} ({}), oppijalle {}", s0.getSuoritus().getKomo(), komo,oppija.getOppijanumero());
                                throw new RuntimeException("Sama suoritus löytyi moneen kertaan! Komo OID "+s0.getSuoritus().getKomo()+" ("+komo+") oppijalle " + oppija.getOppijanumero());
                        }
                });


                return convertP(suoritukset.stream(), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan);
        }

        private static Stream<AvainArvoDTO> yoTila(SuoritusJaArvosanat s) {
                return of(s).filter(s0 -> wrap(s0).isYoTutkinto() && !wrap(s0).isKeskeytynyt()).map(suoritus -> {
                        AvainArvoDTO a = new AvainArvoDTO();
                        a.setAvain(new StringBuilder("YO_").append("TILA").toString());
                        if(new SuoritusJaArvosanatWrapper(suoritus).isValmis()) {
                                a.setArvo("true");
                        } else {
                                a.setArvo("false");
                        }
                        return of(a);
                }).findAny().orElse(empty());
        }

	private static Stream<AvainArvoDTO> convertP(
			Stream<SuoritusJaArvosanat> suoritukset, DateTime pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan) {


            return suoritukset
                    .flatMap(s ->
                                    of(
                                            yoTila(s),
                                            PERUSOPETUS.convert(of(s).filter(s0 -> wrap(s0).isPerusopetus() && !wrap(s0).isKeskeytynyt()).findAny(), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan),
                                            LISAOPETUS.convert(of(s).filter(s0 -> wrap(s0).isLisaopetus()).findAny(), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan),
                                            //AMMATTISTARTTI.convert(of(s).filter(s0 -> wrap(s0).isAmmattistartti()), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan),
                                            //VALMENTAVA.convert(of(s).filter(s0 -> wrap(s0).isValmentava()), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan),
                                            //AMMATILLISEEN_VALMENTAVA.convert(of(s).filter(s0 -> wrap(s0).isAmmatilliseenValmistava()), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan),
                                            //ULKOMAINEN_KORVAAVA.convert(of(s).filter(s0 -> wrap(s0).isUlkomainenKorvaava()), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan),
                                            LUKIO.convert(of(s).filter(s0 -> wrap(s0).isLukio() && !wrap(s0).isKeskeytynyt()).findAny(), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan)
                                            //AMMATILLINEN.convert(of(s).filter(s0 -> wrap(s0).isAmmatillinen()), pvmMistaAlkaenUusiaSuorituksiaEiOtetaEnaaMukaan)
                                    ).flatMap(sx -> sx)
                    )
                .filter(Objects::nonNull);
	}

}
