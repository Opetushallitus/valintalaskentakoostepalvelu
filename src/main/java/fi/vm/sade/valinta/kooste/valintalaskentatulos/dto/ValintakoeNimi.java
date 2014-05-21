package fi.vm.sade.valinta.kooste.valintalaskentatulos.dto;

public class ValintakoeNimi {

	private final String nimi;
	private final String oid;

	public ValintakoeNimi(String nimi, String oid) {
		this.nimi = nimi;
		this.oid = oid;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOid() {
		return oid;
	}

}
