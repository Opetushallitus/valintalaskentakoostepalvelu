package fi.vm.sade.valinta.kooste.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
	private Map<String, String> lisatiedot = null;
	private Map<String, String> hakutoiveet = null;
	private final static String ETUNIMET = "Etunimet";
	private final static String KUTSUMANIMI = "Kutsumanimi";
	private final static String SUKUNIMI = "Sukunimi";
	private final static String ASIOINTIKIELI = "asiointikieli";
	private final static String LUPAJULKAISUUN = "lupaJulkaisu";
	private final static String HETU = "Henkilotunnus";
	private final static String SAHKOPOSTI = "Sähköposti";
	private final static String SYNTYMAAIKA = "syntymaaika";

	public HakemusWrapper(Hakemus hakemus) {
		this.hakemus = hakemus;
	}

	public String getSahkopostiOsoite() {
		getHenkilotiedot();
		return Optional.ofNullable(henkilotiedot.get(SAHKOPOSTI)).orElse(
				StringUtils.EMPTY);
	}

	public String getHenkilotunnusTaiSyntymaaika() {
		getHenkilotiedot();
		return Optional.ofNullable(henkilotiedot.get(HETU)).orElse(
				Optional.ofNullable(henkilotiedot.get(SYNTYMAAIKA)).orElse(
						StringUtils.EMPTY));
	}

	public String getHenkilotunnus() {
		getHenkilotiedot();
		return Optional.ofNullable(henkilotiedot.get(HETU)).orElse(
				StringUtils.EMPTY);
	}

	public Integer getHakutoiveenPrioriteetti(String hakukohdeOid) {
		getHakutoiveet();

		if (hakutoiveet.containsValue(hakukohdeOid)) {
			for (Entry<String, String> s : hakutoiveet.entrySet()) {
				if (hakukohdeOid.equals(s.getValue())) {
					try {
						String value = s.getKey().split("preference")[1]
								.split("-")[0];
						return Integer.parseInt(value);
					} catch (Exception e) {

					}
				}
			}
		}
		return null;
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

	public boolean getLupaJulkaisuun() {
		getLisatiedot(); // lazy load henkilotiedot
		if (lisatiedot.containsKey(LUPAJULKAISUUN)) {
			String l = lisatiedot.get(LUPAJULKAISUUN);
			try {
				return Boolean.TRUE.equals(Boolean.valueOf(l));
			} catch (Exception e) {
			}
		}
		return false;
	}

	public String getAsiointikieli() {
		getLisatiedot(); // lazy load henkilotiedot
		if (lisatiedot.containsKey(ASIOINTIKIELI)) {
			return KieliUtil
					.normalisoiKielikoodi(lisatiedot.get(ASIOINTIKIELI));
		} else {
			return KieliUtil.SUOMI;
		}
	}

	public Map<String, String> getLisatiedot() {
		if (lisatiedot == null) {
			lisatiedot = // hakemus.getAnswers().getHenkilotiedot();
			new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			try {
				lisatiedot.putAll(hakemus.getAnswers().getLisatiedot());
			} catch (Exception e) {

			}
		}
		return lisatiedot;
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

	public Map<String, String> getHakutoiveet() {
		if (hakutoiveet == null) {
			hakutoiveet = // hakemus.getAnswers().getHenkilotiedot();
			new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			try {
				hakutoiveet.putAll(hakemus.getAnswers().getHakutoiveet());
			} catch (Exception e) {

			}
		}
		return hakutoiveet;
	}
}
