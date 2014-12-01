package fi.vm.sade.valinta.kooste.viestintapalvelu.predicate;

import java.util.function.BiPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;

public class HakukohteessaHylattyHakijaBiPredicate implements
		BiPredicate<HakijaDTO, String> {
	private static final Logger LOG = LoggerFactory
			.getLogger(HakukohteessaHylattyHakijaBiPredicate.class);

	@Override
	public boolean test(HakijaDTO hakija, String hakukohdeOid) {
		if (hakija.getHakutoiveet() == null) {
		} else {
			for (HakutoiveDTO h : hakija.getHakutoiveet()) {
				if (hakukohdeOid.equals(h.getHakukohdeOid())) {
					for (HakutoiveenValintatapajonoDTO vjono : h
							.getHakutoiveenValintatapajonot()) {
						if (vjono.getTila() != null
								&& vjono.getTila().isHyvaksytty()) {
							return false;
						}
					}
				}

			}
		}
		return true;
	}

}
