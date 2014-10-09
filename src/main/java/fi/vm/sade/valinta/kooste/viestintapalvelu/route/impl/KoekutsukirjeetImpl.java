package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static rx.Observable.from;
import static rx.Observable.zip;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class KoekutsukirjeetImpl implements KoekutsukirjeetService {
	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeetImpl.class);
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final ViestintapalveluAsyncResource viestintapalveluAsyncResource;
	private final ValintaperusteetValintakoeAsyncResource valintakoeResource;
	private final ValintalaskentaValintakoeAsyncResource osallistumisetResource;
	private final int VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA = 30;

	// private final DeferredManager deferredManager;

	@Autowired
	public KoekutsukirjeetImpl(
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ApplicationAsyncResource applicationAsyncResource,
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			ValintaperusteetValintakoeAsyncResource valintaperusteetValintakoeAsyncResource,
			ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {// ,
		// DeferredManager deferredManager) {
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.applicationAsyncResource = applicationAsyncResource;
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.valintakoeResource = valintaperusteetValintakoeAsyncResource;
		this.osallistumisetResource = valintalaskentaValintakoeAsyncResource;
		// this.deferredManager = deferredManager;
	}

	@Override
	public void koekutsukirjeetHakemuksille(KoekutsuProsessi prosessi,
			KoekutsuDTO koekutsu, Collection<String> hakemusOids) {
		from(applicationAsyncResource.getApplicationsByOids(hakemusOids))
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(koekutsukirjeiksi(prosessi, koekutsu),
						new Action1<Throwable>() {
							public void call(Throwable t1) {
								LOG.error(
										"Hakemuksien haussa hakutoiveelle {}: {}",
										koekutsu.getHakukohdeOid(),
										t1.getMessage());
								prosessi.keskeyta();
							}
						});
	}

	@Override
	public void koekutsukirjeetOsallistujille(KoekutsuProsessi prosessi,
			KoekutsuDTO koekutsu, List<String> valintakoeOids) {
		// ehka tarvitaan
		final Future<List<ValintakoeOsallistuminenDTO>> osallistumiset = osallistumisetResource
				.haeHakutoiveelle(koekutsu.getHakukohdeOid());
		final Future<List<ValintakoeDTO>> valintakokeetFuture = valintakoeResource
				.haeValintakokeet(valintakoeOids);
		final Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
				.getApplicationsByOid(koekutsu.getHakuOid(),
						koekutsu.getHakukohdeOid());

		zip(from(valintakokeetFuture), from(hakemuksetFuture),
				new Func2<List<ValintakoeDTO>, List<Hakemus>, List<Hakemus>>() {

					public List<Hakemus> call(List<ValintakoeDTO> valintakoes,
							List<Hakemus> hakemukset) {
						// Haetaan valintaperusteista valintakokeet
						// VT-838
						//
						// valintakoes.iterator().next().getTunniste()
						try {
							boolean haetaankoKaikkiHakutoiveenHakijatValintakokeeseen = valintakoes
									.stream()
									.filter(Objects::nonNull)
									.anyMatch(
											vk -> Boolean.TRUE.equals(vk
													.getKutsutaankoKaikki()));

							if (haetaankoKaikkiHakutoiveenHakijatValintakokeeseen) {
								LOG.info(
										"Kaikki hakutoiveen {} hakijat osallistuu!",
										koekutsu.getHakukohdeOid());
								return hakemukset;
							}
						} catch (Exception e) {
							LOG.error(
									"Kutsutaanko kaikki kokeeseen tarkistus epaonnistui! {}",
									e.getMessage());
							throw e;
						}
						List<String> haettavatValintakoeOids = valintakoes
								.stream()
								.filter(Objects::nonNull)
								.filter(vk -> !Boolean.TRUE.equals(vk
										.getKutsutaankoKaikki()))
								.map(vk -> vk.getOid())
								.collect(Collectors.toList());
						try {
							Set<String> osallistujienHakemusOidit = osallistumiset
									.get()
									.stream()
									.filter(Objects::nonNull)
									//
									.filter(OsallistujatPredicate.osallistujat(
											koekutsu.getHakukohdeOid(),
											haettavatValintakoeOids))
									.map(vk -> vk.getHakemusOid())
									.collect(Collectors.toSet());
							// vain hakukohteen osallistujat
							return hakemukset
									.stream()
									.filter(h -> osallistujienHakemusOidit
											.contains(h.getOid()))
									.collect(Collectors.toList());
						} catch (Exception e) {
							LOG.error("Osallistumisia ei saatu valintalaskennasta! Valintakokeita oli "
									+ haettavatValintakoeOids.size());
							throw new RuntimeException(
									"Osallistumisia ei saatu valintalaskennasta! Valintakokeita oli "
											+ haettavatValintakoeOids.size()
											+ ". Syy " + e.getMessage());
						}

					}

				})
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(koekutsukirjeiksi(prosessi, koekutsu),
						new Action1<Throwable>() {
							public void call(Throwable t1) {
								LOG.error(
										"Osallistumistietojen haussa hakutoiveelle {}: {}",
										koekutsu.getHakukohdeOid(),
										t1.getMessage());
								prosessi.keskeyta();
							}
						});

	}

	private Action1<List<Hakemus>> koekutsukirjeiksi(
			final KoekutsuProsessi prosessi, final KoekutsuDTO koekutsu) {
		return new Action1<List<Hakemus>>() {
			public void call(List<Hakemus> hakemukset) {
				if (hakemukset.isEmpty()) {
					LOG.error(
							"Hakutoiveeseen {} ei ole hakijoita. Yritettiin muodostaa koekutsukirjetta!",
							koekutsu.getHakukohdeOid());
					throw new RuntimeException(
							"Koekutsuja ei voida muodostaa ilman valintakoetta johon osallistuu edes joku.");
				}
				// // Puuttuvat hakemukset //
				try {
					LOG.info("Luodaan kirje.");
					LetterBatch letterBatch = koekutsukirjeetKomponentti
							.valmistaKoekutsukirjeet(hakemukset,
									koekutsu.getHakuOid(),
									koekutsu.getHakukohdeOid(),
									koekutsu.getLetterBodyText(),
									koekutsu.getTarjoajaOid(),
									koekutsu.getTag(),
									koekutsu.getTemplateName());
					LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
					String batchId = viestintapalveluAsyncResource
							.viePdfJaOdotaReferenssi(letterBatch).get(35L,
									TimeUnit.SECONDS);
					LOG.info("Saatiin kirjeen seurantaId {}", batchId);
					prosessi.vaiheValmistui();
					PublishSubject<String> stop = PublishSubject.create();
					Observable.interval(1, TimeUnit.SECONDS)
							.take(VIESTINTAPALVELUN_MAKSIMI_POLLAUS_SEKUNTIA)
							.takeUntil(stop).subscribe(new Action1<Long>() {
								public void call(Long t1) {
									try {
										LOG.warn(
												"Tehdaan status kutsu seurantaId:lle {}",
												batchId);
										LetterBatchStatusDto status = viestintapalveluAsyncResource
												.haeStatus(batchId).get(900L,
														TimeUnit.MILLISECONDS);
										if ("ready".equals(status.getStatus())) {
											LOG.error("Koekutsukirjeet valmistui!");
											prosessi.valmistui(batchId);
											stop.onNext(null);
										}
									} catch (Exception e) {
										LOG.error(
												"Statuksen haku epaonnistui {}",
												e.getMessage());
									}
								}
							}, new Action1<Throwable>() {
								public void call(Throwable t1) {
									prosessi.keskeyta();
								}
							}, new Action0() {
								public void call() {
									prosessi.keskeyta();
								}
							});
				} catch (Exception e) {
					LOG.error("Virhe hakutoiveelle {}: {}",
							koekutsu.getHakukohdeOid(), e.getMessage());
					prosessi.keskeyta();
				}
			}
		};
	}
}
