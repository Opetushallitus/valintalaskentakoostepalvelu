package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.util.KieliUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class JalkiohjauskirjeDTO {
	private final String tarjoajaOid;
	private final String sisalto;
	private final String templateName;
	private final String tag;
	private final String hakuOid;
	private final String kielikoodi;

	public JalkiohjauskirjeDTO(String tarjoajaOid, String sisalto,
			String templateName, String tag, String hakuOid, String kielikoodi) {
		this.tarjoajaOid = tarjoajaOid;
		this.sisalto = sisalto;
		this.templateName = templateName;
		this.tag = tag;
		this.hakuOid = hakuOid;
		this.kielikoodi = kielikoodi;
	}

	public boolean isRuotsinkielinenAineisto() {
		return KieliUtil.RUOTSI.equals(kielikoodi);
	}

	public String getKielikoodi() {
		return kielikoodi;
	}

	public String getHakuOid() {
		return hakuOid;
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
		if (templateName == null) {
			return "jalkiohjauskirje";
		}
		return templateName;
	}
}
