package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class NimiJaOpetuskieli {
	private final HakukohdeDTO nimi;
	private final String opetuskieli;

	public NimiJaOpetuskieli(HakukohdeDTO nimi, String opetuskieli) {
		this.nimi = nimi;
		this.opetuskieli = opetuskieli;
	}

	public String getOpetuskieli() {
		return opetuskieli;
	}

	public HakukohdeDTO getNimi() {
		return nimi;
	}

	public Teksti getHakukohdeNimi() {
		return new Teksti(nimi.getHakukohdeNimi());
	}

	public Teksti getTarjoajaNimi() {
		return new Teksti(nimi.getTarjoajaNimi());
	}

}
