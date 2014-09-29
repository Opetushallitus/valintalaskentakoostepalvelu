package fi.vm.sade.valinta.kooste.kela.dto;

import org.apache.poi.ss.formula.eval.NotImplementedException;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class TunnistettuHaku implements Haku {
	private final HakuV1RDTO haku;
	private final boolean kkhaku;
	private final boolean lisahaku;

	public TunnistettuHaku(HakuV1RDTO haku, boolean kkhaku, boolean lisahaku) {
		this.haku = haku;
		this.lisahaku = lisahaku;
		this.kkhaku = kkhaku;
	}

	@Override
	public HakuV1RDTO getAsTarjontaHakuDTO() {
		return this.haku;
	}

	@Override
	public boolean isKorkeakouluhaku() {
		return kkhaku;
	}

	@Override
	public boolean isLisahaku() {
		return lisahaku;
	}
}
