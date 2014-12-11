package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.Map;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface KirjeProsessi {

	void vaiheValmistui();

	void valmistui(String dokumenttiId);

	void keskeyta();
	
	void keskeyta(String syy);

    void keskeyta(String syy, Map<String, String> virheet);

	boolean isKeskeytetty();
}
