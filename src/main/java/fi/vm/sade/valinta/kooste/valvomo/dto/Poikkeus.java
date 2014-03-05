package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.Lists;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Poikkeus {

	// tyyppi
	public static final String HAKUOID = "Haku";
	public static final String HAKEMUSOID = "Hakemus";
	public static final String HAKUKOHDEOID = "Hakukohde";
	public static final String VALINTAKOEOID = "Valintakoe";

	public static final String SIJOITTELU = "Sijoittelupalvelu";
	public static final String VALINTATIETO = "Valintatietopalvelu";
	public static final String VIESTINTAPALVELU = "Viestintäpalvelu";
	public static final String DOKUMENTTIPALVELU = "Dokumenttipalvelu";
	public static final String HAKU = "Hakupalvelu";
	public static final String KOODISTO = "Koodistopalvelu";
	public static final String TARJONTA = "Tarjontapalvelu";
	public static final String VALINTALASKENTA = "Valintalaskentapalvelu";
	public static final String KOOSTEPALVELU = "Koostepalvelu";

	private final Collection<Oid> oidit;
	private final String palvelu;
	private final String viesti;
	private final String palvelukutsu;

	public Poikkeus(String palvelu, String palvelukutsu, String viesti,
			Oid... oidit) {
		this.oidit = new CopyOnWriteArrayList<Oid>(oidit);
		this.palvelu = palvelu;
		this.viesti = viesti;
		this.palvelukutsu = palvelukutsu;
	}

	public Poikkeus(String palvelu, String palvelukutsu, String viesti,
			Collection<Oid> oidit) {
		this.oidit = new CopyOnWriteArrayList<Oid>(oidit);
		this.palvelu = palvelu;
		this.viesti = viesti;
		this.palvelukutsu = palvelukutsu;
	}

	public Collection<Oid> getOidit() {
		return oidit;
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

	public static Oid hakuOid(String oid) {
		return new Oid(oid, HAKUOID);
	}

	public static Oid hakukohdeOid(String oid) {
		return new Oid(oid, HAKUKOHDEOID);
	}

	public static Oid valintakoeOid(String oid) {
		return new Oid(oid, VALINTAKOEOID);
	}

	public static Collection<Oid> valintakoeOids(Collection<String> oids) {
		Collection<Oid> o = Lists.newArrayList();
		for (String oid : oids) {
			o.add(new Oid(oid, VALINTAKOEOID));
		}
		return o;
	}

	public static Oid hakemusOid(String oid) {
		return new Oid(oid, HAKEMUSOID);
	}

	public static Collection<Oid> hakemusOids(Collection<String> oids) {
		Collection<Oid> o = Lists.newArrayList();
		for (String oid : oids) {
			o.add(new Oid(oid, HAKEMUSOID));
		}
		return o;
	}

}
