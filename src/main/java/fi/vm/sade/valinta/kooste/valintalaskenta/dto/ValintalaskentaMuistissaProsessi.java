package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.valvomo.dto.KokonaisTyo;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.OsaTyo;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintalaskentaMuistissaProsessi extends Prosessi {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaProsessi.class);
	private final OsaTyo hakukohteilleHakemukset;
	private final OsaTyo hakemukset;
	private final OsaTyo valintaperusteet;
	private final OsaTyo valintalaskenta;
	private final KokonaisTyo kokonaistyo;
	private final boolean peruutaProsessiPoikkeuksesta;
	private final Collection<Varoitus> varoitukset;

	private List<String> kasitellytHakukohteet = Collections
			.synchronizedList(Lists.<String> newArrayList());

	public ValintalaskentaMuistissaProsessi(String hakuOid) {
		this(hakuOid, new OsaTyo("Valintalaskenta", 0), new OsaTyo(
				"Hakukohteille hakemukset"), new OsaTyo("Hakemukset", 0),
				new OsaTyo("Valintaperusteet", 0));
	}

	public void peruuta() {
		getExceptions().add("Valintalaskenta peruutettu");
	}

	public ValintalaskentaMuistissaProsessi(String hakuOid,
			final OsaTyo valintalaskenta,
			final OsaTyo hakukohteilleHakemukset, final OsaTyo hakemukset,
			final OsaTyo valintaperusteet) {
		super("Valintalaskentamuistissa", "Haulle", hakuOid);
		this.varoitukset = Collections.synchronizedList(Lists
				.<Varoitus> newArrayList());
		this.peruutaProsessiPoikkeuksesta = true;
		this.valintalaskenta = valintalaskenta;
		this.hakukohteilleHakemukset = hakukohteilleHakemukset;
		this.hakemukset = hakemukset;
		this.valintaperusteet = valintaperusteet;
		this.kokonaistyo = new KokonaisTyo(Arrays.asList(valintalaskenta,
				valintaperusteet, hakukohteilleHakemukset, hakemukset));

	}

	public Collection<Varoitus> getVaroitukset() {
		return varoitukset;
	}

	public OsaTyo getValintaperusteet() {
		return valintaperusteet;
	}

	public OsaTyo getHakukohteilleHakemukset() {
		return hakukohteilleHakemukset;
	}

	public OsaTyo getHakemukset() {
		return hakemukset;
	}

	public KokonaisTyo getKokonaistyo() {
		return kokonaistyo;
	}

	public OsaTyo getValintalaskenta() {
		return valintalaskenta;
	}

	@com.fasterxml.jackson.annotation.JsonIgnore
	public List<String> getKasitellytHakukohteet() {
		return kasitellytHakukohteet;
	}

	public boolean hasPoikkeuksia() {
		if (peruutaProsessiPoikkeuksesta) {
			return !getExceptions().isEmpty();// !kokonaistyo.getPoikkeukset().isEmpty();
		} else {
			return false;
		}
	}

	public boolean isPeruutaProsessiPoikkeuksesta() {
		return peruutaProsessiPoikkeuksesta;
	}
}
