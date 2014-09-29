package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

public interface Haku {

	HakuV1RDTO getAsTarjontaHakuDTO();

	boolean isKorkeakouluhaku();

	boolean isLisahaku();
}
