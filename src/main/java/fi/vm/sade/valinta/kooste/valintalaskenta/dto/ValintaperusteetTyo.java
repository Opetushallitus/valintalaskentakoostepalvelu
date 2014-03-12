package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.List;

import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

public class ValintaperusteetTyo<A> extends
		Esitieto<A, List<ValintaperusteetTyyppi>> {

	public ValintaperusteetTyo(String oid) {
		super(oid);
	}

}
