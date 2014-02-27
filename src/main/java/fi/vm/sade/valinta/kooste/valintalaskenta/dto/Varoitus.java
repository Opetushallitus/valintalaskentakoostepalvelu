package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

public class Varoitus {

	private final String oid;
	private final String selite;

	public Varoitus(String oid, String selite) {
		this.oid = oid;
		this.selite = selite;
	}

	public String getOid() {
		return oid;
	}

	public String getSelite() {
		return selite;
	}
}
