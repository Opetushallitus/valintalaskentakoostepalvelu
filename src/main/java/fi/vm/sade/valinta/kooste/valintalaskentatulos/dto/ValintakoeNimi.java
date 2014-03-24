package fi.vm.sade.valinta.kooste.valintalaskentatulos.dto;

public class ValintakoeNimi {

	private final String tunniste;
	private final String nimi;

	public ValintakoeNimi(String nimi, String tunniste) {
		this.nimi = nimi;
		this.tunniste = tunniste;
	}

	public String getNimi() {
		return nimi;
	}

	public String getTunniste() {
		return tunniste;
	}

}
