package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Tyovaihe laskenta reitilla
 */
public class LaskentaJaHakukohde {

	private final static String NIMI_FORMAT = "hakukohdeOid(%s) %s";
	private final Laskenta laskenta;
	private final String hakukohdeOid;
	private volatile boolean luovutus = false;

	public LaskentaJaHakukohde(Laskenta laskenta, String hakukohdeOid) {
		this.laskenta = laskenta;
		this.hakukohdeOid = hakukohdeOid;
	}

	public void luovuta() {
		luovutus = true;
	}

	public boolean isLuovutettu() {
		return luovutus;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}

	public Laskenta getLaskenta() {
		return laskenta;
	}

	public String toString() {
		return String.format(NIMI_FORMAT, hakukohdeOid, laskenta);
	}
}
