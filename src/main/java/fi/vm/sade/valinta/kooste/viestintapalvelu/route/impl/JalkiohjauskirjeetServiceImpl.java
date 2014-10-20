package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila.PERUNUT;
import static rx.Observable.from;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rx.Observable;
import rx.functions.Action3;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import com.google.common.collect.Sets;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class JalkiohjauskirjeetServiceImpl implements JalkiohjauskirjeService {

	private final static Logger LOG = LoggerFactory
			.getLogger(JalkiohjauskirjeetServiceImpl.class);
	// private static final String TYHJA_TARJOAJANIMI = "Tuntematon koulu!";
	private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
	// private final KirjeetHakukohdeCache kirjeetHakukohdeCache;
	private final JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;
	// private final SijoitteluIlmankoulutuspaikkaaKomponentti
	// sijoitteluIlmankoulutuspaikkaaKomponentti;
	private final SijoitteluAsyncResource sijoitteluAsyncResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final KirjeetHakukohdeCache kirjeetHakukohdeCache;

	@Autowired
	public JalkiohjauskirjeetServiceImpl(
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			// KirjeetHakukohdeCache kirjeetHakukohdeCache,
			JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti,
			SijoitteluAsyncResource sijoitteluAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			KirjeetHakukohdeCache kirjeetHakukohdeCache) {
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.jalkiohjauskirjeetKomponentti = jalkiohjauskirjeetKomponentti;
		this.sijoitteluAsyncResource = sijoitteluAsyncResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
	}

	@Override
	public void jalkiohjauskirjeetHakemuksille(KirjeProsessi prosessi,
			JalkiohjauskirjeDTO jalkiohjauskirjeDTO, List<String> hakemusOids) {
		from(
				sijoitteluAsyncResource
						.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO
								.getHakuOid()))
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						hakijat -> {
							/**
							 * VIALLISET DATA POIS FILTTEROINTI
							 */
							Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat
									.getResults());
							/**
							 * WHITELIST FILTTEROINTI
							 */
							Set<String> whitelist = Sets
									.newHashSet(hakemusOids);
							Collection<HakijaDTO> whitelistinJalkeen = vainHakeneetJalkiohjattavat
									.stream()
									.filter(h -> whitelist.contains(h
											.getHakemusOid()))
									.collect(Collectors.toList());
							muodostaKirjeet().call(whitelistinJalkeen,
									prosessi, jalkiohjauskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Koulutuspaikattomien haku haulle {} epaonnistui! {}",
									jalkiohjauskirjeDTO.getHakuOid(),
									throwable.getMessage());
							prosessi.keskeyta();
						});
	}

	@Override
	public void jalkiohjauskirjeetHakukohteelle(KirjeProsessi prosessi,
			JalkiohjauskirjeDTO jalkiohjauskirjeDTO) {
		from(
				sijoitteluAsyncResource
						.getHakijatIlmanKoulutuspaikkaa(jalkiohjauskirjeDTO
								.getHakuOid()))
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						hakijat -> {
							/**
							 * VIALLISET DATA POIS FILTTEROINTI
							 */
							Collection<HakijaDTO> vainHakeneetJalkiohjattavat = puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(hakijat
									.getResults());
							muodostaKirjeet().call(vainHakeneetJalkiohjattavat,
									prosessi, jalkiohjauskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Koulutuspaikattomien haku haulle {} epaonnistui! {}",
									jalkiohjauskirjeDTO.getHakuOid(),
									throwable.getMessage());
							prosessi.keskeyta();
						});
	}

	private Action3<Collection<HakijaDTO>, KirjeProsessi, JalkiohjauskirjeDTO> muodostaKirjeet() {
		return (hakijat, prosessi, kirje) -> {
			if (hakijat.isEmpty()) {
				LOG.error("Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
				throw new RuntimeException(
						"Jalkiohjauskirjeita ei voida muodostaa tyhjalle joukolle!");
			}

			List<Hakemus> hakemukset;
			{
				Collection<String> hakemusOids = hakijat.stream()
						.map(h -> h.getHakemusOid())
						.collect(Collectors.toList());
				try {
					LOG.info("Haetaan hakemukset!");
					hakemukset = applicationAsyncResource
							.getApplicationsByOids(hakemusOids).get(1L,
									TimeUnit.MINUTES);
				} catch (Exception e) {
					LOG.error(
							"Hakemusten haussa oideilla tapahtui virhe! Oidit={}",
							Arrays.toString(hakemusOids.toArray()));
					throw new RuntimeException(
							"Hakemusten haussa oideilla tapahtui virhe!");
				}
			}
			/**
			 * KIELELLA FILTTEROINTI
			 */
			Collection<Hakemus> yksikielisetHakemukset;
			{
				final boolean ruotsinkieliset = kirje
						.isRuotsinkielinenAineisto();
				yksikielisetHakemukset = hakemukset
						.stream()
						.filter(h -> ruotsinkieliset == KieliUtil.RUOTSI
								.equals(new HakemusWrapper(h)
										.getAsiointikieli()))
						.collect(Collectors.toList());
			}
			Collection<HakijaDTO> yksikielisetHakijat;
			{
				Set<String> hakemusOids = yksikielisetHakemukset.stream()
						.map(h -> h.getOid()).collect(Collectors.toSet());
				yksikielisetHakijat = hakijat.stream()
						.filter(h -> hakemusOids.contains(h.getHakemusOid()))
						.collect(Collectors.toList());
			}
			final Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
			for (HakijaDTO hakija : yksikielisetHakijat) {
				for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
					String hakukohdeOid = hakutoive.getHakukohdeOid();
					if (!metaKohteet.containsKey(hakukohdeOid)) { // lisataan
																	// puuttuva
																	// hakukohde
						try {

							metaKohteet.put(hakukohdeOid, kirjeetHakukohdeCache
									.haeHakukohde(hakukohdeOid));

						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Tarjonnasta ei saatu hakukohdetta {}: {}",
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
													new Teksti(
															"Nimetön hakukohde")));
						}

					}
				}
			}
			LetterBatch letterBatch = jalkiohjauskirjeetKomponentti
					.teeJalkiohjauskirjeet(kirje.getKielikoodi(),
							yksikielisetHakijat, yksikielisetHakemukset,
							metaKohteet, kirje.getHakuOid(),
							kirje.getTemplateName(), kirje.getSisalto(),
							kirje.getTag());
			try {
				if (prosessi.isKeskeytetty()) {
					LOG.error("Jalkiohjauskirjeiden luonti on keskeytetty kayttajantoimesta!");
					return;
				}
				LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
				String batchId = viestintapalveluAsyncResource
						.viePdfJaOdotaReferenssi(letterBatch).get(35L,
								TimeUnit.SECONDS);
				LOG.info("Saatiin kirjeen seurantaId {}", batchId);
				prosessi.vaiheValmistui();
				PublishSubject<String> stop = PublishSubject.create();
				Observable
						.interval(1, TimeUnit.SECONDS)
						.take(ViestintapalveluAsyncResource.VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA)
						.takeUntil(stop)
						.subscribe(
								pulse -> {
									try {
										LOG.warn(
												"Tehdaan status kutsu seurantaId:lle {}",
												batchId);
										LetterBatchStatusDto status = viestintapalveluAsyncResource
												.haeStatus(batchId).get(900L,
														TimeUnit.MILLISECONDS);
										if (prosessi.isKeskeytetty()) {
											LOG.error("Jalkiohjauskirjeiden luonti on keskeytetty kayttajantoimesta!");
											stop.onNext(null);
											return;
										}
										if ("error".equals(status.getStatus())) {
											LOG.error("Jalkiohjauskirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
											prosessi.keskeyta();
											stop.onNext(null);
										}
										if ("ready".equals(status.getStatus())) {
											prosessi.vaiheValmistui();
											LOG.error("Jalkiohjauskirjeet valmistui!");
											prosessi.valmistui(batchId);
											stop.onNext(null);
										}
									} catch (Exception e) {
										LOG.error(
												"Statuksen haku epaonnistui {}",
												e.getMessage());
									}

								}, throwable -> {
									prosessi.keskeyta();
								}, () -> {
									prosessi.keskeyta();
								});
			} catch (Exception e) {
				LOG.error("Virhe haulle {}: {}", kirje.getHakuOid(),
						e.getMessage());
				prosessi.keskeyta();
			}
		};
	}

	private Collection<HakijaDTO> puutteellisillaTiedoillaOlevatJaItseItsensaPeruneetPois(
			Collection<HakijaDTO> hyvaksymattomatHakijat) {
		return hyvaksymattomatHakijat.stream()
		//
		// Filtteröidään puutteellisilla tiedoilla
		// olevat
		// hakijat pois
		//
				.filter(hakija -> {
					if (hakija == null || hakija.getHakutoiveet() == null
							|| hakija.getHakutoiveet().isEmpty()) {
						LOG.error("Hakija ilman hakutoiveita!");
						return false;
					}
					return true;
				})
				//
				// OVT-8553 Itse itsensa peruuttaneet pois
				//
				.filter(hakija -> hakija
						.getHakutoiveet()
						.stream()
						.anyMatch(
								hakutoive -> hakutoive
										.getHakutoiveenValintatapajonot()
										.stream()
										.noneMatch(
												valintatapajono -> valintatapajono
														.getVastaanottotieto() == PERUNUT)))
				//
				.collect(Collectors.toList());
	}
}
