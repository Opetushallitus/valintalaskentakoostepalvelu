package fi.vm.sade.valinta.kooste.erillishaku.service;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ErillishaunVientiService {
	
	void vie(KirjeProsessi prosessi, ErillishakuDTO erillishaku);
}
