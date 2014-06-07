package fi.vm.sade.valinta.kooste.sijoitteluntulos.dto;

public class Valmis {

	private final String tarjoajaOid;
	private final String tulosId;
	private final String hakukohdeOid;

	public Valmis(String hakukohdeOid, String tarjoajaOid, String tulosId) {
		this.hakukohdeOid = hakukohdeOid;
		this.tarjoajaOid = tarjoajaOid;
		this.tulosId = tulosId;
	}

	public boolean isOnnistunut() {
		return tulosId != null;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}

	public String getTarjoajaOid() {
		return tarjoajaOid;
	}

	public String getTulosId() {
		return tulosId;
	}
}
