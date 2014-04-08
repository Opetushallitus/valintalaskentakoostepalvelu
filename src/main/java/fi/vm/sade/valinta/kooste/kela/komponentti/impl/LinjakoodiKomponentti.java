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
public class LinjakoodiKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(LinjakoodiKomponentti.class);

	// private final HakukohdeResource tarjontaResource;
	private final KoodiService koodiService;

	@Autowired
	public LinjakoodiKomponentti(KoodiService koodiService) {
		this.koodiService = koodiService;
	}

	public String haeLinjakoodi(String hakukohdeNimiUri) {
		// @Property("hakukohdeOid") String hakukohdeOid
		// if (hakukohdeOid == null) {
		// throw new SijoittelupalveluException(
		// "Sijoittelu palautti puutteellisesti luodun hakutoiveen! Hakukohteen tunniste puuttuu!");
		// } else {
		// LOG.debug("Yhteys {}, HakukohdeResource.getHakukohdeNimi({})", new
		// Object[] { tarjontaResourceUrl,
		// hakukohdeOid });
		// HakukohdeDTO dto;
		// try {
		// dto = tarjontaResource.getByOID(hakukohdeOid);
		// } catch (Exception e) {
		// e.printStackTrace();
		// LOG.error("Ei hakukohdetta {} tarjonnassa!", hakukohdeOid);
		// throw new TarjontaException("Tarjonnasta ei löydy hakukohdetta " +
		// hakukohdeOid);
		// }
		// String uri = dto.getHakukohdeNimiUri();
		// if (uri == null) {
		// LOG.error("Hakukohteen {} uri oli null!", hakukohdeOid);
		// throw new TarjontaException("Tarjonnalla ei ollut hakukohteelle " +
		// hakukohdeOid
		// + " hakukohteenNimiUri:a!");
		// }

		String koodiUri = TarjontaUriToKoodistoUtil.cleanUri(hakukohdeNimiUri);
		Integer koodiVersio = TarjontaUriToKoodistoUtil
				.stripVersion(hakukohdeNimiUri);
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
				if (arvo.length() == 3) {
					return arvo;
				} else {
					LOG.error(
							"Koodistosta palautui virheellinen arvo {} uri:lle {}, versio {}",
							new Object[] { arvo, koodiUri, koodiVersio });
				}
			} else {
				LOG.error(
						"Koodistosta palautui null arvo uri:lle {}, versio {}",
						new Object[] { koodiUri, koodiVersio });
			}
		}
		throw new KoodistoException("Koodistosta ei saatu arvoa urille "
				+ koodiUri + " ja käytetylle versiolle " + koodiVersio);

	}

	// private String haeLinjakoodi(String hakukohdeOid,
	// Map<String, String> linjakoodiCache, Set<String> linjakoodiErrorSet) {
	// if (linjakoodiCache.containsKey(hakukohdeOid)) {
	// return linjakoodiCache.get(hakukohdeOid);
	// } else {
	// if (linjakoodiErrorSet.contains(hakukohdeOid)) {
	// LOG.error("Linjakoodia ei saada tarjonnan kohteelle {}",
	// hakukohdeOid);
	// } else {
	// try {
	// String linjakoodi = linjakoodiProxy
	// .haeLinjakoodi(hakukohdeOid);
	// linjakoodiCache.put(hakukohdeOid, linjakoodi);
	// return linjakoodi;
	// } catch (Exception e) {
	// e.printStackTrace();
	// LOG.error(
	// "Linjakoodia ei saatu tarjonnan kohteelle {}: Syystä {}",
	// new Object[] { hakukohdeOid, e.getMessage() });
	// linjakoodiErrorSet.add(hakukohdeOid);
	// }
	// }
	// }
	// return "000";
	// }
}
