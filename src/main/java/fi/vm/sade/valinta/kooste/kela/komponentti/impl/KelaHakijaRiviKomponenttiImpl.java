package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.HENKILOTUNNUS;
import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.SYNTYMAAIKA;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.OrganisaatioKomponentti;

@Component
public class KelaHakijaRiviKomponenttiImpl {

	private static final Logger LOG = LoggerFactory
			.getLogger(KelaHakijaRiviKomponenttiImpl.class);
	private static final Integer KESAKUU = 6;

	@Autowired
	private ApplicationResource hakemusProxy;

	@Autowired
	private LinjakoodiKomponentti linjakoodiProxy;

	@Autowired
	private OrganisaatioKomponentti organisaatioProxy;

	@Autowired
	private HaeHakuTarjonnaltaKomponentti hakuProxy;

	public List<TKUVAYHVA> luo(@Body HakijaDTO hakija,
			@Property("lukuvuosi") Date lukuvuosi,
			@Property("poimintapaivamaara") Date poimintapaivamaara)
			throws Exception {
		List<TKUVAYHVA> hakijanVastaanottamat = Lists.newArrayList();
		try {
			Set<String> linjakoodiErrorSet = new HashSet<String>();
			Map<String, String> linjakoodiCache = new HashMap<String, String>();
			Set<String> oppilaitosErrorSet = new HashSet<String>();
			Map<String, String> oppilaitosCache = new HashMap<String, String>();

			for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
				for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive
						.getHakutoiveenValintatapajonot()) {
					if (HYVAKSYTTY.equals(valintatapajono.getTila())) {
						TKUVAYHVA.Builder builder = new TKUVAYHVA.Builder();
						String hakukohdeOid = hakutoive.getHakukohdeOid();
						String tarjoajaOid = hakutoive.getTarjoajaOid();
						// LINJAKOODI
						builder.setLinjakoodi(haeLinjakoodi(hakukohdeOid,
								linjakoodiCache, linjakoodiErrorSet));
						// KOULUTUKSEN ALKAMISVUOSI
						builder.setLukuvuosi(lukuvuosi);
						// YHTEISHAUNKOULUKOODI
						builder.setOppilaitos(haeOppilaitos(tarjoajaOid,
								oppilaitosCache, oppilaitosErrorSet));

						builder.setValintapaivamaara(new Date()); // TODO:
																	// Sijoittelun
																	// täytyy
																	// osata
																	// kertoa
																	// tämä!
						builder.setSukunimi(hakija.getSukunimi());
						builder.setEtunimet(hakija.getEtunimi());

						try {
							Hakemus hakemus = hakemusProxy
									.getApplicationByOid(hakija.getHakemusOid());

							Map<String, String> henkilotiedot = new TreeMap<String, String>(
									String.CASE_INSENSITIVE_ORDER);
							henkilotiedot.putAll(hakemus.getAnswers()
									.getHenkilotiedot());

							if (henkilotiedot.containsKey(HENKILOTUNNUS)) {
								String standardinMukainenHenkilotunnus = henkilotiedot
										.get(HENKILOTUNNUS);
								// KELA ei halua vuosisata merkkia
								// henkilotunnukseen!
								StringBuilder kelanVaatimaHenkilotunnus = new StringBuilder();
								kelanVaatimaHenkilotunnus.append(
										standardinMukainenHenkilotunnus
												.substring(0, 6)).append(
										standardinMukainenHenkilotunnus
												.substring(7, 11));
								builder.setHenkilotunnus(kelanVaatimaHenkilotunnus
										.toString());
							} else { // Ulkomaalaisille syntyma-aika hetun
										// sijaan
								String syntymaAika = henkilotiedot
										.get(SYNTYMAAIKA); // esim
															// 04.05.1965
								// Poistetaan pisteet ja tyhjaa loppuun
								String syntymaAikaIlmanPisteita = syntymaAika
										.replace(".", "");
								builder.setHenkilotunnus(syntymaAikaIlmanPisteita
										.toString());
							}
						} catch (Exception e) {
							LOG.error(
									"Henkilötunnuksen hakeminen hakemuspalvelulta hakemukselle {} epäonnistui!",
									hakija.getHakemusOid());
							// e.printStackTrace();
							builder.setHenkilotunnus("XXXXXXXXXX");
						}
						builder.setPoimintapaivamaara(poimintapaivamaara);
						DateTime dateTime = new DateTime(lukuvuosi);
						if (dateTime.getMonthOfYear() > KESAKUU) { // myohemmin
																	// kuin
																	// kesakuussa!
							builder.setSyksyllaAlkavaKoulutus();
						} else {
							builder.setKevaallaAlkavaKoulutus();
						}
						LOG.info(
								"Tietue KELA-tiedostoon luotu onnistuneesti henkilölle {}",
								hakija.getHakemusOid());
						hakijanVastaanottamat.add(builder.build());
					}
				}
			}
			if (hakijanVastaanottamat.isEmpty()) {
				LOG.error(
						"Sijoittelulta palautui hakija (hakemusoid: {}) joka ei oikeasti ollut hyväksyttynä koulutukseen!",
						hakija.getHakemusOid());
			}
			return hakijanVastaanottamat;
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Odottamaton virhe {}", e.getMessage());
			throw e;
		}
		//
		// throw new
		// SijoittelupalveluException("Sijoittelulta palautui hakija (hakemusoid: "
		// + hakija.getHakemusOid()
		// + ") joka ei oikeasti ollut hyväksyttynä koulutukseen!");
	}

	private String haeLinjakoodi(String hakukohdeOid,
			Map<String, String> linjakoodiCache, Set<String> linjakoodiErrorSet) {
		if (linjakoodiCache.containsKey(hakukohdeOid)) {
			return linjakoodiCache.get(hakukohdeOid);
		} else {
			if (linjakoodiErrorSet.contains(hakukohdeOid)) {
				LOG.error("Linjakoodia ei saada tarjonnan kohteelle {}",
						hakukohdeOid);
			} else {
				try {
					String linjakoodi = linjakoodiProxy
							.haeLinjakoodi(hakukohdeOid);
					linjakoodiCache.put(hakukohdeOid, linjakoodi);
					return linjakoodi;
				} catch (Exception e) {
					e.printStackTrace();
					LOG.error(
							"Linjakoodia ei saatu tarjonnan kohteelle {}: Syystä {}",
							new Object[] { hakukohdeOid, e.getMessage() });
					linjakoodiErrorSet.add(hakukohdeOid);
				}
			}
		}
		return "000";
	}

	private String haeOppilaitos(String tarjoajaOid,
			Map<String, String> oppilaitosCache, Set<String> oppilaitosErrorSet) {
		if (oppilaitosCache.containsKey(tarjoajaOid)) {
			return oppilaitosCache.get(tarjoajaOid);
		} else {
			if (oppilaitosErrorSet.contains(tarjoajaOid)) {
				LOG.error(
						"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
						tarjoajaOid);
			} else {
				try {
					OrganisaatioRDTO organisaatio = organisaatioProxy
							.haeOrganisaatio(tarjoajaOid);
					if (organisaatio == null) {
						// new
						// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
						LOG.error(
								"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
								tarjoajaOid);
						oppilaitosErrorSet.add(tarjoajaOid);
					} else {
						if (organisaatio.getYhteishaunKoulukoodi() == null) {
							// new
							// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
							LOG.error(
									"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}",
									tarjoajaOid);
							oppilaitosErrorSet.add(tarjoajaOid);
						} else {
							oppilaitosCache.put(tarjoajaOid,
									organisaatio.getYhteishaunKoulukoodi());
							return organisaatio.getYhteishaunKoulukoodi();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					LOG.error(
							"Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}: Virhe {}",
							new Object[] { tarjoajaOid, e.getMessage() });
					// OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
					oppilaitosErrorSet.add(tarjoajaOid);
				}
			}
			return "0000";
		}
	}

}
