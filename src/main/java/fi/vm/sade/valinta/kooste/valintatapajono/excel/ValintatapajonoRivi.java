package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.math.BigDecimal;
import java.util.Map;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;

/**
 * @author Jussi Jartamo
 */
@ApiModel
public class ValintatapajonoRivi {
	private final String oid;
	private final String nimi;

	@ApiModelProperty(allowableValues = "MAARITTELEMATON,HYVAKSYTTAVISSA,HYLATTY,HYVAKSYTTY_HARKINNANVARAISESTI")
	private final String tila;
	private final boolean validi;
	private final JarjestyskriteerituloksenTila kriteeriTila;
	private final Map<String, String> kuvaus;
	private final int jonosija;
	private final String virhe;

	public ValintatapajonoRivi(String oid, String jonosija, String nimi,
	//
			String tila, String fi, String sv, String en) {
		StringBuilder defaultVirhe = new StringBuilder();
		this.oid = oid;
		this.nimi = nimi;
		this.kuvaus = Maps.newHashMap();
		if (!StringUtils.isBlank(fi)) {
			kuvaus.put(KieliUtil.SUOMI, fi);
		}
		if (!StringUtils.isBlank(sv)) {
			kuvaus.put(KieliUtil.RUOTSI, sv);
		}
		if (!StringUtils.isBlank(en)) {
			kuvaus.put(KieliUtil.ENGLANTI, en);
		}
		this.tila = tila;
		boolean errors = false;
		JarjestyskriteerituloksenTila defaultTila = JarjestyskriteerituloksenTila.HYLATTY;
		if (ValintatapajonoExcel.HYLATTY.equals(tila) || ValintatapajonoExcel.VAKIO_HYLATTY.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYLATTY;
		} else if (ValintatapajonoExcel.HYVAKSYTTAVISSA.equals(tila) || ValintatapajonoExcel.VAKIO_HYVAKSYTTAVISSA.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
		} else if (ValintatapajonoExcel.HYVAKSYTTY_HARKINNANVARAISESTI.equals(tila) || ValintatapajonoExcel.VAKIO_HYVAKSYTTY_HARKINNANVARAISESTI.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTY_HARKINNANVARAISESTI;
		} else {
			defaultVirhe.append("Tuntematon tila ").append(tila).append(".");
			errors = true;
		}

		int defaultJonosija = 0;
		try {
			if (StringUtils.isBlank(jonosija)) {
				if (JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA
						.equals(defaultTila)) {
					errors = true;
					defaultVirhe
							.append(" Tyhjä jonosija vaikka hakija on hyväksyttävissä.");
				}
			} else {
				defaultJonosija = new BigDecimal(jonosija).toBigInteger()
						.intValue();
				if (!JarjestyskriteerituloksenTila.HYLATTY.equals(defaultTila)) {
					defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
			errors = true;
			defaultVirhe.append(" Jonosijaa ").append(jonosija)
					.append(" ei voi muuttaa numeroksi.");

		}
		this.kriteeriTila = defaultTila;
		this.jonosija = defaultJonosija;
		this.validi = !errors
				|| defaultTila
						.equals(JarjestyskriteerituloksenTila.MAARITTELEMATON);
		this.virhe = defaultVirhe.toString().trim();
	}

	public JarjestyskriteerituloksenTila asTila() {
		return kriteeriTila;
	}

	public String getTila() {
		return tila;
	}

	public int getJonosija() {
		return jonosija;
	}

	public String getVirhe() {
		return virhe;
	}

	public boolean isValidi() {
		return validi;
	}

	public Map<String, String> getKuvaus() {
		return kuvaus;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOid() {
		return oid;
	}
}
