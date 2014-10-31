package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static rx.Observable.from;
import static rx.Observable.zip;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
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

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.LueHakijapalvelunOsoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.SijoittelussaHyvaksyttyHakijaBiPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class HyvaksymiskirjeetServiceImpl implements HyvaksymiskirjeetService {

	private static final Logger LOG = LoggerFactory
			.getLogger(HyvaksymiskirjeetServiceImpl.class);
	private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
	private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
	// private final SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;
	private final SijoitteluAsyncResource sijoitteluAsyncResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final OrganisaatioAsyncResource organisaatioAsyncResource;
	private final HaeOsoiteKomponentti haeOsoiteKomponentti;

	@Autowired
	public HyvaksymiskirjeetServiceImpl(
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
			SijoitteluAsyncResource sijoitteluAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			OrganisaatioAsyncResource organisaatioAsyncResource,
			HaeOsoiteKomponentti haeOsoiteKomponentti) {
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
		this.sijoitteluAsyncResource = sijoitteluAsyncResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.organisaatioAsyncResource = organisaatioAsyncResource;
		this.haeOsoiteKomponentti = haeOsoiteKomponentti;
	}

	@Override
	public void hyvaksymiskirjeetHakemuksille(final KirjeProsessi prosessi,
			final HyvaksymiskirjeDTO hyvaksymiskirjeDTO,
			final List<String> hakemusOids) {
		Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
				.getApplicationsByOids(hakemusOids);
		Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource
				.getKoulutuspaikkallisetHakijat(
						hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<OrganisaatioRDTO> organisaatioFuture = organisaatioAsyncResource
				.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
		zip(
				from(hakemuksetFuture),
				from(hakijatFuture),
				from(organisaatioFuture),
				(hakemukset, hakijat, organisaatio) -> {
					LOG.info("Tehdaan valituille hakijoille hyvaksytyt filtterointi.");

					final Set<String> kohdeHakijat = Sets
							.newHashSet(hakemusOids);
					final String hakukohdeOid = hyvaksymiskirjeDTO
							.getHakukohdeOid();
					BiPredicate<HakijaDTO, String> kohdeHakukohteessaHyvaksytty = new SijoittelussaHyvaksyttyHakijaBiPredicate();

					Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat
							.getResults()
							.stream()
							.filter(h -> kohdeHakukohteessaHyvaksytty.test(h,
									hakukohdeOid))
							//
							.filter(h -> kohdeHakijat.contains(h
									.getHakemusOid()))
							//
							.collect(Collectors.toList());
					Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti
							.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
					Osoite hakijapalveluidenOsoite = null;
					try {
						MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet
								.get(hyvaksymiskirjeDTO.getHakukohdeOid());
						hakijapalveluidenOsoite = LueHakijapalvelunOsoite
								.lueHakijapalvelunOsoite(haeOsoiteKomponentti,
										kohdeHakukohde.getHakukohteenKieli(),
										organisaatio);
					} catch (Exception e) {
						LOG.error(
								"Hakijapalveluiden osoitteen haussa odottamaton virhe {},\r\n{}",
								e.getMessage(),
								Arrays.toString(e.getStackTrace()));
					}
					return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
							hakijapalveluidenOsoite,
							hyvaksymiskirjeessaKaytetytHakukohteet,
							kohdeHakukohteessaHyvaksytyt, hakemukset,
							hyvaksymiskirjeDTO.getHakukohdeOid(),
							hyvaksymiskirjeDTO.getHakuOid(),
							hyvaksymiskirjeDTO.getTarjoajaOid(),
							hyvaksymiskirjeDTO.getSisalto(),
							hyvaksymiskirjeDTO.getTag(),
							hyvaksymiskirjeDTO.getTemplateName());
				})
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						letterBatch -> {
							letterBatchToViestintapalvelu().call(letterBatch,
									prosessi, hyvaksymiskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Sijoittelu tai hakemuspalvelukutsu epaonnistui {}",
									throwable.getMessage());
							prosessi.keskeyta();
						});
	}

	@Override
	public void hyvaksymiskirjeetHakukohteelle(KirjeProsessi prosessi,
			final HyvaksymiskirjeDTO hyvaksymiskirjeDTO) {
		Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
				.getApplicationsByOid(hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<HakijaPaginationObject> hakijatFuture = sijoitteluAsyncResource
				.getKoulutuspaikkallisetHakijat(
						hyvaksymiskirjeDTO.getHakuOid(),
						hyvaksymiskirjeDTO.getHakukohdeOid());
		Future<OrganisaatioRDTO> organisaatioFuture = organisaatioAsyncResource
				.haeOrganisaatio(hyvaksymiskirjeDTO.getTarjoajaOid());
		zip(
				from(hakemuksetFuture),
				from(hakijatFuture),
				from(organisaatioFuture),
				(hakemukset, hakijat, organisaatio) -> {

					LOG.info("Tehdaan hakukohteeseen valituille hyvaksytyt filtterointi.");
					final String hakukohdeOid = hyvaksymiskirjeDTO
							.getHakukohdeOid();
					BiPredicate<HakijaDTO, String> kohdeHakukohteessaHyvaksytty = new SijoittelussaHyvaksyttyHakijaBiPredicate();

					Collection<HakijaDTO> kohdeHakukohteessaHyvaksytyt = hakijat
							.getResults()
							.stream()
							.filter(h -> kohdeHakukohteessaHyvaksytty.test(h,
									hakukohdeOid)).collect(Collectors.toList());
					Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = hyvaksymiskirjeetKomponentti
							.haeKiinnostavatHakukohteet(kohdeHakukohteessaHyvaksytyt);
					Osoite hakijapalveluidenOsoite = null;
					try {
						MetaHakukohde kohdeHakukohde = hyvaksymiskirjeessaKaytetytHakukohteet
								.get(hyvaksymiskirjeDTO.getHakukohdeOid());
						hakijapalveluidenOsoite = LueHakijapalvelunOsoite
								.lueHakijapalvelunOsoite(haeOsoiteKomponentti,
										kohdeHakukohde.getHakukohteenKieli(),
										organisaatio);
					} catch (Exception e) {
						LOG.error(
								"Hakijapalveluiden osoitteen haussa odottamaton virhe {},\r\n{}",
								e.getMessage(),
								Arrays.toString(e.getStackTrace()));
					}
					return hyvaksymiskirjeetKomponentti.teeHyvaksymiskirjeet(
							hakijapalveluidenOsoite,
							hyvaksymiskirjeessaKaytetytHakukohteet,
							kohdeHakukohteessaHyvaksytyt, hakemukset,
							hyvaksymiskirjeDTO.getHakukohdeOid(),
							hyvaksymiskirjeDTO.getHakuOid(),
							hyvaksymiskirjeDTO.getTarjoajaOid(),
							hyvaksymiskirjeDTO.getSisalto(),
							hyvaksymiskirjeDTO.getTag(),
							hyvaksymiskirjeDTO.getTemplateName());
				})
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						letterBatch -> {
							letterBatchToViestintapalvelu().call(letterBatch,
									prosessi, hyvaksymiskirjeDTO);
						},
						throwable -> {
							LOG.error(
									"Sijoittelu tai hakemuspalvelukutsu epaonnistui {} {}",
									throwable.getMessage(), Arrays.asList(throwable.getStackTrace()));
							prosessi.keskeyta();
						});
	}

	public Action3<LetterBatch, KirjeProsessi, HyvaksymiskirjeDTO> letterBatchToViestintapalvelu() {
		return (letterBatch, prosessi, kirje) -> {
			try {
				if (prosessi.isKeskeytetty()) {
					LOG.error("Hyvaksymiskirjeiden luonti on keskeytetty kayttajantoimesta!");
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
											LOG.error("Hyvaksymiskirjeiden luonti on keskeytetty kayttajantoimesta!");
											stop.onNext(null);
											return;
										}
										if ("error".equals(status.getStatus())) {
											LOG.error("Hyvaksymiskirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
											prosessi.keskeyta();
											stop.onNext(null);
										}
										if ("ready".equals(status.getStatus())) {
											prosessi.vaiheValmistui();
											LOG.error("Hyvaksymiskirjeet valmistui!");
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
				LOG.error("Virhe hakukohteelle {}: {}",
						kirje.getHakukohdeOid(), e.getMessage());
				prosessi.keskeyta();
			}
		};
	}
}
