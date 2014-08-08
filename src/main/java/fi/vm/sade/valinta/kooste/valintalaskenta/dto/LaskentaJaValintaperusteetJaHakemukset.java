package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.List;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Tyovaihe laskenta reitilla
 */
public class LaskentaJaValintaperusteetJaHakemukset {

	private final Laskenta laskenta;
	private final List<ValintaperusteetDTO> valintaperusteet;
	private final List<Hakemus> hakemukset;
	private final String hakukohdeOid;

	public LaskentaJaValintaperusteetJaHakemukset(Laskenta laskenta,
			String hakukohdeOid, List<ValintaperusteetDTO> valintaperusteet,
			List<Hakemus> hakemukset) {
		this.laskenta = laskenta;
		this.hakukohdeOid = hakukohdeOid;
		this.valintaperusteet = valintaperusteet;
		this.hakemukset = hakemukset;
	}

	public LaskentaJaValintaperusteetJaHakemukset yhdista(
			LaskentaJaValintaperusteetJaHakemukset t) {
		if (!hakukohdeOid.equals(t.getHakukohdeOid())) {
			throw new RuntimeException(
					"Ei voida yhdistaa kahden eri hakukohteen valintaperusteita ja hakemuksia keskenaan!");
		}
		List<ValintaperusteetDTO> v = valintaperusteet;
		List<Hakemus> h = hakemukset;
		if (t.getValintaperusteet() != null) {
			v = t.getValintaperusteet();
		}
		if (t.getHakemukset() != null) {
			h = t.getHakemukset();
		}
		// if(v == null || h == null) {
		// throw new
		// RuntimeException("Laskentaa ei voida suorittaa koska palvelimelta on saatu puutteellisia tietoja!");
		// }
		return new LaskentaJaValintaperusteetJaHakemukset(laskenta,
				hakukohdeOid, v, h);
	}

	public boolean isValmisLaskettavaksi() {
		return valintaperusteet != null && hakemukset != null;
	}

	public List<Hakemus> getHakemukset() {
		return hakemukset;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}

	public Laskenta getLaskenta() {
		return laskenta;
	}

	public List<ValintaperusteetDTO> getValintaperusteet() {
		return valintaperusteet;
	}
}
