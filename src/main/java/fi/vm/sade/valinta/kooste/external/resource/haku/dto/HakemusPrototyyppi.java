package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakemusPrototyyppi {

	private String hakuOid;
	private String hakukohdeOid;
	private String hakijaOid;
	private String etunimi;
	private String sukunimi;
	private String henkilotunnus;
	private String syntymaAika;
	
	public void setEtunimi(String etunimi) {
		this.etunimi = etunimi;
	}
	public void setHakijaOid(String hakijaOid) {
		this.hakijaOid = hakijaOid;
	}
	public void setHakukohdeOid(String hakukohdeOid) {
		this.hakukohdeOid = hakukohdeOid;
	}
	public void setHakuOid(String hakuOid) {
		this.hakuOid = hakuOid;
	}
	public void setHenkilotunnus(String henkilotunnus) {
		this.henkilotunnus = henkilotunnus;
	}
	public void setSukunimi(String sukunimi) {
		this.sukunimi = sukunimi;
	}
	public void setSyntymaAika(String syntymaAika) {
		this.syntymaAika = syntymaAika;
	}
	
	public String getEtunimi() {
		return etunimi;
	}
	public String getHakijaOid() {
		return hakijaOid;
	}
	public String getHakukohdeOid() {
		return hakukohdeOid;
	}
	public String getHakuOid() {
		return hakuOid;
	}
	public String getHenkilotunnus() {
		return henkilotunnus;
	}
	public String getSukunimi() {
		return sukunimi;
	}
	public String getSyntymaAika() {
		return syntymaAika;
	}
	
}
