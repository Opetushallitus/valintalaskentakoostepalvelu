package fi.vm.sade.valinta.kooste.erillishaku.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ErillishakuDTO {

	private final String hakuOid;
	private final String hakukohdeOid;
	
	public ErillishakuDTO(String hakuOid, String hakukohdeOid) {
		this.hakuOid = hakuOid;
		this.hakukohdeOid = hakukohdeOid;
	}
	
	public String getHakukohdeOid() {
		return hakukohdeOid;
	}
	public String getHakuOid() {
		return hakuOid;
	}
}
