package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;

import com.google.common.collect.Lists;

public class RiviBuilder {

	private Collection<Rivi> rivit;

	public RiviBuilder() {
		this.rivit = Lists.newArrayList();
	}

	public RiviBuilder addTyhja() {
		rivit.add(new Rivi(Teksti.tyhja()));
		return this;
	}

	public RiviBuilder addSolu(Solu solu) {
		rivit.add(new Rivi(solu));
		return this;
	}

	public RiviBuilder addOid(String oid) {
		rivit.add(new OidRivi(oid));
		return this;
	}

	public RiviBuilder addRivi(Rivi rivi) {
		rivit.add(rivi);
		return this;
	}

	public RiviBuilder addOid(String... oidit) {
		rivit.add(new OidRivi(Lists.newArrayList(oidit)));
		return this;
	}

	public RiviBuilder addTeksti(String teksti) {
		rivit.add(new Rivi(new Teksti(teksti)));
		return this;
	}

	public RiviBuilder addTeksti(String teksti, int ulottuvuus) {
		rivit.add(new Rivi(new Teksti(teksti, ulottuvuus)));
		return this;
	}

	public Rivi build() {
		return new Kooste(rivit);
	}

	public Rivi build(boolean nakyvissa) {
		return new Kooste(rivit, nakyvissa);
	}
}
