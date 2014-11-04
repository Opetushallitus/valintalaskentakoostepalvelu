package fi.vm.sade.valinta.kooste.erillishaku.service;

import java.io.InputStream;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ErillishaunTuontiService {

	void tuo(KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data);
}
