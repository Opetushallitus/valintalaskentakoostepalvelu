package fi.vm.sade.valinta.kooste.excel;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Sarake {

	public final static Sarake PIILOTETTU = new Sarake(false);

	private final boolean naytetaanko;

	public Sarake(boolean naytetaanko) {
		this.naytetaanko = naytetaanko;
	}

	public boolean isNaytetaanko() {
		return naytetaanko;
	}
}
