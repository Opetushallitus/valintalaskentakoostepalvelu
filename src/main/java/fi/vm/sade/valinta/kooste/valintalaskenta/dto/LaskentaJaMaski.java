package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 * Tyovaihe laskenta reitilla
 */
public class LaskentaJaMaski {

	private final Laskenta laskenta;
	private final Maski maski;
	
	public LaskentaJaMaski(Laskenta laskenta) {
		this.laskenta = laskenta;
		this.maski = new Maski();
	}
	
	public LaskentaJaMaski(Laskenta laskenta, Maski maski) {
		this.laskenta = laskenta;
		if(maski == null) {
		this.maski = new Maski();	
		} else {
		this.maski = maski;
		}
	}
	
	public Laskenta getLaskenta() {
		return laskenta;
	}
	public Maski getMaski() {
		return maski;
	}
}
