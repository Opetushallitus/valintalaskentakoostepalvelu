package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HyvaksymiskirjeDTO {
	private final String tarjoajaOid;
	private final String sisalto;
	private final String templateName;
	private final String tag;
	private final String hakukohdeOid;
	private final String hakuOid;
	private final Long sijoitteluajoId;

	public HyvaksymiskirjeDTO(String tarjoajaOid, String sisalto,
			String templateName, String tag, String hakukohdeOid,
			String hakuOid, Long sijoitteluajoId) {
		this.tarjoajaOid = tarjoajaOid;
		this.sisalto = sisalto;
		this.templateName = templateName;
		this.tag = tag;
		this.hakukohdeOid = hakukohdeOid;
		this.hakuOid = hakuOid;
		this.sijoitteluajoId = sijoitteluajoId;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}

	public String getHakuOid() {
		return hakuOid;
	}

	public Long getSijoitteluajoId() {
		return sijoitteluajoId;
	}

	public String getSisalto() {
		return sisalto;
	}

	public String getTag() {
		return tag;
	}

	public String getTarjoajaOid() {
		return tarjoajaOid;
	}

	public String getTemplateName() {
		return templateName;
	}
}
