package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

import java.util.List;

public class ValintaperusteetTyoRest<A> extends
		Esitieto<A, List<ValintaperusteetDTO>> {

	public ValintaperusteetTyoRest(String oid) {
		super(oid);
	}

}
