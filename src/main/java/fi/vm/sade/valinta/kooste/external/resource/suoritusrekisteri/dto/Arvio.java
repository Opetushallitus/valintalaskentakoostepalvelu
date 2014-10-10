package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Arvio {
	private String arvosana;
	private String asteikko;
	private Integer pisteet;

	public String getAsteikko() {
		return asteikko;
	}

	public String getArvosana() {
		return arvosana;
	}

	public Integer getPisteet() {
		return pisteet;
	}
}
