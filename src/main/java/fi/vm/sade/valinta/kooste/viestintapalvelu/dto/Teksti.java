package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import static fi.vm.sade.valinta.kooste.util.KieliUtil.ENGLANTI;
import static fi.vm.sade.valinta.kooste.util.KieliUtil.RUOTSI;
import static fi.vm.sade.valinta.kooste.util.KieliUtil.SUOMI;
import static fi.vm.sade.valinta.kooste.util.KieliUtil.normalisoiKielikoodi;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.Maps;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Vikasietoinen lokalisoituteksti DTO. Palauttaa aina jotain.
 */
public class Teksti {

	private final static String EI_ARVOA = "<< ei arvoa >>";
	private final TreeMap<String, String> normalisoituKieliJaKoodi;

	public Teksti(Map<String, String> kieletJaKoodit) {
		this.normalisoituKieliJaKoodi = Maps.newTreeMap();
		if (kieletJaKoodit != null && !kieletJaKoodit.isEmpty()) {
			for (Entry<String, String> kk : kieletJaKoodit.entrySet()) {
				if (kk.getKey() == null || kk.getValue() == null) {
					// EI LISATA ARVOTONTA KOODIA
					// normalisoituKieliJaKoodi.put(normalisoiKielikoodi(kk.getKey()),
					// EI_ARVOA);
				} else {
					normalisoituKieliJaKoodi.put(
							normalisoiKielikoodi(kk.getKey()), kk.getValue());
				}
			}
		}
	}

	public boolean isArvoton() {
		return normalisoituKieliJaKoodi.isEmpty();
	}

	public Teksti(String suomenkielinenTeksti) {
		this(asMap(SUOMI, suomenkielinenTeksti));
	}

	public String getKieli() {
		if (normalisoituKieliJaKoodi.containsKey(SUOMI)) {
			return SUOMI;
		} else if (normalisoituKieliJaKoodi.containsKey(RUOTSI)) {
			return RUOTSI;
		} else {
			return ENGLANTI;
		}
	}

	/**
	 * 
	 * @param normalisoituKielikoodi
	 * @param oletusarvo
	 *            jos arvoa ei loydy niin palautetaan oletusarvo
	 * @return
	 */
	public String getTeksti(String normalisoituKielikoodi, String oletusarvo) {
		if (isArvoton()) {
			return oletusarvo;
		}
		if (normalisoituKieliJaKoodi.containsKey(normalisoituKielikoodi)) {
			return normalisoituKieliJaKoodi.get(normalisoituKielikoodi);
		}
		return getTeksti();
	}

	/**
	 * 
	 * @param normalisoituKielikoodi
	 *            normalisoitu KieliUtil:n normalisoikielikoodi-metodilla
	 *            ("FI","SE","EN")
	 * @return
	 */
	public String getTeksti(String normalisoituKielikoodi) {
		return getTeksti(normalisoituKielikoodi, EI_ARVOA);
	}

	/**
	 * @return preferoi suomea
	 */
	public String getTeksti() {
		if (normalisoituKieliJaKoodi.containsKey(SUOMI)) {
			return normalisoituKieliJaKoodi.get(SUOMI);
		} else if (normalisoituKieliJaKoodi.containsKey(RUOTSI)) {
			return normalisoituKieliJaKoodi.get(RUOTSI);
		} else {
			return normalisoituKieliJaKoodi.firstEntry().getValue();
		}
	}

	private static TreeMap<String, String> asMap(String key, String value) {
		TreeMap<String, String> m = Maps.newTreeMap();
		m.put(key, value);
		return m;
	}
}
