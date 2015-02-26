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

	public Arvio() {

	}

	public void setArvosana(String arvosana) {
		this.arvosana = arvosana;
	}

	public void setAsteikko(String asteikko) {
		this.asteikko = asteikko;
	}

	public void setPisteet(Integer pisteet) {
		this.pisteet = pisteet;
	}

	public Arvio(String arvosana, String asteikko, Integer pisteet) {
		this.arvosana = arvosana;
		this.asteikko = asteikko;
		this.pisteet = pisteet;
	}

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
