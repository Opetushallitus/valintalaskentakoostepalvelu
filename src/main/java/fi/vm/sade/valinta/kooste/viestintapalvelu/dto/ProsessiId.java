package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Paluuviesti prosessiId:n välittämiseen käyttöliittymään
 */
public class ProsessiId {

	private final String id;

	public ProsessiId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
