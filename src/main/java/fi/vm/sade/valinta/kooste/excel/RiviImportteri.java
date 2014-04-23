package fi.vm.sade.valinta.kooste.excel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class RiviImportteri {

	private static final Logger LOG = LoggerFactory
			.getLogger(RiviImportteri.class);
	private boolean ok = false;

	protected void tarkista(Rivi rivi) {

	}

	public boolean importoi(Rivi rivi) {
		if (!ok) {
			ok = true;
			tarkista(rivi);
			return true;
		}
		return false;
	}

}
