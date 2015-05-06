package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

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

import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
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

import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
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

/**
 * @author Jussi Jartamo
 */
@Component
public class JalkiohjauskirjeetKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(JalkiohjauskirjeetKomponentti.class);

	private final HaeOsoiteKomponentti osoiteKomponentti;
	private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
	@Autowired
	public JalkiohjauskirjeetKomponentti(
			KoodistoCachedAsyncResource koodistoCachedAsyncResource,
			HaeOsoiteKomponentti osoiteKomponentti) {
		this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
		this.osoiteKomponentti = osoiteKomponentti;
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

	private Map<String, Hakemus> hakemuksistaOidMap(
			final Collection<Hakemus> hakemukset) {
		Map<String, Hakemus> m = Maps.newHashMap();
		for (Hakemus h : hakemukset) {
			m.put(h.getOid(), h);
		}
		return m;
	}

	public LetterBatch teeJalkiohjauskirjeet(
			String ylikirjoitettuPreferoitukielikoodi,
			@Body final Collection<HakijaDTO> hyvaksymattomatHakijat,
			final Collection<Hakemus> hakemukset,
			final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet,
			@Simple("${property.hakuOid}") String hakuOid,
			@Property("templateName") String templateName,
			@Property("sisalto") String sisalto, @Property("tag") String tag

	) {// @Property(OPH.HAKUOID)
		// String
		// hakuOid)
		// {
		final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
		if (kaikkiHyvaksymattomat == 0) {
			LOG.error("Jälkiohjauskirjeitä yritetään luoda haulle jolla kaikki hakijat on hyväksytty koulutukseen!");
			viestintapalveluLogi("Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
			throw new SijoittelupalveluException(
					"Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
		}
		// final Map<String, MetaHakukohde>
		// jalkiohjauskirjeessaKaytetytHakukohteet =
		// haeKiinnostavatHakukohteet(hyvaksymattomatHakijat);
		final Map<String, Hakemus> hakemusOidHakemukset = hakemuksistaOidMap(hakemukset);
		final List<Letter> kirjeet = new ArrayList<Letter>();
		String preferoituKielikoodi;
		final boolean kaytetaanYlikirjoitettuKielikoodia = StringUtils
				.isNotBlank(ylikirjoitettuPreferoitukielikoodi);
		if (kaytetaanYlikirjoitettuKielikoodia) {
			preferoituKielikoodi = ylikirjoitettuPreferoitukielikoodi;
		} else {
			preferoituKielikoodi = KieliUtil.SUOMI;
		}
		final Map<fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila, Integer> tilaToPrioriteetti = Maps.newHashMap();
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY, 1);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY, 2);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARASIJALTA_HYVAKSYTTY, 3);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA, 4);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUNUT, 5);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUTETTU, 6);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUNTUNUT, 7);
		tilaToPrioriteetti.put(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY, 8);

		Map<String, Koodi> maajavaltio = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
		Map<String, Koodi> posti = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
		for (HakijaDTO hakija : hyvaksymattomatHakijat) {
			final String hakemusOid = hakija.getHakemusOid();
			if (!hakemusOidHakemukset.containsKey(hakemusOid)) {
				continue;
			}
			final Hakemus hakemus = hakemusOidHakemukset.get(hakemusOid); // hakemusProxy.haeHakemus(hakemusOid);
			final Osoite osoite = osoiteKomponentti.haeOsoite(maajavaltio, posti, hakemus);
			final List<Map<String, Object>> tulosList = new ArrayList<Map<String, Object>>();
			if (!kaytetaanYlikirjoitettuKielikoodia) {
				preferoituKielikoodi = new HakemusWrapper(hakemus)
						.getAsiointikieli();
			}

			for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
				String hakukohdeOid = hakutoive.getHakukohdeOid();
				MetaHakukohde metakohde = jalkiohjauskirjeessaKaytetytHakukohteet
						.get(hakukohdeOid);
				Map<String, Object> tulokset = new HashMap<String, Object>();

				tulokset.put(
						"hakukohteenNimi",
						metakohde.getHakukohdeNimi().getTeksti(
								preferoituKielikoodi,
								vakioHakukohteenNimi(hakukohdeOid)));
				tulokset.put("oppilaitoksenNimi", ""); // tieto on jo osana
														// hakukohdenimea
														// joten
														// tuskin tarvii
														// toistaa
				tulokset.put("hylkayksenSyy", StringUtils.EMPTY);

				StringBuilder pisteet = new StringBuilder();
				for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
					if (pistetieto.getArvo() != null) {
						try {
							BigDecimal ehkaNumeroEhkaTotuusarvo = new BigDecimal(
									pistetieto.getArvo());
							pisteet.append(
									suomennaNumero(ehkaNumeroEhkaTotuusarvo))
									.append(ARVO_VALI);
						} catch (NumberFormatException notNumber) {
							// OVT-6340 filtteroidaan totuusarvot pois
						}
					}
				}
				tulokset.put("paasyJaSoveltuvuuskoe", pisteet.toString().trim());
				tulokset.put(
						"organisaationNimi",
						metakohde.getTarjoajaNimi().getTeksti(
								preferoituKielikoodi,
								vakioTarjoajanNimi(hakukohdeOid)));
				StringBuilder omatPisteet = new StringBuilder();
				StringBuilder hyvaksytyt = new StringBuilder();

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
					String kkPiste = suomennaNumero(Optional
							.ofNullable(valintatapajono.getPisteet())
							.orElse(BigDecimal.ZERO));
					String kkMinimi = suomennaNumero(Optional
							.ofNullable(
									valintatapajono
											.getAlinHyvaksyttyPistemaara())
							.orElse(BigDecimal.ZERO));
					kkSijoitukset.add(new Sijoitus(kkNimi, kkJonosija,
							kkHyvaksytyt));
					kkPisteet.add(new Pisteet(kkNimi, kkPiste, kkMinimi));
					//
					// OVT-6334 : Logiikka ei kuulu koostepalveluun!
					//
					if (osoite
							.isUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt()) { // ei
																									// pisteita
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
					
					if (valintatapajono.getHyvaksytty() == null) {
						viestintapalveluLogi("Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
						throw new SijoittelupalveluException(
								"Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
					}
					if (valintatapajono.getHakeneet() == null) {
						viestintapalveluLogi("Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
						throw new SijoittelupalveluException(
								"Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
					}
				}
				Collections.sort(hakutoive.getHakutoiveenValintatapajonot(),
						new Comparator<HakutoiveenValintatapajonoDTO>() {
							@Override
							public int compare(HakutoiveenValintatapajonoDTO o1,
									HakutoiveenValintatapajonoDTO o2) {
								fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila h1 = Optional.ofNullable(o1.getTila()).orElse(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY);
								fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila h2 = Optional.ofNullable(o2.getTila()).orElse(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY);
								if(fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA.equals(h1) 
										//
										&& fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA.equals(h2)) {
									Integer i1 = Optional.ofNullable(o1.getVarasijanNumero()).orElse(0);
									Integer i2 = Optional.ofNullable(o2.getVarasijanNumero()).orElse(0);
									return i1.compareTo(i2);
								}
								return tilaToPrioriteetti.get(h1).compareTo(tilaToPrioriteetti.get(h2));
							}
						});
				for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive
						.getHakutoiveenValintatapajonot()) {
					if (VARALLA.equals(valintatapajono.getTila())
							&& valintatapajono.getVarasijanNumero() != null) {
						tulokset.put("varasija", HakemusUtil
								.varasijanNumeroConverter(
										valintatapajono.getVarasijanNumero(),
										preferoituKielikoodi));
					}
					tulokset.put("hylkaysperuste",
							new Teksti(valintatapajono.getTilanKuvaukset())
									.getTeksti(preferoituKielikoodi,
											StringUtils.EMPTY));

					tulokset.put("valinnanTulos", HakemusUtil.tilaConverter(
							valintatapajono.getTila(), preferoituKielikoodi,
							valintatapajono.isHyvaksyttyHarkinnanvaraisesti()));
					break;
				}
				tulokset.put("omatPisteet", omatPisteet.toString());
				tulokset.put("hyvaksytyt", hyvaksytyt.toString());
				tulokset.put("alinHyvaksyttyPistemaara", StringUtils.EMPTY);
				tulokset.put("kaikkiHakeneet", StringUtils.EMPTY);
				tulosList.add(tulokset);
			}
			Map<String, Object> replacements = Maps.newHashMap();
			replacements.put("tulokset", tulosList);
			replacements.put("henkilotunnus",
					new HakemusWrapper(hakemus).getHenkilotunnus());
			kirjeet.add(new Letter(osoite, templateName, preferoituKielikoodi,
					replacements));
		}

		LOG.info(
				"Yritetään luoda viestintapalvelulta jälkiohjauskirjeitä {} kappaletta!",
				kirjeet.size());
		LetterBatch viesti = new LetterBatch(kirjeet);
		viesti.setApplicationPeriod(hakuOid);
		viesti.setFetchTarget(null);
		viesti.setLanguageCode(preferoituKielikoodi);
		viesti.setOrganizationOid(null);
		viesti.setTag(tag);
		viesti.setTemplateName(templateName);
        viesti.setIposti(true);
		Map<String, Object> templateReplacements = Maps.newHashMap();
		templateReplacements.put("sisalto", sisalto);
		viesti.setTemplateReplacements(templateReplacements);
		LOG.debug("\r\n{}", new ViestiWrapper(viesti));
		// Response response =
		// viestintapalveluProxy.haeJalkiohjauskirjeet(viesti);
		viestintapalveluLogi("Tiedot jälkiohjauskirjeen luontiin on välitetty viestintäpalvelulle.");
		return viesti; // response.getEntity();
	}

	private void viestintapalveluLogi(String logiViesti) {
		try {
			// messageProxy.message(logiViesti);
		} catch (Exception ex) {
			LOG.error(
					"Viestintäpalvelun message rajapinta ei ole käytettävissä! {}",
					logiViesti);
		}
	}

}
