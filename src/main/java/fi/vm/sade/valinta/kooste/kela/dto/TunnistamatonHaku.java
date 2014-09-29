package fi.vm.sade.valinta.kooste.kela.dto;

import org.apache.poi.ss.formula.eval.NotImplementedException;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class TunnistamatonHaku implements Haku {

	private final HakuV1RDTO haku;

	public TunnistamatonHaku(HakuV1RDTO haku) {
		this.haku = haku;
	}

	@Override
	public HakuV1RDTO getAsTarjontaHakuDTO() {
		return this.haku;
	}

	@Override
	public boolean isKorkeakouluhaku() {
		throw new NotImplementedException(
				"Tunnistamaton haku joten ei voida sanoa onko kk-haku!");
	}

	@Override
	public boolean isLisahaku() {
		throw new NotImplementedException(
				"Tunnistamaton haku joten ei voida sanoa onko lisahaku!");
	}
}
