package fi.vm.sade.valinta.kooste.pistesyotto.route.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
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

	private final ValintakoeResource valintakoeResource;
	private final ApplicationResource applicationResource;
	private final ValintaperusteetResource hakukohdeResource;
	private final DokumenttiResource dokumenttiResource;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta;
	private final HaeHakuTarjonnaltaKomponentti hakuTarjonnalta;

	@Autowired
	public PistesyottoVientiRouteImpl(ValintakoeResource valintakoeResource,
			ApplicationResource applicationResource,
			DokumenttiResource dokumenttiResource,
			ValintaperusteetResource hakukohdeResource,
			HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta,
			HaeHakuTarjonnaltaKomponentti hakuTarjonnalta) {
		this.valintakoeResource = valintakoeResource;
		this.applicationResource = applicationResource;
		this.hakukohdeResource = hakukohdeResource;
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
						String hakuOid = hakuOid(exchange);
						String hakukohdeOid = hakukohdeOid(exchange);
						String hakuNimi = new Teksti(hakuTarjonnalta.getHaku(
								hakuOid).getNimi()).getTeksti();
						HakukohdeNimiRDTO hnimi = hakukohdeTarjonnalta
								.haeHakukohdeNimi(hakukohdeOid);
						String tarjoajaOid = hnimi.getTarjoajaOid();
						String hakukohdeNimi = new Teksti(hnimi
								.getHakukohdeNimi()).getTeksti();
						String tarjoajaNimi = new Teksti(hnimi
								.getTarjoajaNimi()).getTeksti();
						dokumenttiprosessi(exchange).setKokonaistyo(
						// haun nimi ja hakukohteen nimi
								1 + 1 +
								// osallistumistiedot + valintaperusteet +
								// hakemuspistetiedot
										1 + 1 + 1
										// luonti
										+ 1
										// dokumenttipalveluun vienti
										+ 1);
						// LOG.error("Osallistumistiedot");
						List<ValintakoeOsallistuminenDTO> osallistumistiedot = valintakoeResource
								.hakuByHakutoive(hakukohdeOid);
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// LOG.error("Valintaperusteet");
						List<ValintaperusteDTO> valintaperusteet = hakukohdeResource
								.findAvaimet(hakukohdeOid);
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
						List<ApplicationAdditionalDataDTO> pistetiedot = applicationResource
								.getApplicationAdditionalData(hakuOid,
										hakukohdeOid);
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// LOG.error("Excelin luonti");
						PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(
								hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi,
								hakukohdeNimi, tarjoajaNimi,
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
