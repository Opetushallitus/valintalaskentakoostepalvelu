package fi.vm.sade.valinta.kooste.util;

import java.util.*;
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
		Map<String, Arvosana> yoArvosanatMap = suorituksetJaArvosanat
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
                .map(a -> {
                    String uusiAine = aineMapper(a.getAine(), a.getLisatieto());
                    Arvosana arvosana = new Arvosana(a.getId(), a.getSuoritus(),
                            uusiAine, a.getValinnainen(), a.getMyonnetty(), a.getSource(),
                            a.getArvio(), a.getLisatieto());
                    return arvosana;
                })
                //
				.collect(Collectors.toMap(Arvosana::getAine, a->a,
                        (s, a) -> max(Arrays.asList(s,a))));

        List<Arvosana> yoArvosanat = new ArrayList<>(yoArvosanatMap.values());
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

	public static Arvosana max(List<Arvosana> arvosanat) {
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

    private static String aineMapper(String aine, String lisatieto) {
        if (aine.equals("AINEREAALI")) {
            return lisatieto;
        } else if (aine.equals("REAALI")) { // Vanha reaali
            switch (lisatieto) {
                case "ET":
                    return "RY";
                case "UO":
                    return "RO";
                case "UE":
                    return "RR";
                default:
                    return "REAALI_"+lisatieto;
            }

        } else if(aine.equals("A")) { // Pitkät kielit
            switch (lisatieto) {
                case "SA": return "SA";
                case "EN": return "EA";
                case "UN": return "HA";
                case "VE": return "VA";
                case "IT": return "TA";
                case "RA": return "FA";
                case "FI": return "CA";
                case "RU": return "BA";
                case "ES": return "PA";
                case "PG": return "GA";
                default: return "A_"+lisatieto;
            }
        } else if(aine.equals("B")) { // Keskipitkät kielet
            switch (lisatieto) {
                case "SA": return "SB";
                case "EN": return "EB";
                case "UN": return "HB";
                case "VE": return "VB";
                case "IT": return "TB";
                case "RA": return "FB";
                case "FI": return "CB";
                case "RU": return "BB";
                case "ES": return "PB";
                case "PG": return "GB";
                default: return "B_"+lisatieto;
            }
        } else if(aine.equals("C")) { // Lyhyet kielet
            switch (lisatieto) {
                case "SA": return "SC";
                case "EN": return "EC";
                case "UN": return "HC";
                case "VE": return "VC";
                case "IT": return "TC";
                case "RA": return "FC";
                case "FI": return "CC";
                case "RU": return "BC";
                case "ES": return "PC";
                case "PG": return "GC";
                case "IS": return "IC";
                case "ZA": return "DC";
                case "KR": return "KC";
                case "QS": return "QC";
                case "LA": return "L7";
                default: return "C_"+lisatieto;
            }
        } else if(aine.equals("AI")) { // Äidinkielet
            switch (lisatieto) {
                case "IS": return "I";
                case "RU": return "O";
                case "FI": return "A";
                case "ZA": return "Z";
                case "QS": return "W";
                default: return "AI_"+lisatieto;
            }
        } else if(aine.equals("VI2")) { // Suomi/Ruotsi toisena kielenä
            switch (lisatieto) {
                case "RU": return "O5";
                case "FI": return "A5";
                default: return "VI2_"+lisatieto;
            }
        } else if(aine.equals("PITKA")) { // Pitkä matematiikka
            switch (lisatieto) {
                case "MA": return "M";
                default: return "PITKA_"+lisatieto;
            }
        } else if(aine.equals("LYHYT")) { // Lyhyt matematiikka
            switch (lisatieto) {
                case "MA": return "N";
                default: return "LYHYT_"+lisatieto;
            }
        } else if(aine.equals("SAKSALKOUL")) { // Saksalaisen koulun saksan kielen koe
            switch (lisatieto) {
                case "SA": return "S9";
                default: return "SAKSALKOUL_"+lisatieto;
            }
        } else if(aine.equals("KYPSYYS")) { // Englanninkielinen kypsyyskoe
            switch (lisatieto) {
                case "EN": return "J";
                default: return "KYPSYYS_"+lisatieto;
            }
        } else if(aine.equals("D")) { // Latina
            switch (lisatieto) {
                case "LA": return "L1";
                default: return "D_"+lisatieto;
            }
        } else {
            return "XXX";
        }

    }

}
