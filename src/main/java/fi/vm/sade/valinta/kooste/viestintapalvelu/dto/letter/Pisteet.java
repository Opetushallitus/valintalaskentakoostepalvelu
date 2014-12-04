package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

/**
 * 
 * @author jussi jartamo
 *
 */
public class Pisteet {
	private String nimi;
	private String oma;
	private String minimi;

	public Pisteet() {

	}

	public Pisteet(String nimi, String oma, String minimi) {
		this.nimi = nimi;
		this.oma = oma;
		this.minimi = minimi;
	}

	public String getMinimi() {
		return minimi;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOma() {
		return oma;
	}
}
