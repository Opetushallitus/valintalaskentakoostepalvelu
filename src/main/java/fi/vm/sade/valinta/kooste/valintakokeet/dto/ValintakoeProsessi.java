package fi.vm.sade.valinta.kooste.valintakokeet.dto;

import java.util.Arrays;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintakoeProsessi extends DokumenttiProsessi {

	public ValintakoeProsessi(String hakuOid) {
		super("Valintakoelaskenta",
				"Muistinvarainen valintakoelaskenta haulle", hakuOid, Arrays
						.asList("valintakoelaskenta"));
	}
}
