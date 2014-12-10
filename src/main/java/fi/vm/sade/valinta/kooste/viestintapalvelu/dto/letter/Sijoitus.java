package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

/**
 * 
 * @author jussi jartamo
 *
 */
public class Sijoitus {
	private String nimi;
	private String oma;
	private String hyvaksytyt;

	public Sijoitus() {

	}

	public Sijoitus(String nimi, Integer om, Integer hyvaksyty) {
		this.nimi = nimi;
		if(om == null) {
			this.oma = "-";
		} else {
			this.oma = om.toString();
		}
		if(hyvaksyty == null) {
			this.hyvaksytyt = "-";
		} else {
			this.hyvaksytyt = hyvaksyty.toString();
		}
	}

	public String getHyvaksytyt() {
		return hyvaksytyt;
	}

	public String getNimi() {
		return nimi;
	}

	public String getOma() {
		return oma;
	}
}
