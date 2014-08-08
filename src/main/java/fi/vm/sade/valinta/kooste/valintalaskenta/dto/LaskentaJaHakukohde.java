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

	public LaskentaJaHakukohde(Laskenta laskenta, String hakukohdeOid) {
		this.laskenta = laskenta;
		this.hakukohdeOid = hakukohdeOid;
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
