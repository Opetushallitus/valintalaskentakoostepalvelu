package fi.vm.sade.valinta.kooste.sijoittelu.exception;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SijoittelultaEiSisaltoaPoikkeus extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SijoittelultaEiSisaltoaPoikkeus(String viesti) {
		super(viesti);
	}

}
