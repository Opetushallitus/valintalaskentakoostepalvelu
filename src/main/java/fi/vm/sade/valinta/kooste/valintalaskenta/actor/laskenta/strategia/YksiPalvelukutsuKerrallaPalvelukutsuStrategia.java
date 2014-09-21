package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author jussija
 *
 *         Yksi palvelukutsukerrallaan palvelukutsujonoa tyhjentava strategia
 */
public class YksiPalvelukutsuKerrallaPalvelukutsuStrategia extends
		AbstraktiPalvelukutsuStrategia {

	private final static Logger LOG = LoggerFactory
			.getLogger(YksiPalvelukutsuKerrallaPalvelukutsuStrategia.class);

	public void aloitaUusiPalvelukutsu() {
		try {
			if (aloitettujaPalvelukutsuja() == 0) {
				kaynnistaJonossaSeuraavaPalvelukutsu();
			}
		} catch (Exception e) {
			LOG.error("Kaynnistyksessa tapahtui virhe! {}", e.getMessage());
		}
	}

}
