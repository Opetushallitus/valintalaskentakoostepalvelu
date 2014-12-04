package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import java.util.Collections;

import com.google.common.base.Predicate;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HakutoiveenValintatapajonoComparator;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SijoittelussaHyvaksyttyHakijaPredicate implements
		Predicate<HakijaDTO> {

	private final String hakukohdeOid;

	public SijoittelussaHyvaksyttyHakijaPredicate(String hakukohdeOid) {
		this.hakukohdeOid = hakukohdeOid;
	}

	@Override
	public boolean apply(HakijaDTO input) {
		if (input.getHakutoiveet() == null) {
		} else {
			for (HakutoiveDTO h : input.getHakutoiveet()) {

				if (hakukohdeOid.equals(h.getHakukohdeOid())) {
					final boolean checkFirstValintatapajonoOnly = false;
					// sort by
					// priority
//					Collections.sort(h.getHakutoiveenValintatapajonot(),
//							HakutoiveenValintatapajonoComparator.DEFAULT);

					for (HakutoiveenValintatapajonoDTO vjono : h
							.getHakutoiveenValintatapajonot()) {
						if (HakemuksenTila.HYVAKSYTTY.equals(vjono.getTila())
								|| HakemuksenTila.VARASIJALTA_HYVAKSYTTY
										.equals(vjono.getTila())) {
							return true;
						}
						if (checkFirstValintatapajonoOnly) {
							return false;
						}
					}
				}

			}
		}
		return false;
	}

}
