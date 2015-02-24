package fi.vm.sade.valinta.kooste.util;

import java.util.*;
import java.util.stream.Collectors;
import static java.util.stream.Stream.*;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import static fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper.*;
import fi.vm.sade.valinta.kooste.util.sure.ArvosanaToAvainArvoDTOConverter;
import fi.vm.sade.valinta.kooste.util.sure.YoToAvainArvoDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;

/**
 * 
 * @author jussi jartamo
 *
 */
public class OppijaToAvainArvoDTOConverter {

        private static final ArvosanaToAvainArvoDTOConverter PERUSOPETUS = new ArvosanaToAvainArvoDTOConverter("PK_");
        private static final ArvosanaToAvainArvoDTOConverter LISAOPETUS = new ArvosanaToAvainArvoDTOConverter("LO_");
        private static final ArvosanaToAvainArvoDTOConverter AMMATTISTARTTI = new ArvosanaToAvainArvoDTOConverter("AS_");
        private static final ArvosanaToAvainArvoDTOConverter VALMENTAVA = new ArvosanaToAvainArvoDTOConverter("VA_");
        private static final ArvosanaToAvainArvoDTOConverter AMMATILLISEEN_VALMENTAVA = new ArvosanaToAvainArvoDTOConverter("AV_");
        private static final ArvosanaToAvainArvoDTOConverter ULKOMAINEN_KORVAAVA = new ArvosanaToAvainArvoDTOConverter("UK_");
        private static final ArvosanaToAvainArvoDTOConverter LUKIO = new ArvosanaToAvainArvoDTOConverter("LK_");
        private static final ArvosanaToAvainArvoDTOConverter AMMATILLINEN = new ArvosanaToAvainArvoDTOConverter("AM_");

	public static List<AvainArvoDTO> convert(Oppija oppija) {
		if (oppija == null || oppija.getSuoritukset() == null) {
			return Collections.emptyList();
		}
        List<AvainArvoDTO> avainArvot = convert(oppija.getSuoritukset());
        AvainArvoDTO ensikertalaisuus = new AvainArvoDTO();
        ensikertalaisuus.setAvain("ensikertalainen");
        ensikertalaisuus.setArvo(String.valueOf(oppija.isEnsikertalainen()));
        avainArvot.add(ensikertalaisuus);
        return avainArvot;
	}

	public static List<AvainArvoDTO> convert(
			List<SuoritusJaArvosanat> suorituksetJaArvosanat) {
		if (suorituksetJaArvosanat == null) {
			return Collections.emptyList();
		}
        List<SuoritusJaArvosanat> suoritukset =
                suorituksetJaArvosanat.stream()
                        .filter(Objects::nonNull)
                        //
                        .filter(s -> s.getSuoritus() != null)
                        //
                        .filter(s -> s.getArvosanat() != null).collect(Collectors.toList());
        if(suoritukset.isEmpty()) {
            return Collections.emptyList();
        }
		return suoritukset.stream()
                .flatMap(s ->
                        of(
                                YoToAvainArvoDTOConverter.convert(s),
                                PERUSOPETUS.convert(of(s).filter(s0 -> wrap(s0).isPerusopetus())),
                                LISAOPETUS.convert(of(s).filter(s0 -> wrap(s0).isLisaopetus())),
                                AMMATTISTARTTI.convert(of(s).filter(s0 -> wrap(s0).isAmmattistartti())),
                                VALMENTAVA.convert(of(s).filter(s0 -> wrap(s0).isValmentava())),
                                AMMATILLISEEN_VALMENTAVA.convert(of(s).filter(s0 -> wrap(s0).isAmmatilliseenValmistava())),
                                ULKOMAINEN_KORVAAVA.convert(of(s).filter(s0 -> wrap(s0).isUlkomainenKorvaava())),
                                LUKIO.convert(of(s).filter(s0 -> wrap(s0).isLukio())),
                                AMMATILLINEN.convert(of(s).filter(s0 -> wrap(s0).isAmmatillinen()))).flatMap(sx -> sx)
                )
                .filter(Objects::nonNull).collect(Collectors.toList());
	}

}
