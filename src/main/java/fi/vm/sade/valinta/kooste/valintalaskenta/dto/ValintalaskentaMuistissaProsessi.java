package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintalaskentaMuistissaProsessi extends Prosessi {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaProsessi.class);
	private final TyoImpl hakukohteilleHakemukset;
	private final TyoImpl hakemukset;
	private final TyoImpl valintaperusteet;
	private final TyoImpl valintalaskenta;
	private final KokonaisTyo kokonaistyo;
	private final boolean peruutaProsessiPoikkeuksesta;
	private final Collection<Varoitus> varoitukset;

	private List<String> kasitellytHakukohteet = Collections
			.synchronizedList(Lists.<String> newArrayList());

	public ValintalaskentaMuistissaProsessi(String hakuOid) {
		this(hakuOid, new TyoImpl("Valintalaskenta", 0), new TyoImpl(
				"Hakukohteille hakemukset"), new TyoImpl("Hakemukset", 0),
				new TyoImpl("Valintaperusteet", 0));
	}

	public void peruuta() {
		getExceptions().add("Valintalaskenta peruutettu");
	}

	public ValintalaskentaMuistissaProsessi(String hakuOid,
			final TyoImpl valintalaskenta,
			final TyoImpl hakukohteilleHakemukset, final TyoImpl hakemukset,
			final TyoImpl valintaperusteet) {
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

	public TyoImpl getValintaperusteet() {
		return valintaperusteet;
	}

	public TyoImpl getHakukohteilleHakemukset() {
		return hakukohteilleHakemukset;
	}

	public TyoImpl getHakemukset() {
		return hakemukset;
	}

	public KokonaisTyo getKokonaistyo() {
		return kokonaistyo;
	}

	public TyoImpl getValintalaskenta() {
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
