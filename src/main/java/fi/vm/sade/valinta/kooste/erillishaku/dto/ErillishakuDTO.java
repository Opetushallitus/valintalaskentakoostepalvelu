package fi.vm.sade.valinta.kooste.erillishaku.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuDTO {

	private final String hakuOid;
	private final String hakukohdeOid;
	private final String tarjoajaOid;
	private final String valintatapajonoOid;
	private final Hakutyyppi hakutyyppi;
	public ErillishakuDTO(Hakutyyppi hakutyyppi, String hakuOid, String hakukohdeOid, String tarjoajaOid, String valintatapajonoOid) {
		this.hakutyyppi = hakutyyppi;
		this.hakuOid = hakuOid;
		this.hakukohdeOid = hakukohdeOid;
		this.tarjoajaOid = tarjoajaOid;
		this.valintatapajonoOid = valintatapajonoOid;
	}
	
	public Hakutyyppi getHakutyyppi() {
		return hakutyyppi;
	}
	
	public String getTarjoajaOid() {
		return tarjoajaOid;
	}
	public String getValintatapajonoOid() {
		return valintatapajonoOid;
	}
	public String getHakukohdeOid() {
		return hakukohdeOid;
	}
	public String getHakuOid() {
		return hakuOid;
	}
}
