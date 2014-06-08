package fi.vm.sade.valinta.kooste.sijoitteluntulos.dto;

public class Valmis {

	private final String tarjoajaOid;
	private final String tulosId;
	private final String hakukohdeOid;
	private final boolean eiTuloksia;

	public Valmis(String hakukohdeOid, String tarjoajaOid, String tulosId) {
		this.hakukohdeOid = hakukohdeOid;
		this.tarjoajaOid = tarjoajaOid;
		this.tulosId = tulosId;
		this.eiTuloksia = false;
	}

	public Valmis(String hakukohdeOid, String tarjoajaOid, String tulosId,
			boolean eiTuloksia) {
		this.hakukohdeOid = hakukohdeOid;
		this.tarjoajaOid = tarjoajaOid;
		this.tulosId = tulosId;
		this.eiTuloksia = eiTuloksia;
	}

	public boolean isEiTuloksia() {
		return eiTuloksia;
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
