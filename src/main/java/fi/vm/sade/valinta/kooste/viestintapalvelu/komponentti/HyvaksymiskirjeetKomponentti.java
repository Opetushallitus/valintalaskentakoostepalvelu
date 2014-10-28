package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_EROTIN;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VAKIO;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VALI;
import static fi.vm.sade.valinta.kooste.util.Formatter.suomennaNumero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Pisteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         OLETTAA ETTA KAIKILLE VALINTATAPAJONOILLE TEHDAAN HYVAKSYMISKIRJE JOS
 *         HAKEMUS ON HYVAKSYTTY YHDESSAKIN!
 * 
 *         Nykyisellaan hakemukset haetaan tassa komponentissa. Taytyisi
 *         refaktoroida niin etta hakemukset tuodaan komponentille.
 */
@Component
public class HyvaksymiskirjeetKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(HyvaksymiskirjeetKomponentti.class);

	private static final String TYHJA_TARJOAJANIMI = "Tuntematon koulu!";
	private static final String TYHJA_HAKUKOHDENIMI = "Tuntematon koulutus!";

	private KirjeetHakukohdeCache kirjeetHakukohdeCache;
	private HaeOsoiteKomponentti osoiteKomponentti;
	private ApplicationResource applicationResource;

	@Autowired
	public HyvaksymiskirjeetKomponentti(
			KirjeetHakukohdeCache kirjeetHakukohdeCache,
			HaeOsoiteKomponentti osoiteKomponentti,
			ApplicationResource applicationResource) {
		this.osoiteKomponentti = osoiteKomponentti;
		this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
		this.applicationResource = applicationResource;
	}

	private String vakioHakukohteenNimi(String hakukohdeOid) {
		return new StringBuilder().append("Hakukohteella ")
				.append(hakukohdeOid).append(" ei ole hakukohteennimeä")
				.toString();
	}

	private String vakioTarjoajanNimi(String hakukohdeOid) {
		return new StringBuilder().append("Hakukohteella ")
				.append(hakukohdeOid).append(" ei ole tarjojannimeä")
				.toString();
	}

	public LetterBatch teeHyvaksymiskirjeet(Osoite hakijapalveluidenOsoite,
			Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
			Collection<HakijaDTO> hakukohteenHakijat, List<Hakemus> hakemukset,
			String hakukohdeOid, String hakuOid, String tarjoajaOid,
			String sisalto, String tag, String templateName) {

		LOG.debug(
				"Hyvaksymiskirjeet for hakukohde '{}' and haku '{}' and sijoitteluajo '{}'",
				new Object[] { hakukohdeOid, hakuOid, });
		assert (hakukohdeOid != null);
		assert (hakuOid != null);
		Map<String, Hakemus> hakukohteenHakemukset = Maps.newHashMap();
		for (Hakemus h : hakemukset) {
			hakukohteenHakemukset.put(h.getOid(), h);
		}
		final int kaikkiHakukohteenHyvaksytyt = hakukohteenHakijat.size();
		if (kaikkiHakukohteenHyvaksytyt == 0) {
			LOG.error(
					"Hyväksymiskirjeitä yritetään luoda hakukohteelle {} millä ei ole hyväksyttyjä hakijoita!",
					hakukohdeOid);
			throw new HakemuspalveluException(
					"Hakukohteella on oltava vähintään yksi hyväksytty hakija että hyväksymiskirjeet voidaan luoda!");
		}
		// final Map<String, MetaHakukohde>
		// hyvaksymiskirjeessaKaytetytHakukohteet =
		// haeKiinnostavatHakukohteet(hakukohteenHakijat);
		final List<Letter> kirjeet = new ArrayList<Letter>();
		final Teksti koulu;
		final Teksti koulutus;
		final String preferoituKielikoodi;
		{
			MetaHakukohde metakohde = hyvaksymiskirjeessaKaytetytHakukohteet
					.get(hakukohdeOid);
			koulu = metakohde.getTarjoajaNimi();
			koulutus = metakohde.getHakukohdeNimi();
			preferoituKielikoodi = metakohde.getHakukohteenKieli();// KieliUtil.SUOMI;
		}

		for (HakijaDTO hakija : hakukohteenHakijat) {
			final String hakemusOid = hakija.getHakemusOid();
			final Hakemus hakemus = hakukohteenHakemukset.get(hakemusOid);
			// hakemus = hakemusWithRetryTwice(hakemusOid);

			final Osoite osoite = osoiteKomponentti.haeOsoite(hakemus);
			final List<Map<String, Object>> tulosList = new ArrayList<Map<String, Object>>();

			// Hyvaksymiskirjeilla preferoitukieli tulee hakukohteen kielesta
			// jarjestyksessa suomi,ruotsi,englanti

			for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {

				MetaHakukohde metakohde = hyvaksymiskirjeessaKaytetytHakukohteet
						.get(hakutoive.getHakukohdeOid());

				Map<String, Object> tulokset = new HashMap<String, Object>();

				tulokset.put("oppilaitoksenNimi", ""); // tieto on jo osana
														// hakukohdenimea
														// joten
														// tuskin tarvii
														// toistaa
				tulokset.put("hylkayksenSyy", StringUtils.EMPTY);

				StringBuilder pisteet = new StringBuilder();
				for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
					try {
						// OVT-7877 Koepisteiden formaattiin ei pysty luottamaan
						// koostepalvelussa
						String arvo = StringUtils.trimToEmpty(
								pistetieto.getArvo()).replace(",", ".");
						BigDecimal ehkaNumeroEhkaTotuusarvo = new BigDecimal(
								arvo);
						pisteet.append(suomennaNumero(ehkaNumeroEhkaTotuusarvo))
								.append(ARVO_VALI);
					} catch (NumberFormatException notNumber) {
						// OVT-6340 filtteroidaan totuusarvot pois
					}

				}
				tulokset.put("paasyJaSoveltuvuuskoe", pisteet.toString().trim());

				StringBuilder omatPisteet = new StringBuilder();
				StringBuilder hyvaksytyt = new StringBuilder();
				boolean firstOnly = true;
				// ei sortata! pitaisi olla jo oikeassa jarjestyksessa
				// Collections.sort(hakutoive.getHakutoiveenValintatapajonot(),
				// HakutoiveenValintatapajonoComparator.DEFAULT);
				//
				// VT-1036
				//
				List<Sijoitus> kkSijoitukset = Lists.newArrayList();
				List<Pisteet> kkPisteet = Lists.newArrayList();
				tulokset.put("sijoitukset", kkSijoitukset);
				tulokset.put("pisteet", kkPisteet);
				for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive
						.getHakutoiveenValintatapajonot()) {
					String kkNimi = valintatapajono.getValintatapajonoNimi();
					int kkJonosija = Optional.ofNullable(
							valintatapajono.getJonosija()).orElse(0)
							+ Optional.ofNullable(
									valintatapajono.getTasasijaJonosija())
									.orElse(0) - 1;
					int kkHyvaksytyt = Optional.ofNullable(
							valintatapajono.getHyvaksytty()).orElse(0);
					int kkPiste = Optional
							.ofNullable(valintatapajono.getPisteet())
							.orElse(BigDecimal.ZERO).intValue();
					int kkMinimi = Optional
							.ofNullable(
									valintatapajono
											.getAlinHyvaksyttyPistemaara())
							.orElse(BigDecimal.ZERO).intValue();
					kkSijoitukset.add(new Sijoitus(kkNimi, kkJonosija,
							kkHyvaksytyt));
					kkPisteet.add(new Pisteet(kkNimi, kkPiste, kkMinimi));

					// Hyvaksytty valintatapajonossa -- oletataan etta
					// hyvaksytty hakukohteeseen
					// if (HYVAKSYTTY.equals(valintatapajono.getTila())) {
					// preferoituKielikoodi = metakohde.getHakukohteenKieli();
					// }
					//
					// OVT-6334 : Logiikka ei kuulu koostepalveluun!
					//
					if (osoite
							.isUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt()) {
						// ei pisteita
						omatPisteet
								.append(ARVO_VAKIO)
								.append(ARVO_EROTIN)
								.append(suomennaNumero(valintatapajono
										.getAlinHyvaksyttyPistemaara(),
										ARVO_VAKIO)).append(ARVO_VALI);
					} else {
						omatPisteet
								.append(suomennaNumero(
										valintatapajono.getPisteet(),
										ARVO_VAKIO))
								.append(ARVO_EROTIN)
								.append(suomennaNumero(valintatapajono
										.getAlinHyvaksyttyPistemaara(),
										ARVO_VAKIO)).append(ARVO_VALI);
					}
					hyvaksytyt
							.append(suomennaNumero(
									valintatapajono.getHyvaksytty(), ARVO_VAKIO))
							.append(ARVO_EROTIN)
							.append(suomennaNumero(
									valintatapajono.getHakeneet(), ARVO_VAKIO))
							.append(ARVO_VALI);
					// Ylikirjoittuu viimeisella arvolla jos valintatapajonoja
					// on useampi
					// Nykyinen PDF formaatti ei kykene esittamaan usean jonon
					// selitteita jarkevasti
					if (firstOnly) {
						if (VARALLA.equals(valintatapajono.getTila())
								&& valintatapajono.getVarasijanNumero() != null) {

							tulokset.put("varasija", HakemusUtil
									.varasijanNumeroConverter(valintatapajono
											.getVarasijanNumero(),
											preferoituKielikoodi));
						}
						String hylkaysperuste = new Teksti(
								valintatapajono.getTilanKuvaukset()).getTeksti(
								preferoituKielikoodi, StringUtils.EMPTY);
						// if (StringUtils.isNotBlank(hylkaysperuste)) {
						// LOG.error("\r\nEpätyhjä hylkäysperuste,\r\n{}\r\n",
						// hylkaysperuste);
						// }
						tulokset.put("hylkaysperuste", hylkaysperuste);
						tulokset.put(
								"valinnanTulos",
								HakemusUtil.tilaConverter(
										valintatapajono.getTila(),
										preferoituKielikoodi,
										valintatapajono
												.isHyvaksyttyHarkinnanvaraisesti()));
						firstOnly = false;
					}
					if (valintatapajono.getHyvaksytty() == null) {
						throw new SijoittelupalveluException(
								"Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
					}
					if (valintatapajono.getHakeneet() == null) {
						throw new SijoittelupalveluException(
								"Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
					}
				}
				tulokset.put(
						"organisaationNimi",
						metakohde.getTarjoajaNimi().getTeksti(
								preferoituKielikoodi,
								vakioTarjoajanNimi(hakukohdeOid)));
				tulokset.put("omatPisteet", omatPisteet.toString());
				tulokset.put("hyvaksytyt", hyvaksytyt.toString());
				tulokset.put("alinHyvaksyttyPistemaara", StringUtils.EMPTY);
				tulokset.put("kaikkiHakeneet", StringUtils.EMPTY);
				tulokset.put("hakijapalveluidenOsoite", hakijapalveluidenOsoite);
				tulokset.put(
						"hakukohteenNimi",
						metakohde.getHakukohdeNimi().getTeksti(
								preferoituKielikoodi,
								vakioHakukohteenNimi(hakukohdeOid)));

				tulosList.add(tulokset);
			}
			Map<String, Object> replacements = Maps.newHashMap();
			replacements.put("tulokset", tulosList);
			replacements.put("koulu", koulu.getTeksti(preferoituKielikoodi,
					vakioTarjoajanNimi(hakukohdeOid)));
			replacements.put("henkilotunnus",
					new HakemusWrapper(hakemus).getHenkilotunnus());
			replacements.put("koulutus", koulutus.getTeksti(
					preferoituKielikoodi, vakioHakukohteenNimi(hakukohdeOid)));
			kirjeet.add(new Letter(osoite, templateName, preferoituKielikoodi,
					replacements));
		}

		LOG.info(
				"Yritetään luoda viestintapalvelulta hyvaksymiskirjeitä {} kappaletta!",
				kirjeet.size());
		Collections.sort(kirjeet, new Comparator<Letter>() {
			@Override
			public int compare(Letter o1, Letter o2) {
				try {
					return o1.getAddressLabel().getLastName()
							.compareTo(o2.getAddressLabel().getLastName());
				} catch (Exception e) {
					return 0;
				}
			}
		});
		LetterBatch viesti = new LetterBatch(kirjeet);
		viesti.setApplicationPeriod(hakuOid);
		viesti.setFetchTarget(hakukohdeOid);
		viesti.setLanguageCode(preferoituKielikoodi);
		viesti.setOrganizationOid(tarjoajaOid);
		viesti.setTag(tag);
		viesti.setTemplateName(templateName);
		Map<String, Object> templateReplacements = Maps.newHashMap();
		templateReplacements.put("sisalto", sisalto);
		templateReplacements.put("hakukohde", koulutus.getTeksti(
				preferoituKielikoodi, vakioHakukohteenNimi(hakukohdeOid)));
		templateReplacements.put("tarjoaja", koulu.getTeksti(
				preferoituKielikoodi, vakioTarjoajanNimi(hakukohdeOid)));
		viesti.setTemplateReplacements(templateReplacements);
		LOG.debug("\r\n{}", new ViestiWrapper(viesti));
		return viesti;
	}

	//
	// Hakee kaikki hyvaksymiskirjeen kohteena olevan hakukohteen hakijat ja
	// niihin liittyvat hakukohteet - eli myos hakijoiden hylatyt hakukohteet!
	// Metahakukohteille haetaan muun muassa tarjoajanimi!
	//
	public Map<String, MetaHakukohde> haeKiinnostavatHakukohteet(
			Collection<HakijaDTO> hakukohteenHakijat) {
		Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
		for (HakijaDTO hakija : hakukohteenHakijat) {
			for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
				String hakukohdeOid = hakutoive.getHakukohdeOid();
				if (!metaKohteet.containsKey(hakukohdeOid)) {
					try {
						metaKohteet.put(hakukohdeOid, kirjeetHakukohdeCache
								.haeHakukohde(hakukohdeOid));
					} catch (Exception e) {
						e.printStackTrace();
						LOG.error("Tarjonnasta ei saatu hakukohdetta {}: {}",
								new Object[] { hakukohdeOid, e.getMessage() });
						metaKohteet
								.put(hakukohdeOid,
										new MetaHakukohde(
												new Teksti(
														new StringBuilder()
																.append("Hakukohde ")
																.append(hakukohdeOid)
																.append(" ei löydy tarjonnasta!")
																.toString()),
												new Teksti(TYHJA_TARJOAJANIMI)));
					}
				}
			}
		}
		return metaKohteet;
	}

}
