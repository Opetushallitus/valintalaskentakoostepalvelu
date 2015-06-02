package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.Collection;
/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintaTulosServiceDto {
	private String hakemusOid;
	private String hakijaOid;
	private Collection<HakutoiveenValintatulos> hakutoiveenValintatulosekset;
	private Collection<HakutoiveDto> hakutoiveet;

	public Collection<HakutoiveDto> getHakutoiveet() {
		return hakutoiveet;
	}

	public void setHakutoiveet(Collection<HakutoiveDto> hakutoiveet) {
		this.hakutoiveet = hakutoiveet;
	}

	public Collection<HakutoiveenValintatulos> getHakutoiveenValintatulosekset() {
		return hakutoiveenValintatulosekset;
	}

	public void setHakutoiveenValintatulosekset(Collection<HakutoiveenValintatulos> hakutoiveenValintatulosekset) {
		this.hakutoiveenValintatulosekset = hakutoiveenValintatulosekset;
	}


	public void setHakemusOid(String hakemusOid) {
		this.hakemusOid = hakemusOid;
	}
	public void setHakijaOid(String hakijaOid) {
		this.hakijaOid = hakijaOid;
	}
	
	public String getHakemusOid() {
		return hakemusOid;
	}
	public String getHakijaOid() {
		return hakijaOid;
	}
}
