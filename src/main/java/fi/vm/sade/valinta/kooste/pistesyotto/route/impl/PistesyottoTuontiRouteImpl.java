package fi.vm.sade.valinta.kooste.pistesyotto.route.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoArvo;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoRivi;
import fi.vm.sade.valinta.kooste.pistesyotto.route.PistesyottoTuontiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class PistesyottoTuontiRouteImpl extends AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(PistesyottoTuontiRouteImpl.class);

	private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
	private final ValintaperusteetAsyncResource valintaperusteetResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final ApplicationResource applicationResource;

	@Autowired
	public PistesyottoTuontiRouteImpl(
			ValintalaskentaValintakoeAsyncResource valintakoeResource,
			ApplicationAsyncResource applicationAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetResource,
			ApplicationResource applicationResource) {
		this.valintakoeResource = valintakoeResource;
		this.applicationResource = applicationResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.valintaperusteetResource = valintaperusteetResource;
	}

	@Override
	public void configure() throws Exception {
		Endpoint pistesyottoTuonti = endpoint(PistesyottoTuontiRoute.SEDA_PISTESYOTTO_TUONTI);
		Endpoint luontiEpaonnistui = endpoint("direct:pistesyotto_tuonti_deadletterchannel");
		from(pistesyottoTuonti)
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(true))
				//
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).setKokonaistyo(
						// osallistumistiedot + valintaperusteet +
						// hakemuspistetiedot
								1 + 1
								// luonti
								+ 1
								// tuonti hakupalveluun
								+ 1);
						String hakuOid = hakuOid(exchange);
						String hakukohdeOid = hakukohdeOid(exchange);
						String hakuNimi = StringUtils.EMPTY;
						String hakukohdeNimi = StringUtils.EMPTY;
						String tarjoajaNimi = StringUtils.EMPTY;
						Future<List<ValintakoeOsallistuminenDTO>> osallistumistiedotFuture = valintakoeResource
								.haeHakutoiveelle(hakukohdeOid);
						Future<List<ValintaperusteDTO>> valintaperusteetFuture = valintaperusteetResource
								.findAvaimet(hakukohdeOid);
						Future<List<ApplicationAdditionalDataDTO>> pistetiedotFuture = applicationAsyncResource
								.getApplicationAdditionalData(hakuOid,
										hakukohdeOid);
						// LOG.error("Osallistumistiedot");
						List<ValintakoeOsallistuminenDTO> osallistumistiedot;
						try {
							osallistumistiedot = osallistumistiedotFuture.get();
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTALASKENTA,
											"Osallistumistietojen haku valintalaskennoista epäonnistui. ",
											e.getMessage(), Poikkeus
													.hakukohdeOid(hakukohdeOid)));
							throw e;
						}
						// LOG.error("Valintaperusteet");
						List<ValintaperusteDTO> valintaperusteet;
						try {
							valintaperusteet = valintaperusteetFuture.get();
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTAPERUSTEET,
											"Valintaperusteiden haku epäonnistui. ",
											e.getMessage(), Poikkeus
													.hakukohdeOid(hakukohdeOid)));
							throw e;
						}
						Collection<String> valintakoeTunnisteet = FluentIterable
								.from(valintaperusteet)
								.transform(
										new Function<ValintaperusteDTO, String>() {
											@Override
											public String apply(
													ValintaperusteDTO input) {
												return input.getTunniste();
											}
										}).toList();
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// LOG.error("Additional data");
						List<ApplicationAdditionalDataDTO> pistetiedot;
						try {
							pistetiedot = pistetiedotFuture.get();
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.HAKU,
											"Pistetietojen haku epäonnistui. ",
											e.getMessage(), Poikkeus
													.hakuOid(hakuOid), Poikkeus
													.hakukohdeOid(hakukohdeOid)));
							throw e;
						}
						List<Hakemus> hakemukset = Collections.emptyList();
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// LOG.error("Excelin luonti");
						PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
						PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
								hakuOid, hakukohdeOid, null, hakuNimi,
								hakukohdeNimi, tarjoajaNimi, hakemukset,
								Collections.emptySet(),
								valintakoeTunnisteet, osallistumistiedot,
								valintaperusteet, pistetiedot,
								pistesyottoTuontiAdapteri);
						pistesyottoExcel.getExcel().tuoXlsx(
								exchange.getIn().getBody(InputStream.class));
						Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = asMap(pistetiedot);
						List<ApplicationAdditionalDataDTO> uudetPistetiedot = Lists
								.newArrayList();
						for (PistesyottoRivi rivi : pistesyottoTuontiAdapteri
								.getRivit()) {
							ApplicationAdditionalDataDTO additionalData = pistetiedotMapping
									.get(rivi.getOid());
							Map<String, String> newPistetiedot = rivi
									.asAdditionalData();

							if (rivi.isValidi()) {
								LOG.debug("Rivi on muuttunut ja eheä. Tehdään päivitys hakupalveluun");
								additionalData
										.setAdditionalData(newPistetiedot);
								uudetPistetiedot.add(additionalData);
							} else {
								LOG.warn("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");

								for (PistesyottoArvo arvo : rivi.getArvot()) {
									if (!arvo.isValidi()) {
										String virheIlmoitus = new StringBuffer()
												.append("Henkilöllä ")
												.append(rivi.getNimi())
												//
												.append(" (")
												.append(rivi.getOid())
												.append(")")
												//
												.append(" oli virheellinen arvo '")
												.append(arvo.getArvo())
												.append("'")
												.append(" kohdassa ")
												.append(arvo.getTunniste())
												.toString();
										dokumenttiprosessi(exchange)
												.getPoikkeukset()
												.add(new Poikkeus(
														"Pistesyötön tuonti",
														"", virheIlmoitus));
										throw new RuntimeException(
												virheIlmoitus);
									}
								}
							}
						}
						applicationResource.putApplicationAdditionalData(
								hakuOid, hakukohdeOid, uudetPistetiedot);
						dokumenttiprosessi(exchange).setDokumenttiId("valmis");
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// LOG.error("Done");
					}
				})
				//
				.stop();
		/**
		 * DEAD LETTER CHANNEL
		 */
		from(luontiEpaonnistui)
		//
		.log(LoggingLevel.ERROR,
						"Pistesyoton tuonti epaonnistui: ${exception.message}\r\n${exception.stacktrace}")
						//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						if (dokumenttiprosessi(exchange).getPoikkeukset()
								.isEmpty()) {
							String syy;
							if (exchange.getException() == null) {
								syy = "Pistesyötön tuonti taulukkolaskennalla epäonnistui. Ota yhteys ylläpitoon.";
							} else {
								syy = exchange.getException().getMessage();
							}
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Pistesyötön tuonti:", syy));
						}
					}
				})
				//
				.stop();
	}

	private Map<String, ApplicationAdditionalDataDTO> asMap(
			Collection<ApplicationAdditionalDataDTO> datas) {
		Map<String, ApplicationAdditionalDataDTO> mapping = Maps.newHashMap();
		for (ApplicationAdditionalDataDTO data : datas) {
			mapping.put(data.getOid(), data);
		}
		return mapping;
	}
}
