package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static rx.Observable.from;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jersey.repackaged.com.google.common.util.concurrent.Futures;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rx.schedulers.Schedulers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviKuuntelija;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class ErillishaunTuontiServiceImpl implements ErillishaunTuontiService {

	private static final Logger LOG = LoggerFactory
			.getLogger(ErillishaunTuontiServiceImpl.class);
	private final TilaAsyncResource tilaAsyncResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final HenkiloAsyncResource henkiloAsyncResource;
	private final org.joda.time.format.DateTimeFormatter dtf = DateTimeFormat
			.forPattern("dd.MM.yyyy");

	@Autowired
	public ErillishaunTuontiServiceImpl(TilaAsyncResource tilaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			HenkiloAsyncResource henkiloAsyncResource) {
		this.applicationAsyncResource = applicationAsyncResource;
		this.tilaAsyncResource = tilaAsyncResource;
		this.henkiloAsyncResource = henkiloAsyncResource;
	}

	@Override
	public void tuo(KirjeProsessi prosessi, ErillishakuDTO erillishaku,
			InputStream data) {
		LOG.error("Aloitetaan tuonti");
		// applicationAsyncResource.getApplicationsByOid(erillishaku.getHakuOid(),
		// erillishaku.getHakukohdeOid())
		from(Futures.immediateFuture(new Object()))
		//
				.subscribeOn(Schedulers.newThread())
				//
				.subscribe(
						object -> {
							ErillishakuExcel erillishakuExcel;
							final List<HenkiloCreateDTO> henkiloPrototyypit = Lists
									.newArrayList();
							final Map<String, ErillishakuRivi> hetuToRivi = Maps
									.newHashMap();
							try {
								erillishakuExcel = new ErillishakuExcel(
										erillishaku.getHakutyyppi(),
										new ErillishakuRiviKuuntelija() {
											public void erillishakuRiviTapahtuma(
													ErillishakuRivi rivi) {
												if (rivi.getHenkilotunnus() == null
														|| rivi.getSyntymaAika() == null) {
													LOG.warn(
															"Käyttökelvoton rivi {}",
															rivi);
													return;
												}
												// rivi.get
												HenkiloCreateDTO henkilo = new HenkiloCreateDTO();
												henkilo.setEtunimet(rivi
														.getEtunimi());
												henkilo.setSukunimi(rivi
														.getSukunimi());
												henkilo.setHetu(rivi
														.getHenkilotunnus());
												henkilo.setHenkiloTyyppi(HenkiloTyyppi.OPPIJA);
												try {
													henkilo.setSyntymaaika(dtf
															.parseDateTime(
																	rivi.getSyntymaAika())
															.toDate());
													// dtf. );
												} catch (Exception e) {
													LOG.error(
															"Syntymäaikaa {} ei voitu paria muodolle dd.MM.yyyy",
															rivi.getSyntymaAika());
												}
												hetuToRivi.put(
														Optional.ofNullable(
																StringUtils
																		.trimToNull(rivi
																				.getHenkilotunnus()))
																.orElse(rivi
																		.getSyntymaAika()),
														rivi);
												henkiloPrototyypit.add(henkilo);
											}
										});
							} catch (Exception e) {
								LOG.error("Excelin muodostus epaonnistui! {}",
										e.getMessage());
								throw new RuntimeException(e);
							}
							final Collection<ErillishaunHakijaDTO> hakijat;
							try {
								erillishakuExcel.getExcel().tuoXlsx(data);
								hakijat = applicationAsyncResource
										.putApplicationPrototypes(
												erillishaku.getHakuOid(),
												erillishaku.getHakukohdeOid(),
												erillishaku.getTarjoajaOid(),
												henkiloAsyncResource
														.haeHenkilot(
																henkiloPrototyypit)
														.get()
														.stream()
														.map(h -> {
															LOG.error(
																	"Hakija {}",
																	new GsonBuilder()
																			.setPrettyPrinting()
																			.create()
																			.toJson(h));
															HakemusPrototyyppi hp = new HakemusPrototyyppi();
															hp.setEtunimi(h
																	.getEtunimet());
															hp.setSukunimi(h
																	.getSukunimi());
															hp.setHakijaOid(h
																	.getOidHenkilo());
															hp.setHenkilotunnus(h
																	.getHetu());
															
															// hp.setSyntymaAika(//h.getY);
															return hp;
														})
														.collect(
																Collectors
																		.toList()))
										.get()
										.stream()
										.map(hakemus -> {
											ErillishaunHakijaDTO hakija = new ErillishaunHakijaDTO();
											HakemusWrapper wrapper = new HakemusWrapper(
													hakemus);
											ErillishakuRivi rivi = hetuToRivi.get(wrapper
													.getHenkilotunnusTaiSyntymaaika());
											try {
												hakija.setHakemuksenTila(HakemuksenTila.valueOf(rivi
														.getHakemuksenTila()));
											} catch (Exception e) {
												// tuntematon tila tai
												// todennakoisesti asettamatta
												hakija.setHakemuksenTila(HakemuksenTila.HYLATTY);
											}
											hakija.setHakemusOid(hakemus
													.getOid());
											hakija.setHakijaOid(hakemus
													.getPersonOid());
											hakija.setHakukohdeOid(erillishaku
													.getHakukohdeOid());
											hakija.setHakuOid(erillishaku
													.getHakuOid());
											hakija.setSukunimi(wrapper
													.getSukunimi());
											hakija.setEtunimi(wrapper
													.getEtunimi());
											try {
												hakija.setIlmoittautumisTila(fi.vm.sade.sijoittelu.domain.IlmoittautumisTila.valueOf(rivi
														.getIlmoittautumisTila()));
											} catch (Exception e) {
												// tuntematon tila tai
												// todennakoisesti asettamatta
												hakija.setIlmoittautumisTila(null);
											}
											try {
												hakija.setValintatuloksenTila(fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.valueOf(rivi
														.getVastaanottoTila()));
											} catch (Exception e) {
												// tuntematon tila tai
												// todennakoisesti asettamatta
												hakija.setIlmoittautumisTila(null);
											}
											hakija.setJulkaistavissa(rivi
													.isJulkaistaankoTiedot());
											hakija.setTarjoajaOid(erillishaku
													.getTarjoajaOid());
											hakija.setValintatapajonoOid(erillishaku
													.getValintatapajonoOid());
											return hakija;
										}).collect(Collectors.toList());

							} catch (Throwable e) {
								LOG.error("Excelin tuonti epaonnistui! {} {}",
										e.getMessage(),
										Arrays.toString(e.getStackTrace()));
								throw new RuntimeException(e);
							}
							LOG.error("Viedaan hakijoita {} jonoon {}",
									hakijat.size(),
									erillishaku.getValintatapajononNimi());
							if (!hakijat.isEmpty()) {
								tilaAsyncResource.tuoErillishaunTilat(
										erillishaku.getHakuOid(),
										erillishaku.getHakukohdeOid(),
										erillishaku.getValintatapajononNimi(),
										hakijat);
							} else {
								LOG.error("Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
								throw new RuntimeException(
										"Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
							}
							prosessi.vaiheValmistui();
							prosessi.valmistui("ok");
						},
						poikkeus -> {
							if (poikkeus == null) {
								LOG.error("Suoritus keskeytyi tuntemattomaan NPE poikkeukseen!");
							} else {
								LOG.error(
										"Erillishaun tuonti keskeytyi virheeseen {}. {}",
										poikkeus.getMessage(), Arrays
												.toString(poikkeus
														.getStackTrace()));
							}
							prosessi.keskeyta();
						});
	}

	public void erillishaunTuonti() {

	}
}
