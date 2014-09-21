package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaJaHaku {
	private final LaskentaImpl laskenta;
	private final List<String> hakukohdeOids;

	public LaskentaJaHaku(LaskentaImpl laskenta, List<String> hakukohdeOids) {
		this.laskenta = laskenta;
		this.hakukohdeOids = hakukohdeOids;
	}

	public List<String> getHakukohdeOids() {
		return hakukohdeOids;
	}

	public LaskentaImpl getLaskenta() {
		return laskenta;
	}
}
