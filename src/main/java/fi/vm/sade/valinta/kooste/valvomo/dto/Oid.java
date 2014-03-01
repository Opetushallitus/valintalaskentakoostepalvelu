package fi.vm.sade.valinta.kooste.valvomo.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Oid {

	private final String oid;
	private final String tyyppi;

	public Oid(String oid, String tyyppi) {
		this.oid = oid;
		this.tyyppi = tyyppi;
	}

	public String getOid() {
		return oid;
	}

	public String getTyyppi() {
		return tyyppi;
	}
}
