package fi.vm.sade.valinta.kooste.erillishaku.service;

import java.io.InputStream;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ErillishaunTuontiService {

	void tuo(ErillishakuDTO erillishaku, InputStream data);
}
