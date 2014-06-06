package fi.vm.sade.valinta.kooste.util;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUNTUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;

public class HakemusUtil {

	private static final Logger LOG = LoggerFactory
			.getLogger(HakemusUtil.class);
	private final static Map<String, Map<HakemuksenTila, String>> TILAT = valmistaTilat();
	private final static Map<String, String> VARASIJAT = varasijaTekstinTilat();
	public static final String ASIOINTIKIELI = "asiointikieli";

	private static Map<String, String> varasijaTekstinTilat() {
		Map<String, String> varasijaTekstinTilat = Maps.newHashMap();
		varasijaTekstinTilat.put(KieliUtil.SUOMI, "Varasijan numero on ");
		varasijaTekstinTilat.put(KieliUtil.RUOTSI, "Reservplatsens nummer är ");
		varasijaTekstinTilat.put(KieliUtil.ENGLANTI, "Waiting list number is ");
		return varasijaTekstinTilat;
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
		fi.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Godkänd enligt prövning");
		sv.put(PERUNUT, "Annullerad");
		Map<HakemuksenTila, String> en = new HashMap<HakemuksenTila, String>();
		en.put(HYLATTY, "Rejected");
		en.put(VARALLA, "In reserve");
		en.put(PERUUNTUNUT, "Cancelled");
		en.put(HYVAKSYTTY, "Accepted");
		fi.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Accepted");
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
		HakemuksenTila lopullinenTila = tila;
		if (HYVAKSYTTY.equals(tila) && harkinnanvarainen) {
			lopullinenTila = HARKINNANVARAISESTI_HYVAKSYTTY;
		}
		try {
			return TILAT.get(preferoitukielikoodi).get(lopullinenTila);
		} catch (Exception e) {
			LOG.error(
					"Hakemuksen tila utiliteetilla ei ole konversiota enumille: {}",
					tila);
			return tila.toString();
		}
	}
}
