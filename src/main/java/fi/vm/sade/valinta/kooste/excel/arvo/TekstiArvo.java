package fi.vm.sade.valinta.kooste.excel.arvo;

import fi.vm.sade.valinta.kooste.excel.ArvoTyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Teksti;

public class TekstiArvo extends Arvo {

	private final String teksti;
	private final boolean nimike;
	private final boolean editoitava;
	private final int ulottuvuus;
	public TekstiArvo(String teksti) {
		super(ArvoTyyppi.NIMIKE);
		this.teksti = teksti;
		this.nimike = true;
		this.editoitava = false;
		this.ulottuvuus = 1;
	}

	public TekstiArvo(String teksti, boolean nimike) {
		super(ArvoTyyppi.NIMIKE);
		this.teksti = teksti;
		this.nimike = nimike;
		this.editoitava = false;
		this.ulottuvuus = 1;
	}

	public TekstiArvo(String teksti, boolean nimike, boolean editoitava) {
		super(ArvoTyyppi.NIMIKE);
		this.teksti = teksti;
		this.nimike = nimike;
		this.editoitava = editoitava;
		this.ulottuvuus = 1;
	}
	public TekstiArvo(String teksti, boolean nimike, boolean editoitava, int ulottuvuus) {
		super(ArvoTyyppi.NIMIKE);
		this.teksti = teksti;
		this.nimike = nimike;
		this.editoitava = editoitava;
		this.ulottuvuus = ulottuvuus;
	}
	@Override
	public String toString() {
		return teksti;
	}

	public Teksti asTeksti() {
		int preferoituleveys = 0;
		if (!nimike) {
			preferoituleveys = Excel.VAKIO_LEVEYS / 2;
		}
		return new Teksti(teksti, true, false, teksti == null,
				preferoituleveys, ulottuvuus, editoitava);// ,
		// Excel.VAKIO_LEVEYS
		// / 2);
	}

	private static final TekstiArvo TYHJA = new TekstiArvo(null);
	private static final TekstiArvo TYHJA_EI_NIMIKE = new TekstiArvo(null,
			false);

	public static TekstiArvo tyhja() {
		return TYHJA;
	}

	public static TekstiArvo tyhja(boolean nimike) {
		if (!nimike) {
			return TYHJA_EI_NIMIKE;
		}
		return TYHJA;
	}
}
