package fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto;

import java.util.List;

public class Metadata {
	private List<Yhteystieto> yhteystiedot;
	
	public List<Yhteystieto> getYhteystiedot() {
		return yhteystiedot;
	}
	public void setYhteystiedot(List<Yhteystieto> yhteystiedot) {
		this.yhteystiedot = yhteystiedot;
	}
}
