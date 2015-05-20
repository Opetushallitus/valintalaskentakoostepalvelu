package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Tyovaihe laskenta reitilla
 */
public class LaskentaJaHakukohde {

	private final static String NIMI_FORMAT = "hakukohdeOid(%s) %s";
	private final LaskentaStartParams laskenta;
	private final String hakukohdeOid;
	private volatile boolean luovutus = false;
	private volatile boolean valmistui = false; // jos tyo esim valmistuu koska
												// hakemuksia oli nolla niin
												// valintaperusteita ei haeta
												// suotta

	public LaskentaJaHakukohde(LaskentaStartParams laskenta, String hakukohdeOid) {
		this.laskenta = laskenta;
		this.hakukohdeOid = hakukohdeOid;
	}

	public void valmistui() {
		valmistui = true;
	}

	public void luovuta() {
		luovutus = true;
	}

	public boolean isValmistui() {
		return valmistui;
	}

	public boolean isLuovutettu() {
		return luovutus;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}

	public LaskentaStartParams getLaskenta() {
		return laskenta;
	}

	public String toString() {
		return String.format(NIMI_FORMAT, hakukohdeOid, laskenta);
	}
}
