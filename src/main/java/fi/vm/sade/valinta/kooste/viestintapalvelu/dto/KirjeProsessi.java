package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

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

	boolean isKeskeytetty();
}
