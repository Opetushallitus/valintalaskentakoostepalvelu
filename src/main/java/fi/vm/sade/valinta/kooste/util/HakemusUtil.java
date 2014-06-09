package fi.vm.sade.valinta.kooste.util;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUNTUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.EI_ILMOITTAUTUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.EI_TEHTY;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.LASNA;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.LASNA_SYKSY;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.POISSA;
import static fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila;

/**
 * 
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakemusUtil {

	private static final Logger LOG = LoggerFactory
			.getLogger(HakemusUtil.class);
	private final static Map<String, Map<HakemuksenTila, String>> TILAT = valmistaTilat();
	private final static Map<String, String> VARASIJAT = varasijaTekstinTilat();
	private final static Map<String, Map<IlmoittautumisTila, String>> ILMOITTAUTUMISTILAT = ilmoittautumisTilat();
	public static final String ASIOINTIKIELI = "asiointikieli";

	private static Map<String, String> varasijaTekstinTilat() {
		Map<String, String> varasijaTekstinTilat = Maps.newHashMap();
		varasijaTekstinTilat.put(KieliUtil.SUOMI, "Varasijan numero on ");
		varasijaTekstinTilat.put(KieliUtil.RUOTSI, "Reservplatsens nummer är ");
		varasijaTekstinTilat.put(KieliUtil.ENGLANTI, "Waiting list number is ");
		return varasijaTekstinTilat;
	}

	private static Map<String, Map<IlmoittautumisTila, String>> ilmoittautumisTilat() {

		Map<String, Map<IlmoittautumisTila, String>> kielet = new HashMap<String, Map<IlmoittautumisTila, String>>();

		Map<IlmoittautumisTila, String> fi = new HashMap<IlmoittautumisTila, String>();
		// *
		// *public enum IlmoittautumisTila {
		// EI_TEHTY, // Ei tehty
		// LASNA_KOKO_LUKUVUOSI, // Läsnä (koko lukuvuosi)
		// POISSA_KOKO_LUKUVUOSI, // Poissa (koko lukuvuosi)
		// EI_ILMOITTAUTUNUT, // Ei ilmoittautunut
		// LASNA_SYKSY, // Läsnä syksy, poissa kevät
		// POISSA_SYKSY, // Poissa syksy, läsnä kevät
		// LASNA, // Läsnä, keväällä alkava koulutus
		// POISSA; // Poissa, keväällä alkava koulutus
		// }
		fi.put(EI_TEHTY, "Ei tehty");
		fi.put(LASNA_KOKO_LUKUVUOSI, "Läsnä koko lukuvuosi");
		fi.put(POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
		fi.put(EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
		fi.put(LASNA_SYKSY, "Läsnä syksy");
		fi.put(LASNA, "Läsnä");
		fi.put(POISSA, "Poissa");

		Map<IlmoittautumisTila, String> sv = new HashMap<IlmoittautumisTila, String>();
		sv.put(EI_TEHTY, "Ei tehty");
		sv.put(LASNA_KOKO_LUKUVUOSI, "Läsnä koko lukuvuosi");
		sv.put(POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
		sv.put(EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
		sv.put(LASNA_SYKSY, "Läsnä syksy");
		sv.put(LASNA, "Läsnä");
		sv.put(POISSA, "Poissa");
		Map<IlmoittautumisTila, String> en = new HashMap<IlmoittautumisTila, String>();
		en.put(EI_TEHTY, "Ei tehty");
		en.put(LASNA_KOKO_LUKUVUOSI, "Läsnä koko lukuvuosi");
		en.put(POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
		en.put(EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
		en.put(LASNA_SYKSY, "Läsnä syksy");
		en.put(LASNA, "Läsnä");
		en.put(POISSA, "Poissa");

		kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
		kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
		kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
		return Collections.unmodifiableMap(kielet);
	}

	private static Map<String, Map<HakemuksenTila, String>> valmistaTilat() {

		Map<String, Map<HakemuksenTila, String>> kielet = new HashMap<String, Map<HakemuksenTila, String>>();

		Map<HakemuksenTila, String> fi = new HashMap<HakemuksenTila, String>();
		fi.put(HYLATTY, "Hylätty");
		fi.put(VARALLA, "Varalla");
		fi.put(PERUUNTUNUT, "Peruuntunut");
		fi.put(HYVAKSYTTY, "Hyväksytty");
		fi.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Harkinnanvaraisesti hyväksytty");
		fi.put(PERUNUT, "Perunut");
		Map<HakemuksenTila, String> sv = new HashMap<HakemuksenTila, String>();
		sv.put(HYLATTY, "Underkänd");
		sv.put(VARALLA, "På reservplats");
		sv.put(PERUUNTUNUT, "Annullerad");
		sv.put(HYVAKSYTTY, "Godkänd");
		sv.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Godkänd enligt prövning");
		sv.put(PERUNUT, "Annullerad");
		Map<HakemuksenTila, String> en = new HashMap<HakemuksenTila, String>();
		en.put(HYLATTY, "Rejected");
		en.put(VARALLA, "In reserve");
		en.put(PERUUNTUNUT, "Cancelled");
		en.put(HYVAKSYTTY, "Accepted");
		en.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Accepted");
		en.put(PERUNUT, "Canceled");

		kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
		kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
		kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
		return Collections.unmodifiableMap(kielet);
	}

	public static String varasijanNumeroConverter(Integer numero,
			String preferoitukielikoodi) {
		return new StringBuilder().append(VARASIJAT.get(preferoitukielikoodi))
				.append(numero).toString();
	}

	public static String tilaConverter(HakemuksenTila tila,
			String preferoitukielikoodi, boolean harkinnanvarainen) {
		return tilaConverter(tila, preferoitukielikoodi, harkinnanvarainen,
				false, null);
	}

	public static String tilaConverter(IlmoittautumisTila tila,
			String preferoitukielikoodi) {
		if (tila == null) {
			return StringUtils.EMPTY;
		}
		try {
			return ILMOITTAUTUMISTILAT.get(preferoitukielikoodi).get(tila);
		} catch (Exception e) {
			LOG.error(
					"Hakemuksen tila utiliteetilla ei ole konversiota ilmoittautumistilalle: {}",
					tila);
			return tila.toString();
		}
	}

	public static String tilaConverter(HakemuksenTila tila,
			String preferoitukielikoodi, boolean harkinnanvarainen,
			boolean lisaaVarasijanNumero, Integer varasijanNumero) {
		if (tila == null) {
			return StringUtils.EMPTY;
		}
		HakemuksenTila lopullinenTila = tila;
		if (HYVAKSYTTY.equals(tila) && harkinnanvarainen) {
			lopullinenTila = HARKINNANVARAISESTI_HYVAKSYTTY;
		}
		try {
			if (lisaaVarasijanNumero && VARALLA.equals(lopullinenTila)
					&& varasijanNumero != null) {
				return new StringBuilder()
						.append(TILAT.get(preferoitukielikoodi).get(
								lopullinenTila)).append(" (")
						.append(varasijanNumero).append(")").toString();
			} else {
				return TILAT.get(preferoitukielikoodi).get(lopullinenTila);
			}
		} catch (Exception e) {
			LOG.error(
					"Hakemuksen tila utiliteetilla ei ole konversiota hakemustilalle: {}",
					tila);
			return tila.toString();
		}
	}
}
