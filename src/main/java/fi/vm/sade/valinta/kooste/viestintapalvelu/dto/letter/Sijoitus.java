package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

/**
 * 
 * @author jussi jartamo
 *
 */
public class Sijoitus {
	private String nimi;
	private int oma;
	private int hyvaksytyt;

	public Sijoitus() {

	}

	public Sijoitus(String nimi, int oma, int hyvaksytyt) {
		this.nimi = nimi;
		this.oma = oma;
		this.hyvaksytyt = hyvaksytyt;
	}

	public int getHyvaksytyt() {
		return hyvaksytyt;
	}

	public String getNimi() {
		return nimi;
	}

	public int getOma() {
		return oma;
	}
}
