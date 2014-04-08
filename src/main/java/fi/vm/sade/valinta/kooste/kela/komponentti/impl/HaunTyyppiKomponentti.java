package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.valinta.kooste.exception.KoodistoException;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Use proxy instead of calling bean:hakukohdeTarjonnaltaKomponentti!
 *         Proxy provides retries!
 */
@Component
public class HaunTyyppiKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(HaunTyyppiKomponentti.class);

	// private final HakukohdeResource tarjontaResource;
	private final KoodiService koodiService;

	@Autowired
	public HaunTyyppiKomponentti(KoodiService koodiService) {
		this.koodiService = koodiService;
	}

	public String haunTyyppi(String haunTyyppiUri) {
		String koodiUri = TarjontaUriToKoodistoUtil.cleanUri(haunTyyppiUri);
		Integer koodiVersio = TarjontaUriToKoodistoUtil
				.stripVersion(haunTyyppiUri);
		SearchKoodisCriteriaType koodistoHaku = TarjontaUriToKoodistoUtil
				.toSearchCriteria(koodiUri, koodiVersio);

		List<KoodiType> koodiTypes = koodiService.searchKoodis(koodistoHaku);
		if (koodiTypes.isEmpty()) {
			throw new KoodistoException(
					"Koodisto palautti tyhjän koodijoukon urille " + koodiUri
							+ " ja käytetylle versiolle " + koodiVersio);
		}
		for (KoodiType koodi : koodiTypes) {
			String arvo = koodi.getKoodiArvo();
			if (arvo != null) {
				return arvo;

			} else {
				LOG.error(
						"Koodistosta palautui null arvo uri:lle {}, versio {}",
						new Object[] { koodiUri, koodiVersio });
			}
		}
		throw new KoodistoException("Koodistosta ei saatu arvoa urille "
				+ koodiUri + " ja käytetylle versiolle " + koodiVersio);

	}
}
