package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.util.KieliUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrapperi sijoittelun HakutoiveDTO:lle jossa meta dataa liittyen
 *         kaikkiin hakukohteen hakijoihin
 */
public class MetaHakukohde {

	private final Teksti hakukohdeNimi;
	private final Teksti tarjoajaNimi;
	private final String hakukohteenKieli;

	public MetaHakukohde(Teksti hakukohdeNimi, Teksti tarjoajaNimi) {
		this.hakukohdeNimi = hakukohdeNimi;
		this.tarjoajaNimi = tarjoajaNimi;
		this.hakukohteenKieli = null;
	}

	public MetaHakukohde(Teksti hakukohdeNimi, Teksti tarjoajaNimi,
			String hakukohteenKieli) {
		this.hakukohdeNimi = hakukohdeNimi;
		this.tarjoajaNimi = tarjoajaNimi;
		this.hakukohteenKieli = hakukohteenKieli;
	}

	public String getHakukohteenKieli() {
		if (hakukohteenKieli == null) {
			if (hakukohdeNimi == null) {
				return KieliUtil.SUOMI;
			}
			return getHakukohdeNimi().getKieli();
		}
		return hakukohteenKieli;
	}

	public Teksti getHakukohdeNimi() {
		return hakukohdeNimi;
	}

	public Teksti getTarjoajaNimi() {
		return tarjoajaNimi;
	}

}
