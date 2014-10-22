package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

/**
 * 
 * @author jussi jartamo
 *
 */
public class Pisteet {
	private String nimi;
	private int oma;
	private int minimi;

	public Pisteet() {

	}

	public Pisteet(String nimi, int oma, int minimi) {
		this.nimi = nimi;
		this.oma = oma;
		this.minimi = minimi;
	}

	public int getMinimi() {
		return minimi;
	}

	public String getNimi() {
		return nimi;
	}

	public int getOma() {
		return oma;
	}
}
