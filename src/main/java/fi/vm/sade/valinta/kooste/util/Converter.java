package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Eligibility;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: wuoti Date: 9.9.2013 Time: 10.08
 */
public class Converter {
	private static final Logger LOG = LoggerFactory.getLogger(Converter.class);
	private final static String ETUNIMET = "Etunimet";
	private final static String SUKUNIMI = "Sukunimi";
	private final static String PREFERENCE = "preference";
	private final static String KOULUTUS_ID = "Koulutus-id";
	private final static String DISCRETIONARY = "discretionary";

	private final static String EI_ARVOSANAA = "Ei arvosanaa";

	private static class Hakutoive {
		private Boolean harkinnanvaraisuus;
		private String hakukohdeOid;

		private Boolean getHarkinnanvaraisuus() {
			return harkinnanvaraisuus;
		}

		private void setHarkinnanvaraisuus(Boolean harkinnanvaraisuus) {
			this.harkinnanvaraisuus = harkinnanvaraisuus;
		}

		private String getHakukohdeOid() {
			return hakukohdeOid;
		}

		private void setHakukohdeOid(String hakukohdeOid) {
			this.hakukohdeOid = hakukohdeOid;
		}
	}

	/**
	 * Poistaa "Ei arvosanaa" -kentät hakemukselta. Tämän funkkarin voi poistaa
	 * kunhan hakemuspalveluun saadaan tehtyä filtteri näille kentille
	 * 
	 * @param arvo
	 * @return
	 */
	private static String sanitizeArvo(String arvo) {
		if (arvo != null && EI_ARVOSANAA.equals(arvo)) {
			return "";
		}

		return arvo;
	}

	public static HakemusDTO hakemusToHakemusDTO(Hakemus hakemus) {
		HakemusDTO hakemusTyyppi = new HakemusDTO();
		hakemusTyyppi.setHakemusoid(hakemus.getOid());
		hakemusTyyppi.setHakijaOid(hakemus.getPersonOid());
        hakemusTyyppi.setHakuoid(hakemus.getApplicationSystemId());

		if (hakemus.getAnswers() != null) {
			try {
				if (hakemus.getAnswers().getHenkilotiedot() != null) {
					hakemusTyyppi.setEtunimi(hakemus.getAnswers()
							.getHenkilotiedot().get(ETUNIMET));
					hakemusTyyppi.setSukunimi(hakemus.getAnswers()
							.getHenkilotiedot().get(SUKUNIMI));

					for (Map.Entry<String, String> e : hakemus.getAnswers()
							.getHenkilotiedot().entrySet()) {
						AvainArvoDTO aa = new AvainArvoDTO();
						aa.setAvain(e.getKey());
						aa.setArvo(sanitizeArvo(e.getValue()));

						hakemusTyyppi.getAvaimet().add(aa);
					}
				}
			} catch (Exception e) {
				LOG.error("Epaonnistuminen henkilotietojen konversioon {}!",
						e.getMessage());
				throw e;
			}

			try {
				try {
					hakemus.getAnswers()
							.getHakutoiveet()
							.putAll(mapEligibilityAndStatus(
									hakemus.getPreferenceEligibilities(),
									hakemus.getAnswers().getHakutoiveet()));
				} catch (Exception e) {
					throw new RuntimeException(
							"Eligibilities statusten mappaus preferensseihin epaonnistui! "
									+ e.getMessage(), e);
				}
				Map<Integer, Hakutoive> hakutoiveet = new HashMap<Integer, Hakutoive>();
				if (hakemus.getAnswers().getHakutoiveet() != null) {
					for (Map.Entry<String, String> e : hakemus.getAnswers()
							.getHakutoiveet().entrySet()) {
						AvainArvoDTO aa = new AvainArvoDTO();
						aa.setAvain(e.getKey());
						aa.setArvo(sanitizeArvo(e.getValue()));

						hakemusTyyppi.getAvaimet().add(aa);

						if (e.getKey().startsWith(PREFERENCE)) {
							Integer prioriteetti;
							String numberAfterPreference;
							try {
								numberAfterPreference = e.getKey()
								.replaceAll("\\D+", "");
								if(StringUtils.isBlank(numberAfterPreference)) {
									continue;
								}
								prioriteetti = Integer.valueOf(numberAfterPreference);
							}catch(Exception ee) {
								LOG.error("Toivomusjarjestykseton preferenssi {}", e.getKey());
								throw ee;
							}
							Hakutoive hakutoive = null;
							if (!hakutoiveet.containsKey(prioriteetti)) {
								hakutoive = new Hakutoive();
								hakutoiveet.put(prioriteetti, hakutoive);
							} else {
								hakutoive = hakutoiveet.get(prioriteetti);
							}

							if (e.getKey().endsWith(KOULUTUS_ID)) {
								hakutoive.setHakukohdeOid(e.getValue());
							} else if (e.getKey().endsWith(DISCRETIONARY)) {
								Boolean discretionary = Boolean.valueOf(e
										.getValue());
								discretionary = discretionary == null ? false
										: discretionary;

								hakutoive.setHarkinnanvaraisuus(discretionary);
							}
						}
					}

					for (Map.Entry<Integer, Hakutoive> e : hakutoiveet
							.entrySet()) {
						Hakutoive hakutoive = e.getValue();
						if (hakutoive != null) {
							if (hakutoive.getHakukohdeOid() != null
									&& !hakutoive.getHakukohdeOid().trim()
											.isEmpty()) {
								HakukohdeDTO hk = new HakukohdeDTO();
								hk.setOid(hakutoive.getHakukohdeOid());
								hk.setHarkinnanvaraisuus(Boolean.TRUE
										.equals(hakutoive
												.getHarkinnanvaraisuus()));
								hk.setPrioriteetti(e.getKey());
								hakemusTyyppi.getHakukohteet().add(hk);
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Epaonnistuminen hakuvoiteiden konversioon {}!",
						e.getMessage());
				throw e;
			}

			try {
				if (hakemus.getAnswers().getKoulutustausta() != null) {
					for (Map.Entry<String, String> e : hakemus.getAnswers()
							.getKoulutustausta().entrySet()) {
						AvainArvoDTO aa = new AvainArvoDTO();
						aa.setAvain(e.getKey());
						aa.setArvo(sanitizeArvo(e.getValue()));

						hakemusTyyppi.getAvaimet().add(aa);
					}
				}
			} catch (Exception e) {
				LOG.error("Epaonnistuminen koulutustaustan konversioon {}!",
						e.getMessage());
				throw e;
			}

			try {

				if (hakemus.getAnswers().getLisatiedot() != null) {
					for (Map.Entry<String, String> e : hakemus.getAnswers()
							.getLisatiedot().entrySet()) {
						AvainArvoDTO aa = new AvainArvoDTO();
						aa.setAvain(e.getKey());
						aa.setArvo(sanitizeArvo(e.getValue()));

						hakemusTyyppi.getAvaimet().add(aa);
					}
				}
			} catch (Exception e) {
				LOG.error("Epaonnistuminen lisatietojen konversioon {}!",
						e.getMessage());
				throw e;
			}

			try {
				if (hakemus.getAnswers().getOsaaminen() != null) {
                    final List<AvainArvoDTO> osaaminenIlmanArvosanoja = hakemus.getAnswers().getOsaaminen().entrySet()
                            .stream()
                            .filter(entry -> !entry.getKey().startsWith("PK_") && !entry.getKey().startsWith("LK_"))
                            .map(e -> {
                                AvainArvoDTO aa = new AvainArvoDTO();
                                aa.setAvain(e.getKey());
                                aa.setArvo(sanitizeArvo(e.getValue()));
                                return aa;
                            })
                            .collect(Collectors.toList());
                    hakemusTyyppi.getAvaimet().addAll(osaaminenIlmanArvosanoja);

				}
			} catch (Exception e) {
				LOG.error("Epaonnistuminen osaamisen konversioon {}!",
						e.getMessage());
				throw e;
			}
		}
		try {
			if (hakemus.getAdditionalInfo() != null) {
				for (Map.Entry<String, String> e : hakemus.getAdditionalInfo()
						.entrySet()) {
					AvainArvoDTO aa = new AvainArvoDTO();
					aa.setAvain(e.getKey());
					aa.setArvo(e.getValue());

					hakemusTyyppi.getAvaimet().add(aa);
				}
			}
		} catch (Exception e) {
			LOG.error("Epaonnistuminen avainten konversioon {}!",
					e.getMessage());
			throw e;
		}

		return hakemusTyyppi;
	}

	public static Map<String, String> mapEligibilityAndStatus(
			List<Eligibility> eligibilities, Map<String, String> hakutoiveet) {
		Map<String, String> eligibilityAndStatus = Optional
				.ofNullable(eligibilities).orElse(Collections.emptyList())
				.stream()
				.filter(Objects::nonNull)
				// .map(e -> e.getAoId())
				.collect(Collectors.toList())
				.stream()
				.collect(Collectors.toMap(e -> e.getAoId(), e -> e.getParsedEligibilityStatus()));
		return Optional
				.ofNullable(hakutoiveet)
				.orElse(Collections.emptyMap())
				.entrySet()
				.stream()
				// preference{x}-Koulutus-id eli esim preference2-Koulutus-id
				.filter(pair -> {
					boolean b = pair.getKey().startsWith("preference")
							&& pair.getKey().endsWith("-Koulutus-id");
					LOG.debug("Matsaako {} {}", pair, b);
					return b;
				})
				// eligibility with aoId exists
				.filter(pair -> {
					boolean b = eligibilityAndStatus.containsKey(pair
							.getValue());
					LOG.debug("Matsaako key({}) == {}", pair.getValue(), b);
					return b;
				})
				// Maps
				// preference2-Koulutus-id = "1.2.246.562.20.645785477510"
				// To
				// preference2-Koulutus-id-eligibility = "UNKNOWN"
				.collect(
						Collectors.toMap(
								pair -> new StringBuilder(pair.getKey())
										.append("-eligibility").toString(),
								pair -> eligibilityAndStatus.get(pair
										.getValue())));
	}
}
