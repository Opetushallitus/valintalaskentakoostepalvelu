package fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class OsallistujatPredicate implements
		Predicate<ValintakoeOsallistuminenDTO> {

	private final String hakukohdeOid;
	private final Set<String> valintakoeOids;

	private OsallistujatPredicate() {
		this.hakukohdeOid = null;
		this.valintakoeOids = null;
	}

	private OsallistujatPredicate(String hakukohdeOid,
			Collection<String> valintakoeOids) {
		this.hakukohdeOid = hakukohdeOid;
		this.valintakoeOids = Sets.newHashSet(valintakoeOids);
	}

	public boolean apply(ValintakoeOsallistuminenDTO valintakoeOsallistuminen) {
		for (HakutoiveDTO hakutoive : valintakoeOsallistuminen.getHakutoiveet()) {
			if (!hakukohdeOid.equals(hakutoive.getHakukohdeOid())) {
				// vain tarkasteltavasta hakukohteesta
				// ollaan kiinnostuneita
				continue;
			}
			for (ValintakoeValinnanvaiheDTO valinnanvaihe : hakutoive
					.getValinnanVaiheet()) {
				for (ValintakoeDTO valintakoe : valinnanvaihe
						.getValintakokeet()) {
					if (!valintakoeOids.contains(valintakoe.getValintakoeOid())) {
						// vain tarkasteltavista
						// valintakokeista ollaan
						// kiinnostuneita
						continue;
					}
					if (fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen.OSALLISTUU
							.equals(valintakoe.getOsallistuminenTulos()
									.getOsallistuminen())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static OsallistujatPredicate vainOsallistujat(String hakukohdeOid,
			Collection<String> valintakoeOids) {
		return new OsallistujatPredicate(hakukohdeOid, valintakoeOids);
	}
}
