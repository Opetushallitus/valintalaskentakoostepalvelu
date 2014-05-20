package fi.vm.sade.valinta.kooste.function;

import com.google.common.base.Function;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SuppeaHakemusFunction implements Function<SuppeaHakemus, String> {

	public static final SuppeaHakemusFunction TO_HAKEMUS_OIDS = new SuppeaHakemusFunction();

	private SuppeaHakemusFunction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String apply(SuppeaHakemus input) {
		return input.getOid();
	}

}
