package fi.vm.sade.valinta.kooste.excel;

import scala.collection.mutable.StringBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Teksti extends Solu {

	private final String teksti;
	private final int ulottuvuus;
	private final boolean tasausOikealle;
	private final boolean lukittu;

	public Teksti() {
		this.teksti = null;
		this.ulottuvuus = 1;
		this.tasausOikealle = false;
		this.lukittu = false;
	}

	public Teksti(String teksti) {
		this.teksti = teksti;
		this.ulottuvuus = 1;
		this.tasausOikealle = false;
		this.lukittu = false;
	}

	public Teksti(String teksti, boolean tasausOikealle, boolean lukittu) {
		this.teksti = teksti;
		this.ulottuvuus = 1;
		this.tasausOikealle = tasausOikealle;
		this.lukittu = lukittu;
	}

	@Override
	public boolean isLukittu() {
		return lukittu;
	}

	public Teksti(String teksti, int ulottuvuus) {
		this.teksti = teksti;
		this.ulottuvuus = ulottuvuus;
		this.tasausOikealle = false;
		this.lukittu = false;
	}

	@Override
	public int ulottuvuus() {
		return ulottuvuus;
	}

	public String getTeksti() {
		return teksti;
	}

	@Override
	protected boolean validoi() {
		return teksti != null;
	}

	public boolean isTeksti() {
		return true;
	}

	public Teksti toTeksti() {
		return this;
	}

	public Numero toNumero() {
		return new Numero();
	}

	public boolean isNumero() {
		return false;
	}

	@Override
	public boolean isTyhja() {
		return teksti == null;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("teksti: ").append(teksti).toString();
	}

	private static final Teksti TYHJA = new Teksti();

	public static Teksti tyhja() {
		return TYHJA;
	}

	public boolean isTasausOikealle() {
		return tasausOikealle;
	}

	@Override
	public Monivalinta toMonivalinta() {
		throw new RuntimeException(
				"Tekstiä ei voi muuttaa monivalintakentäksi!");
	}
}
