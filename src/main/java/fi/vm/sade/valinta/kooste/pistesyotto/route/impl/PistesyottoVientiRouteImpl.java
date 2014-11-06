package fi.vm.sade.valinta.kooste.pistesyotto.route.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAvaimetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.route.PistesyottoVientiRoute;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.tulos.resource.ValintakoeResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class PistesyottoVientiRouteImpl extends AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(PistesyottoVientiRouteImpl.class);

	private final ValintalaskentaValintakoeAsyncResource valintakoeResource;
	private final ValintaperusteetAvaimetAsyncResource valintaperusteetResource;
	private final DokumenttiResource dokumenttiResource;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta;
	private final HaeHakuTarjonnaltaKomponentti hakuTarjonnalta;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final ValintaperusteetValintakoeAsyncResource valintaperusteetValintakoeResource;
	@Autowired
	public PistesyottoVientiRouteImpl(
			ValintalaskentaValintakoeAsyncResource valintakoeResource,
			DokumenttiResource dokumenttiResource,
			ValintaperusteetAvaimetAsyncResource valintaperusteetResource,
			HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta,
			HaeHakuTarjonnaltaKomponentti hakuTarjonnalta,
			ApplicationAsyncResource applicationAsyncResource,
			ValintaperusteetValintakoeAsyncResource valintaperusteetValintakoeResource) {
		this.valintaperusteetValintakoeResource = valintaperusteetValintakoeResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.valintakoeResource = valintakoeResource;
		this.valintaperusteetResource = valintaperusteetResource;
		this.dokumenttiResource = dokumenttiResource;
		this.hakukohdeTarjonnalta = hakukohdeTarjonnalta;
		this.hakuTarjonnalta = hakuTarjonnalta;
	}

	@Override
	public void configure() throws Exception {
		Endpoint pistesyottoVienti = endpoint(PistesyottoVientiRoute.SEDA_PISTESYOTTO_VIENTI);
		Endpoint luontiEpaonnistui = endpoint("direct:pistesyotto_vienti_deadletterchannel");
		from(pistesyottoVienti)
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {

						dokumenttiprosessi(exchange).setKokonaistyo(
						// haun nimi ja hakukohteen nimi
								1 + 1 + 1 +
								// osallistumistiedot + valintaperusteet +
								// hakemuspistetiedot
										1 + 1
										// luonti
										+ 1
										// dokumenttipalveluun vienti
										+ 1);
						String hakuOid = hakuOid(exchange);
						String hakukohdeOid = hakukohdeOid(exchange);
						String hakuNimi = new Teksti(hakuTarjonnalta.getHaku(
								hakuOid).getNimi()).getTeksti();
						Future<List<ValintakoeOsallistuminenDTO>> osallistumistiedotFuture = valintakoeResource
								.haeHakutoiveelle(hakukohdeOid);
						Future<List<Hakemus>> hakemuksetFuture = applicationAsyncResource
								.getApplicationsByOid(hakuOid, hakukohdeOid);
						Future<List<ValintaperusteDTO>> valintaperusteetFuture = valintaperusteetResource
								.findAvaimet(hakukohdeOid);

						Future<List<ApplicationAdditionalDataDTO>> pistetiedotFuture = applicationAsyncResource
								.getApplicationAdditionalData(hakuOid,
										hakukohdeOid);
						Future<List<HakukohdeJaValintakoeDTO>> hakukohdeJaValintakoeFuture =
						valintaperusteetValintakoeResource.haeValintakokeetHakukohteille(Arrays.asList(hakukohdeOid));
						
						HakukohdeDTO hnimi = hakukohdeTarjonnalta
								.haeHakukohdeNimi(hakukohdeOid);
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita(); // TARJONTA
																				// HAETTU
						String tarjoajaOid = hnimi.getTarjoajaOid();
						String hakukohdeNimi = new Teksti(hnimi
								.getHakukohdeNimi()).getTeksti();
						String tarjoajaNimi = new Teksti(hnimi
								.getTarjoajaNimi()).getTeksti();
						// LOG.error("Excelin luonti");

						List<Hakemus> hakemukset = null;
						try {
							hakemukset = hakemuksetFuture.get();
						} catch (Exception e) {
							LOG.error("Hakemusten haku epaonnistui {}",
									e.getMessage());
							throw e;
						}
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita(); // HAKEMUKSET
						List<ValintaperusteDTO> valintaperusteet = null;
						Collection<String> valintakoeTunnisteet = null;
						try {
							valintaperusteet = valintaperusteetFuture.get();
							valintakoeTunnisteet = FluentIterable
									.from(valintaperusteet)
									.transform(
											new Function<ValintaperusteDTO, String>() {
												@Override
												public String apply(
														ValintaperusteDTO input) {
													return input.getTunniste();
												}
											}).toList();
						} catch (Exception e) {
							LOG.error(
									"Tunnisteiden haku valintaperusteista epaonnistui {}",
									e.getMessage());
							throw e;
						}
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita(); // VALINTAPERUSTEET
						List<ApplicationAdditionalDataDTO> pistetiedot = null;
						try {
							pistetiedot = pistetiedotFuture.get();
						} catch (Exception e) {
							LOG.error(
									"Pistetietojen haku hakuapp:sta epaonnistui {}",
									e.getMessage());
							throw e;
						}
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita(); // PISTETIEDOT
						List<ValintakoeOsallistuminenDTO> osallistumistiedot = null;
						try {
							osallistumistiedot = osallistumistiedotFuture.get();
						} catch (Exception e) {
							LOG.error(
									"Osallistumistietojen haku valintalaskennasta epaonnistui {}",
									e.getMessage());
							throw e;
						}
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita(); // OSALLISTUMISTIEDOT
						
						Set<String> kaikkiKutsutaanTunnisteet = //Collections.emptySet(); 
						hakukohdeJaValintakoeFuture.get().stream().flatMap(h -> h.getValintakoeDTO().stream()).filter(v -> Boolean.TRUE.equals(v.getKutsutaankoKaikki())).map(v -> v.getTunniste()).collect(Collectors.toSet());
						
						PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
								hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi,
								hakukohdeNimi, tarjoajaNimi, hakemukset,
								kaikkiKutsutaanTunnisteet,
								valintakoeTunnisteet, osallistumistiedot,
								valintaperusteet, pistetiedot);
						InputStream xlsx = pistesyottoExcel.getExcel()
								.vieXlsx();
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						String id = generateId();
						Long expirationTime = defaultExpirationDate().getTime();
						List<String> tags = dokumenttiprosessi(exchange)
								.getTags();
						// LOG.error("Excelin tallennus");
						dokumenttiResource.tallenna(id, "pistesyotto.xlsx",
								expirationTime, tags,
								"application/octet-stream", xlsx);
						dokumenttiprosessi(exchange).setDokumenttiId(id);
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
						"Pistesyoton vienti epaonnistui: ${exception.message}\r\n${exception.stacktrace}")
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String syy;
						if (exchange.getException() == null) {
							syy = "Pistesyötön taulukkolaskentaan vienti epäonnistui. Ota yheys ylläpitoon.";
						} else {
							syy = exchange.getException().getMessage();
						}
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.KOOSTEPALVELU,
										"Pistesyötön vienti", syy));
					}
				})
				//
				.stop();
	}
}
