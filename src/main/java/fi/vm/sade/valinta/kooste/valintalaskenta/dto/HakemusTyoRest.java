package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

public class HakemusTyoRest<A> extends Esitieto<A, HakemusDTO> {

	public HakemusTyoRest(String oid) {
		super(oid);
	}
}
