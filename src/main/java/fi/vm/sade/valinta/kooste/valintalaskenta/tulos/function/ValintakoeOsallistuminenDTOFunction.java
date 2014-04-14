package fi.vm.sade.valinta.kooste.valintalaskenta.tulos.function;

import com.google.common.base.Function;

import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class ValintakoeOsallistuminenDTOFunction implements
		Function<ValintakoeOsallistuminenDTO, String> {

	public static final ValintakoeOsallistuminenDTOFunction TO_HAKEMUS_OIDS = new ValintakoeOsallistuminenDTOFunction();

	private ValintakoeOsallistuminenDTOFunction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String apply(ValintakoeOsallistuminenDTO input) {
		return input.getHakemusOid();
	}
}
