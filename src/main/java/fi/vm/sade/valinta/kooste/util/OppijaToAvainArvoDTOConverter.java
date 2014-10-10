package fi.vm.sade.valinta.kooste.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;

/**
 * 
 * @author jussi jartamo
 *
 */
public class OppijaToAvainArvoDTOConverter {
	private static final String YO_ASTEIKKO = "YO";
	private static final List<String> YO_ORDER = Collections
			.unmodifiableList(Arrays.asList("L", "E", "M", "C", "B", "A", "I"));

	public static List<AvainArvoDTO> convert(Oppija oppija) {
		if (oppija == null || oppija.getSuoritukset() == null) {
			return Collections.emptyList();
		}
		return convert(oppija.getSuoritukset());
	}

	public static List<AvainArvoDTO> convert(
			List<SuoritusJaArvosanat> suorituksetJaArvosanat) {
		if (suorituksetJaArvosanat == null) {
			return Collections.emptyList();
		}
		List<Arvosana> yoArvosanat = suorituksetJaArvosanat
				.stream()
				//
				.filter(Objects::nonNull)
				//

				.filter(s -> s.getArvosanat() != null)
				//
				.flatMap(s -> s.getArvosanat().stream())
				//
				.filter(a -> a.getAine() != null && a.getArvio() != null
						&& a.getArvio().getArvosana() != null
						&& YO_ASTEIKKO.equals(a.getArvio().getAsteikko()))
				//
				.collect(Collectors.toList());
		// AINEREAALI = max(UE, UO, ET, FF, PS, HI, FY, KE, BI, GE, TE, YH)
		// <br>
		// REAALI = max(RR, RO, RY)
		// <br>
		// PITKA_KIELI = max(EA, FA, GA, HA, PA, SA, TA, VA)
		// <br>
		// KESKIPITKA_KIELI = max(EB, FB, GB, HB, PB, SB, TB, VB)
		// <br>
		// LYHYT_KIELI = max(EC, FC, GC, L1, PC, SC, TC, VC)
		// <br>
		// AIDINKIELI = max(O, A, I, W, Z, O5, A5)
		List<AvainArvoDTO> aaa = Arrays.asList(
				convert("AINEREAALI",
						max(find(yoArvosanat, "UE", "UO", "ET", "FF", "PS",
								"HI", "FY", "KE", "BI", "GE", "TE", "YH"))),
				//
				convert("REAALI", max(find(yoArvosanat, "RR", "RO", "RY"))),
				//
				convert("PITKA_KIELI",
						max(find(yoArvosanat, "EA", "FA", "GA", "HA", "PA",
								"SA", "TA", "VA"))),
				//
				convert("KESKIPITKA_KIELI",
						max(find(yoArvosanat, "EB", "FB", "GB", "HB", "PB",
								"SB", "TB", "VB"))),
				//
				convert("LYHYT_KIELI",
						max(find(yoArvosanat, "EC", "FC", "GC", "L1", "PC",
								"SC", "TC", "VC"))),
				//
				convert("AIDINKIELI",
						max(find(yoArvosanat, "O", "A", "I", "W", "Z", "O5",
								"A5"))));
		List<AvainArvoDTO> avaimet = Lists.newArrayList(yoArvosanat.stream()
				.map(a -> convert(a)).collect(Collectors.toList()));
		avaimet.addAll(aaa);
		return avaimet.stream().filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private static Arvosana max(List<Arvosana> arvosanat) {
		return arvosanat
				.stream()
				.reduce((a, b) -> {
					if (YO_ORDER.indexOf(a.getArvio().getArvosana()) < YO_ORDER
							.indexOf(b.getArvio().getArvosana())) {
						return a;
					} else {
						return b;
					}
				}).orElse(null);
	}

	private static List<Arvosana> find(List<Arvosana> arvosanat,
			String... aineet) {
		Set<String> kohteet = Sets.newHashSet(aineet);
		return arvosanat.stream().filter(a -> kohteet.contains(a.getAine()))
				.collect(Collectors.toList());
	}

	private static AvainArvoDTO convert(String avain, Arvosana arvosana) {
		if (arvosana == null) {
			return null;
		}
		AvainArvoDTO aa = new AvainArvoDTO();
		aa.setArvo(arvosana.getArvio().getArvosana());
		aa.setAvain(avain);
		return aa;
	}

	private static AvainArvoDTO convert(Arvosana arvosana) {
		AvainArvoDTO aa = new AvainArvoDTO();
		aa.setArvo(arvosana.getArvio().getArvosana());
		aa.setAvain(arvosana.getAine());
		return aa;
	}

}
