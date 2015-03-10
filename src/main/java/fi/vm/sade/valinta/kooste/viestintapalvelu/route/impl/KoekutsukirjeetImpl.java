package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static rx.Observable.from;
import static rx.Observable.zip;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
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
	private final ValintaperusteetAsyncResource valintakoeResource;
	private final ValintalaskentaValintakoeAsyncResource osallistumisetResource;

	@Autowired
	public KoekutsukirjeetImpl(
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ApplicationAsyncResource applicationAsyncResource,
			ViestintapalveluAsyncResource viestintapalveluAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetValintakoeAsyncResource,
			ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.applicationAsyncResource = applicationAsyncResource;
		this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
		this.valintakoeResource = valintaperusteetValintakoeAsyncResource;
		this.osallistumisetResource = valintalaskentaValintakoeAsyncResource;
	}

	@Override
	public void koekutsukirjeetHakemuksille(KirjeProsessi prosessi,
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
	public void koekutsukirjeetOsallistujille(KirjeProsessi prosessi,
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
			final KirjeProsessi prosessi, final KoekutsuDTO koekutsu) {
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
					Function<Hakemus, Stream<String>> hakutoiveetHakemuksesta = h -> (Stream<String>) new HakemusWrapper(
							h).getHakutoiveet()
							.entrySet()
							.stream()
							//
							.filter(Objects::nonNull)
							//
							.filter(e -> {
								return StringUtils.trimToEmpty(e.getKey())
										.endsWith("Opetuspiste-id");
							})
							//
							.map(e -> {
								return e.getValue();
							});

					LOG.error("Haetaan valintakokeet hakutoiveille!");
					final Map<String, HakukohdeJaValintakoeDTO> valintakoeOidsHakutoiveille;
					try {
						Set<String> hakutoiveetKaikistaHakemuksista = Sets
								.newHashSet(hakemukset.stream()
										.flatMap(hakutoiveetHakemuksesta)
										.collect(Collectors.toSet()));
						hakutoiveetKaikistaHakemuksista.add(koekutsu
								.getHakukohdeOid());
						LOG.error("Hakutoiveet hakemuksista:\r\n{}", Arrays
								.toString(hakutoiveetKaikistaHakemuksista
										.toArray()));
						valintakoeOidsHakutoiveille = valintakoeResource
								.haeValintakokeetHakukohteille(
										hakutoiveetKaikistaHakemuksista)
								.get()
								.stream()
								//
								.filter(h -> h.getValintakoeDTO() != null
										&& !h.getValintakoeDTO().isEmpty())
								//
								.collect(
										Collectors.toMap(
												h -> h.getHakukohdeOid(),
												h -> h));
						LOG.error("\r\n{}",
								new GsonBuilder().setPrettyPrinting().create()
										.toJson(valintakoeOidsHakutoiveille));
						if (valintakoeOidsHakutoiveille.isEmpty()) {
							throw new RuntimeException(
									"Yhdellekaan hakutoiveelle ei loytynyt valintakokeita!");
						}
					} catch (Exception e) {
						LOG.error(
								"Valintakokeiden haku hakutoiveille epaonnistui! {}",
								e.getMessage());
						throw e;
					}
					final Map<String, Collection<String>> hakemusOidJaHakijanMuutHakutoiveOids;
					final Set<String> kohdeHakukohteenTunnisteet;
					try {
						LOG.error(
								"Haetaan tunnisteet kohde valintakokeille: Onko valintakoeOid {}",
								valintakoeOidsHakutoiveille
										.containsKey(koekutsu.getHakukohdeOid()));

						kohdeHakukohteenTunnisteet = valintakoeOidsHakutoiveille
								.get(koekutsu.getHakukohdeOid())
								.getValintakoeDTO().stream()
								//
								.filter(Objects::nonNull)
								//
								.filter(v -> Boolean.TRUE.equals(v
										.getAktiivinen()))
								//
								.map(v -> v.getTunniste())
								//
								.collect(Collectors.toSet());
						LOG.error("Mapataan muut hakukohteet");
						hakemusOidJaHakijanMuutHakutoiveOids = hakemukset
								.stream()
								//
								.filter(Objects::nonNull)
								//
								.filter(h -> h.getOid() != null)
								//
								.collect(
										Collectors.toMap(
												h -> h.getOid(),
												h -> hakutoiveetHakemuksesta
														.apply(h)
														//
														.filter(Objects::nonNull)
														//
														// jos joku hakutoive
														// sisaltaa
														// valintakokeen
														// jolla sama tunniste
														// kuin
														// taman hakukohteen
														// valintakokeilla
														//
														.filter(hakutoive -> valintakoeOidsHakutoiveille
																.containsKey(hakutoive))
														//
														.filter(hakutoive -> valintakoeOidsHakutoiveille
																.get(hakutoive)
																.getValintakoeDTO()
																.stream()
																.filter(Objects::nonNull)
																//
																.filter(v -> Boolean.TRUE
																		.equals(v
																				.getAktiivinen()))
																//
																.filter(v -> null != v
																		.getTunniste())
																//
																.anyMatch(
																		v -> kohdeHakukohteenTunnisteet
																				.contains(v
																						.getTunniste())))
														//
														.collect(
																Collectors
																		.toList())));
					} catch (Exception e) {
						LOG.error(
								"Muiden hakukohteiden mappauksessa tapahtui odottamaton virhe {}",
								e.getMessage());
						throw e;
					}
					LOG.info("Luodaan kirje.");
					LetterBatch letterBatch = koekutsukirjeetKomponentti
							.valmistaKoekutsukirjeet(hakemukset,
									koekutsu.getHakuOid(),
									koekutsu.getHakukohdeOid(),
									hakemusOidJaHakijanMuutHakutoiveOids,
									koekutsu.getLetterBodyText(),
									koekutsu.getTarjoajaOid(),
									koekutsu.getTag(),
									koekutsu.getTemplateName());
					LOG.info("Tehdaan viestintapalvelukutsu kirjeille.");
					LetterResponse batchId = viestintapalveluAsyncResource
							.viePdfJaOdotaReferenssi(letterBatch).get(35L,
									TimeUnit.SECONDS);
					LOG.info("Saatiin kirjeen seurantaId {}", batchId.getBatchId());
					prosessi.vaiheValmistui();
                    if(batchId.getStatus().equals(LetterResponse.STATUS_SUCCESS)) {
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
                                                        .haeStatus(batchId.getBatchId())
                                                        .get(900L,
                                                                TimeUnit.MILLISECONDS);
                                                if ("error".equals(status
                                                        .getStatus())) {
                                                    LOG.error("Koekutsukirjeiden muodostus paattyi viestintapalvelun sisaiseen virheeseen!");
                                                    prosessi.keskeyta();
                                                    stop.onNext(null);
                                                }
                                                if ("ready".equals(status
                                                        .getStatus())) {
                                                    prosessi.vaiheValmistui();
                                                    LOG.error("Koekutsukirjeet valmistui!");
                                                    prosessi.valmistui(batchId.getBatchId());
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
                    }  else {
                        prosessi.keskeyta("Hakemuksissa oli virheitä", batchId.getErrors());
                    }
				} catch (Exception e) {
					LOG.error("Virhe hakutoiveelle {}: {} {}",
							koekutsu.getHakukohdeOid(), e.getMessage(), Arrays.toString(e.getStackTrace()));
					prosessi.keskeyta();
				}
			}
		};
	}
}
