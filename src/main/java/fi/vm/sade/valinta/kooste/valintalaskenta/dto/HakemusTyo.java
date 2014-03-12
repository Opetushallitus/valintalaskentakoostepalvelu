package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;

public class HakemusTyo<A> extends Esitieto<A, HakemusTyyppi> {

	public HakemusTyo(String oid) {
		super(oid);
	}
}
