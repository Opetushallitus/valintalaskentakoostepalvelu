package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil.ASUINMAA;
import static fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil.SUOMALAINEN_POSTINUMERO;
import static fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil.SUOMI;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.GsonBuilder;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.service.types.common.KoodiMetadataType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.util.KoodiServiceSearchCriteriaBuilder;
import fi.vm.sade.service.valintaperusteet.dto.model.Kieli;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Yhteystieto;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Maakoodi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;

@Component
public class HaeOsoiteKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(HaeOsoiteKomponentti.class);
	private static final String MAAT_JA_VALTIOT_PREFIX = "maatjavaltiot1_";
	private static final String SUOMI = "fin";
	private static final String POSTI = "posti_";
	private final Cache<String, Maakoodi> koodiCache = CacheBuilder
			.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
	private String applicationResourceUrl;
	private KoodiService koodiService;

	@Autowired
	public HaeOsoiteKomponentti(KoodiService koodiService) {
		this.koodiService = koodiService;
	}

	public Osoite haeOsoiteYhteystiedoista(Yhteystieto yhteystiedot,
			final KieliType preferoitutyyppi, String organisaationimi) {
		Maakoodi maakoodi = null;
		// onko ulkomaalainen?
		// hae koodistosta maa
		String postCode = yhteystiedot.getPostinumeroUri();
		final String uri = postCode;
		try {
			// hae koodistosta maa

			maakoodi = koodiCache.get(uri, new Callable<Maakoodi>() {
				@Override
				public Maakoodi call() throws Exception {
					String postitoimipaikka = StringUtils.EMPTY;
					List<KoodiType> koodiTypes=  koodiService
					.searchKoodis(KoodiServiceSearchCriteriaBuilder
							.latestKoodisByUris(uri));
					//LOG.error("From koodisto {}", new GsonBuilder().setPrettyPrinting().create().toJson(koodiTypes));
					for (KoodiType koodi : koodiTypes) {
						if (koodi.getMetadata() == null) {
							LOG.error(
									"Koodistosta palautuu tyhjiä koodeja! Koodisto uri {}",
									uri);
							continue;
						}
						// preferoidaan englantia
						postitoimipaikka = getKuvaus(koodi.getMetadata(),
								preferoitutyyppi);
						if (postitoimipaikka == null) {
							postitoimipaikka = getNimi(koodi.getMetadata()); // jos
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
					return new Maakoodi(postitoimipaikka, "FI");
				}
			});
		} catch (Exception e) {
			LOG.error(
					"Yhteystiedoille ei saatu haettua maata koodistosta! Koodisto URI {}",
					uri);
			// countryNotFound(hakemusOid, countryCode, uri);
			maakoodi = new Maakoodi(StringUtils.EMPTY, "FI");
		}
		String country = null;
		if(Kieli.EN.equals(preferoitutyyppi)) {
			country = "FINLAND";
		}
		return new Osoite(
				null, null,  // firstname, lastname
				//
				yhteystiedot.getOsoite(), // addressline1
				//
				null, null, // addressline2, addressline3
				//
				postinumero(yhteystiedot.getPostinumeroUri()), // postalCode
				StringUtils.capitalize(StringUtils.lowerCase(maakoodi.getPostitoimipaikka())), // city
				//
				null, country, null,
				organisaationimi,
				//
				null);
	}

	private String postinumero(String url) {
		if (url != null) {

			String[] o = url.split("_");
			if (o != null && o.length > 0) {
				return o[1];
			}

		}
		return StringUtils.EMPTY;
	}

	public Osoite haeOsoite(Hakemus hakemus) {
		String hakemusOid = hakemus.getOid();
		Maakoodi maakoodi = null;
		Map<String, String> henkilotiedot = new TreeMap<String, String>(
				String.CASE_INSENSITIVE_ORDER);
		henkilotiedot.putAll(hakemus.getAnswers().getHenkilotiedot());

		// onko ulkomaalainen?
		if (!SUOMI
				.equalsIgnoreCase(hakemus.getAnswers().getHenkilotiedot().get(ASUINMAA))) {
			// hae koodistosta maa
			String countryCode = henkilotiedot.get(ASUINMAA);
			final String uri = new StringBuilder()
					.append(MAAT_JA_VALTIOT_PREFIX)
					.append(countryCode.toLowerCase()).toString();

			try {
				maakoodi = koodiCache.get(uri, new Callable<Maakoodi>() {
					@Override
					public Maakoodi call() throws Exception {
						String postitoimipaikka = StringUtils.EMPTY;
						String maa = StringUtils.EMPTY;
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
							maa = getNimi(koodi.getMetadata(), KieliType.EN);
							if (maa == null) {
								maa = getNimi(koodi.getMetadata()); // jos
																		// suomea
																		// ei
																		// loydy
																		// kaikki
																		// kay
							}
							LOG.debug("Haettiin maa {} urille {}",
									new Object[] { maa, uri });
							if (maa != null) {
								break;
							}
						}
						return new Maakoodi(postitoimipaikka, maa);
					}
				});
			} catch (Exception e) {
				LOG.error(
						"Hakemukselle {}/applications/{} ei saatu haettua maata koodistosta! Koodisto URI {}",
						new Object[] { applicationResourceUrl, hakemusOid, uri });
				// countryNotFound(hakemusOid, countryCode, uri);
			}
		}

		try {
			// onko ulkomaalainen?
			if (SUOMI.equalsIgnoreCase(hakemus.getAnswers().getHenkilotiedot()
					.get(ASUINMAA))) {

				// hae koodistosta maa
				String postCode = hakemus.getAnswers().getHenkilotiedot()
						.get(SUOMALAINEN_POSTINUMERO);
				final String uri = new StringBuilder().append(POSTI)
						.append(postCode).toString();
				try {
					maakoodi = koodiCache.get(uri, new Callable<Maakoodi>() {
						@Override
						public Maakoodi call() throws Exception {
							String postitoimipaikka = StringUtils.EMPTY;
							String maa = StringUtils.EMPTY;
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
								postitoimipaikka = getNimi(
										koodi.getMetadata(), KieliType.FI);
								if (maa == null) {
									maa = getNimi(koodi.getMetadata()); // jos
																			// suomea
																			// ei
																			// loydy
																			// kaikki
																			// kay
								}
								LOG.debug(
										"Haettiin postitoimipaikka {} urille {}",
										new Object[] { postitoimipaikka, uri });
								if (postitoimipaikka != null) {
									break;
								}
							}
							return new Maakoodi(postitoimipaikka, maa);

						}
					});
				} catch (Exception e) {
					maakoodi = new Maakoodi(null, null);
				}

			}
		} catch (Exception e) { // ei tarvita mutta pidetaan kunnes
								// todennettu
								// etta lisays tuotannossa toimii
		}
		return OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus,
				maakoodi.getMaa(), maakoodi.getPostitoimipaikka());

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

	private static String getNimi(List<KoodiMetadataType> meta) {
		for (KoodiMetadataType data : meta) {
			return data.getNimi();
		}
		return null;
	}

	private static String getNimi(List<KoodiMetadataType> meta,
			KieliType kieli) {
		for (KoodiMetadataType data : meta) {
			if (kieli.equals(data.getKieli())) {
				return data.getNimi();
			}
		}
		return null;
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
