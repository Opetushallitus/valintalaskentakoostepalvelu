package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

/**
 * 
 * @author jussi jartamo
 *
 */
public class Sijoitus {
	private String nimi;
	private Integer oma;
	private Integer hyvaksytyt;

	public Sijoitus() {

	}

	public Sijoitus(String nimi, Integer oma, Integer hyvaksytyt) {
		this.nimi = nimi;
		this.oma = oma;
		this.hyvaksytyt = hyvaksytyt;
	}

	public Integer getHyvaksytyt() {
		return hyvaksytyt;
	}

	public String getNimi() {
		return nimi;
	}

	public Integer getOma() {
		return oma;
	}
}
