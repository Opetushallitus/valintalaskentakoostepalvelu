package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil.ASUINMAA;
import static fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil.SUOMALAINEN_POSTINUMERO;
import static fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil.SUOMI;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.service.types.common.KoodiMetadataType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.util.KoodiServiceSearchCriteriaBuilder;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

@Component
public class HaeOsoiteKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(HaeOsoiteKomponentti.class);
	private static final String MAAT_JA_VALTIOT_PREFIX = "maatjavaltiot1_";
	private static final String POSTI = "posti_";

	private ApplicationResource applicationResource;
	private String applicationResourceUrl;
	private KoodiService koodiService;

	@Autowired
	public HaeOsoiteKomponentti(
			KoodiService koodiService,
			@Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String applicationResourceUrl,
			ApplicationResource applicationResource) {
		this.applicationResource = applicationResource;
		this.applicationResourceUrl = applicationResourceUrl;
		this.koodiService = koodiService;
	}

	public Osoite haeOsoite(String hakemusOid) {
		try {
			/**
			 * Osoitteen formatointi kirjeeseen. Esimerkiksi ulkomaille
			 * lahtevaan kirjeeseen haetaan englanninkielinen maan nimi.
			 */
			LOG.info("Haetaan hakemus {}/applications/{}", new Object[] {
					applicationResourceUrl, hakemusOid });
			Hakemus hakemus = applicationResource
					.getApplicationByOid(hakemusOid);
			if (hakemus == null) {
				notFound(hakemusOid);
				LOG.error("Hakemus {}/applications/{} null-arvo!",
						new Object[] { applicationResourceUrl, hakemusOid, });
			}
			return haeOsoite(hakemus);
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(
					"Hakemus {}/applications/{} sisälsi virheellistä tietoa!",
					new Object[] { applicationResourceUrl, hakemusOid, });
			notFound(hakemusOid);
		}
		return OsoiteHakemukseltaUtil.osoiteHakemuksesta(null, null, null);
	}

	public Osoite haeOsoite(Hakemus hakemus) {
		String hakemusOid = hakemus.getOid();
		String maa = null;
		String postitoimipaikka = null;
		Map<String, String> henkilotiedot = new TreeMap<String, String>(
				String.CASE_INSENSITIVE_ORDER);
		henkilotiedot.putAll(hakemus.getAnswers().getHenkilotiedot());
		try {
			// onko ulkomaalainen?
			if (!SUOMI.equals(hakemus.getAnswers().getHenkilotiedot()
					.get(ASUINMAA))) {
				// hae koodistosta maa
				String countryCode = henkilotiedot.get(ASUINMAA);
				String uri = new StringBuilder().append(MAAT_JA_VALTIOT_PREFIX)
						.append(countryCode.toLowerCase()).toString();

				try {
					for (KoodiType koodi : koodiService
							.searchKoodis(KoodiServiceSearchCriteriaBuilder
									.latestKoodisByUris(uri))) {
						if (koodi.getMetadata() == null) {
							LOG.error(
									"Koodistosta palautuu tyhjiä koodeja! Koodisto uri {}",
									uri);
							continue;
						}
						// preferoidaan englantia
						maa = getKuvaus(koodi.getMetadata(), KieliType.EN);
						if (maa == null) {
							maa = getKuvaus(koodi.getMetadata()); // jos
																	// suomea
																	// ei
																	// loydy
																	// kaikki
																	// kay
						}
						LOG.debug("Haettiin maa {} urille {}", new Object[] {
								maa, uri });
						if (maa != null) {
							break;
						}
					}
				} catch (Exception e) {
					LOG.error(
							"Hakemukselle {}/applications/{} ei saatu haettua maata koodistosta! Koodisto URI {}",
							new Object[] { applicationResourceUrl, hakemusOid,
									uri });
					countryNotFound(hakemusOid, countryCode, uri);
				}
			}
		} catch (Exception e) { // ei tarvita mutta pidetaan kunnes
								// todennettu
								// etta lisays tuotannossa toimii
		}
		try {
			// onko ulkomaalainen?
			if (SUOMI.equals(hakemus.getAnswers().getHenkilotiedot()
					.get(ASUINMAA))) {

				// hae koodistosta maa
				String postCode = hakemus.getAnswers().getHenkilotiedot()
						.get(SUOMALAINEN_POSTINUMERO);
				String uri = new StringBuilder().append(POSTI).append(postCode)
						.toString();

				try {
					for (KoodiType koodi : koodiService
							.searchKoodis(KoodiServiceSearchCriteriaBuilder
									.latestKoodisByUris(uri))) {
						if (koodi.getMetadata() == null) {
							LOG.error(
									"Koodistosta palautuu tyhjiä koodeja! Koodisto uri {}",
									uri);
							continue;
						}
						// preferoidaan englantia
						postitoimipaikka = getKuvaus(koodi.getMetadata(),
								KieliType.FI);
						if (maa == null) {
							maa = getKuvaus(koodi.getMetadata()); // jos
																	// suomea
																	// ei
																	// loydy
																	// kaikki
																	// kay
						}
						LOG.debug("Haettiin postitoimipaikka {} urille {}",
								new Object[] { postitoimipaikka, uri });
						if (postitoimipaikka != null) {
							break;
						}
					}
				} catch (Exception e) {
					LOG.error(
							"Hakemukselle {}/applications/{} ei saatu haettua postitoimipaikkaa koodistosta! Koodisto URI {}",
							new Object[] { applicationResourceUrl, hakemusOid,
									uri });
				}
			}
		} catch (Exception e) { // ei tarvita mutta pidetaan kunnes
								// todennettu
								// etta lisays tuotannossa toimii
		}
		return OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus, maa,
				postitoimipaikka);

	}

	private void notFound(String hakemusOid) {
		try {
			// messageProxy.message("Haku-palvelusta ei löytynyt hakemusta oid:lla "
			// + hakemusOid);
		} catch (Exception ex) {
			LOG.error(
					"Viestintäpalvelun message rajapinta ei ole käytettävissä! Hakemusta {} ei löydy!",
					hakemusOid);
		}
	}

	private void countryNotFound(String hakemusOid, String countryCode,
			String uri) {
		try {
			// messageProxy.message("Koodistosta ei saatu maata urilla " + uri +
			// " hakemukselle " + hakemusOid);
		} catch (Exception ex) {
			LOG.error(
					"Viestintäpalvelun message rajapinta ei ole käytettävissä! Koodistosta ei löydy maata {} hakemukselle {}!",
					new Object[] { uri, hakemusOid });
		}
	}

	private static String getKuvaus(List<KoodiMetadataType> meta) {
		for (KoodiMetadataType data : meta) {
			return data.getKuvaus();
		}
		return null;
	}

	private static String getKuvaus(List<KoodiMetadataType> meta,
			KieliType kieli) {
		for (KoodiMetadataType data : meta) {
			if (kieli.equals(data.getKieli())) {
				return data.getKuvaus();
			}
		}
		return null;
	}
}
