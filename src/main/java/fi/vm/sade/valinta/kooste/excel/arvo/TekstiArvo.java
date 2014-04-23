package fi.vm.sade.valinta.kooste.excel.arvo;

import fi.vm.sade.valinta.kooste.excel.ArvoTyyppi;
import fi.vm.sade.valinta.kooste.excel.Teksti;

public class TekstiArvo extends Arvo {

	private final String teksti;

	public TekstiArvo(String teksti) {
		super(ArvoTyyppi.NIMIKE);
		this.teksti = teksti;
	}

	@Override
	public String toString() {
		return teksti;
	}

	public Teksti asTeksti() {
		return new Teksti(teksti, true, teksti == null);
	}

	private static final TekstiArvo TYHJA = new TekstiArvo(null);

	public static TekstiArvo tyhja() {
		return TYHJA;
	}

}
