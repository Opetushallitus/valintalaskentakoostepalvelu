package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface KoekutsuProsessi {

	void vaiheValmistui();

	void valmistui(String dokumenttiId);

	void keskeyta();
}
