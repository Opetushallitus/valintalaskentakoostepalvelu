package fi.vm.sade.valinta.kooste.util;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Hakemustietojen luku hakemustietueesta vikasietoisesti
 */
public class HakemusWrapper {

	private final Hakemus hakemus;
	private Map<String, String> henkilotiedot = null;
	private final static String ETUNIMET = "Etunimet";
	private final static String KUTSUMANIMI = "Kutsumanimi";
	private final static String SUKUNIMI = "Sukunimi";
	private final static String ASIOINTIKIELI = "asiointikieli";

	public HakemusWrapper(Hakemus hakemus) {
		this.hakemus = hakemus;
	}

	public String getEtunimi() {
		getHenkilotiedot(); // lazy load henkilotiedot
		if (henkilotiedot.containsKey(KUTSUMANIMI)) {
			return henkilotiedot.get(KUTSUMANIMI);
		} else if (henkilotiedot.containsKey(ETUNIMET)) {
			return henkilotiedot.get(ETUNIMET);
		} else {
			return StringUtils.EMPTY;
		}
	}

	public String getSukunimi() {
		getHenkilotiedot(); // lazy load henkilotiedot
		if (henkilotiedot.containsKey(SUKUNIMI)) {
			return henkilotiedot.get(SUKUNIMI);
		} else {
			return StringUtils.EMPTY;
		}
	}

	public String getAsiointikieli() {
		getHenkilotiedot(); // lazy load henkilotiedot
		if (henkilotiedot.containsKey(ASIOINTIKIELI)) {
			return KieliUtil.normalisoiKielikoodi(henkilotiedot
					.get(ASIOINTIKIELI));
		} else {
			return KieliUtil.SUOMI;
		}
	}

	public Map<String, String> getHenkilotiedot() {
		if (henkilotiedot == null) {
			henkilotiedot = // hakemus.getAnswers().getHenkilotiedot();
			new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			try {
				henkilotiedot.putAll(hakemus.getAnswers().getHenkilotiedot());
			} catch (Exception e) {

			}
		}
		return henkilotiedot;
	}
}
