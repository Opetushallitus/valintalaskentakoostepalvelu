package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;

public class ValintatapajonoRivi {
	private final String oid;
	private final String nimi;
	private final String tila;
	private final boolean validi;
	private final JarjestyskriteerituloksenTila kriteeriTila;
	private final Map<String, String> kuvaus;
	private final int jonosija;

	public ValintatapajonoRivi(String oid, String jonosija, String nimi,
	//
			String tila, String fi, String sv, String en) {
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
		JarjestyskriteerituloksenTila defaultTila = JarjestyskriteerituloksenTila.MAARITTELEMATON;
		if (ValintatapajonoExcel.HYLATTY.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYLATTY;
		} else if (ValintatapajonoExcel.HYVAKSYTTAVISSA.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA;
		} else if (ValintatapajonoExcel.MAARITTELEMATON.equals(tila)) {
			defaultTila = JarjestyskriteerituloksenTila.MAARITTELEMATON;
		} else {
			errors = true;
		}
		this.kriteeriTila = defaultTila;

		int defaultJonosija = 0;
		try {
			defaultJonosija = Integer.parseInt(jonosija);

		} catch (Exception e) {
			errors = true;

		}
		this.jonosija = defaultJonosija;
		this.validi = errors;
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
