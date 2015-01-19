package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Poikkeus {

	// tyyppi
	public static final String HAKUOID = "Haku";
	public static final String KOODISTOURI = "Koodisto URI";
	public static final String HAKEMUSOID = "Hakemus";
	public static final String HAKUKOHDEOID = "Hakukohde";
	public static final String VALINTAKOEOID = "Valintakoe";

	public static final String SIJOITTELU = "Sijoittelupalvelu";
	public static final String VALINTATIETO = "Valintatietopalvelu";
	public static final String VALINTAKOELASKENTA = "Valintakoelaskenta";
	public static final String VALINTAPERUSTEET = "Valintaperustepalvelu";
	public static final String VIESTINTAPALVELU = "Viestintäpalvelu";
	public static final String DOKUMENTTIPALVELU = "Dokumenttipalvelu";
	public static final String HAKU = "Hakupalvelu";
	public static final String HAKEMUSPALVELU = "Hakemuspalvelu";
	public static final String HENKILOPALVELU = "Henkilöpalvelu";
	public static final String KOODISTO = "Koodistopalvelu";
	public static final String TARJONTA = "Tarjontapalvelu";
	public static final String VALINTALASKENTA = "Valintalaskentapalvelu";
	public static final String KOOSTEPALVELU = "Koostepalvelu";

	private final Collection<Tunniste> tunnisteet;
	private final String palvelu;
	private final String viesti;
	private final String palvelukutsu;

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Poikkeus) {
			Poikkeus p = (Poikkeus)obj;
			return
					Optional.ofNullable(this.palvelukutsu).orElse(StringUtils.EMPTY).equals(p.getPalvelukutsu())
							//
							&& Optional.ofNullable(this.viesti).orElse(StringUtils.EMPTY).equals(p.getViesti())
							//
							&& Optional.ofNullable(this.palvelu).orElse(StringUtils.EMPTY).equals(p.getPalvelu()) &&
							//
							Optional.ofNullable(this.tunnisteet).orElse(Collections.emptyList()).equals(p.getTunnisteet());
		} else {
			return false;
		}
	}

	public Poikkeus() {
		tunnisteet= null;
		palvelu = null;
		viesti = null;
		palvelukutsu = null;
	}

	public Poikkeus(String palvelu, String palvelukutsu, Tunniste... tunnisteet) {
		this.tunnisteet = new CopyOnWriteArrayList<Tunniste>(tunnisteet);
		this.palvelu = palvelu;
		this.viesti = "";
		this.palvelukutsu = palvelukutsu;
	}

	public Poikkeus(String palvelu, String palvelukutsu, String viesti,
			Tunniste... tunnisteet) {
		this.tunnisteet = new CopyOnWriteArrayList<Tunniste>(tunnisteet);
		this.palvelu = palvelu;
		this.viesti = viesti;
		this.palvelukutsu = palvelukutsu;
	}

	public Poikkeus(String palvelu, String palvelukutsu, String viesti,
			Collection<Tunniste> tunnisteet) {
		this.tunnisteet = new CopyOnWriteArrayList<Tunniste>(tunnisteet);
		this.palvelu = palvelu;
		this.viesti = viesti;
		this.palvelukutsu = palvelukutsu;
	}

	public static Poikkeus koostepalvelupoikkeus(String syy) {
		return new Poikkeus(KOOSTEPALVELU, StringUtils.EMPTY,syy);
	}
	public static Poikkeus koostepalvelupoikkeus(String syy, Collection<Tunniste> oids) {
		return new Poikkeus(KOOSTEPALVELU, StringUtils.EMPTY,syy, oids);
	}
	public static Poikkeus hakemuspalvelupoikkeus(String syy) {
		return new Poikkeus(HAKEMUSPALVELU, StringUtils.EMPTY,syy);
	}
	public static Poikkeus henkilopalvelupoikkeus(String syy) {
		return new Poikkeus(HENKILOPALVELU, StringUtils.EMPTY,syy);
	}
	public Collection<Tunniste> getTunnisteet() {
		return tunnisteet;
	}

	public String getPalvelukutsu() {
		return palvelukutsu;
	}

	public String getPalvelu() {
		return palvelu;
	}

	public String getViesti() {
		return viesti;
	}

	public static Tunniste hakuOid(String oid) {
		return new Tunniste(oid, HAKUOID);
	}

	public static Tunniste hakukohdeOid(String oid) {
		return new Tunniste(oid, HAKUKOHDEOID);
	}

	public static Tunniste valintakoeOid(String oid) {
		return new Tunniste(oid, VALINTAKOEOID);
	}

	public static Collection<Tunniste> valintakoeOids(Collection<String> oids) {
		Collection<Tunniste> o = Lists.newArrayList();
		for (String oid : oids) {
			o.add(new Tunniste(oid, VALINTAKOEOID));
		}
		return o;
	}

	public static Tunniste hakemusOid(String oid) {
		return new Tunniste(oid, HAKEMUSOID);
	}

	public static Collection<Tunniste> hakemusOids(Collection<String> oids) {
		Collection<Tunniste> o = Lists.newArrayList();
		for (String oid : oids) {
			o.add(new Tunniste(oid, HAKEMUSOID));
		}
		return o;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append("[Palvelu:").append(palvelu).append(",viesti:").append(viesti);
		for(Tunniste tunniste : tunnisteet) {
			sb.append("\r\n\t").append(tunniste);
		}
		return sb.append("]").toString();
	}
}
