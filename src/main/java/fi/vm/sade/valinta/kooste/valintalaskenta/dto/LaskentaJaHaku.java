package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaJaHaku {
	private final Laskenta laskenta;
	private final List<String> hakukohdeOids;

	public LaskentaJaHaku(Laskenta laskenta, List<String> hakukohdeOids) {
		this.laskenta = laskenta;
		this.hakukohdeOids = hakukohdeOids;
	}

	public List<String> getHakukohdeOids() {
		return hakukohdeOids;
	}

	public Laskenta getLaskenta() {
		return laskenta;
	}
}
