package fi.vm.sade.valinta.kooste.external.resource.haku.dto;

import java.util.Collection;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakemusPrototyyppiBatch {

	private String hakuOid;
	private String hakukohdeOid;
	private String tarjoajaOid;
	private Collection<HakemusPrototyyppi> hakemusprototyypit;
	
	public HakemusPrototyyppiBatch(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit) {
		this.hakuOid = hakuOid;
		this.hakukohdeOid = hakukohdeOid;
		this.tarjoajaOid = tarjoajaOid;
		this.hakemusprototyypit = hakemusPrototyypit;
	}
	
	public void setHakemusprototyypit(
			Collection<HakemusPrototyyppi> hakemusprototyypit) {
		this.hakemusprototyypit = hakemusprototyypit;
	}
	public void setHakukohdeOid(String hakukohdeOid) {
		this.hakukohdeOid = hakukohdeOid;
	}
	public void setHakuOid(String hakuOid) {
		this.hakuOid = hakuOid;
	}
	public void setTarjoajaOid(String tarjoajaOid) {
		this.tarjoajaOid = tarjoajaOid;
	}
	public Collection<HakemusPrototyyppi> getHakemusprototyypit() {
		return hakemusprototyypit;
	}
	public String getHakukohdeOid() {
		return hakukohdeOid;
	}
	public String getHakuOid() {
		return hakuOid;
	}
	
	public String getTarjoajaOid() {
		return tarjoajaOid;
	}
}
