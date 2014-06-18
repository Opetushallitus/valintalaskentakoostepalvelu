package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintatapajonoVientiRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoVientiRouteImpl.class);

	private final ValintaperusteetResource valintaperusteetResource;
	private final ApplicationResource applicationResource;
	private final DokumenttiResource dokumenttiResource;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta;
	private final HaeHakuTarjonnaltaKomponentti hakuTarjonnalta;
	private final HakukohdeResource hakukohdeResource;

	@Autowired
	public ValintatapajonoVientiRouteImpl(
			ApplicationResource applicationResource,
			DokumenttiResource dokumenttiResource,
			ValintaperusteetResource valintaperusteetResource,
			HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta,
			HaeHakuTarjonnaltaKomponentti hakuTarjonnalta,
			HakukohdeResource hakukohdeResource) {
		this.applicationResource = applicationResource;
		this.dokumenttiResource = dokumenttiResource;
		this.valintaperusteetResource = valintaperusteetResource;
		this.hakukohdeTarjonnalta = hakukohdeTarjonnalta;
		this.hakuTarjonnalta = hakuTarjonnalta;
		this.hakukohdeResource = hakukohdeResource;
	}

	@Override
	public void configure() throws Exception {
		Endpoint valintatapajonoVienti = endpoint(ValintatapajonoVientiRoute.SEDA_VALINTATAPAJONO_VIENTI);
		Endpoint luontiEpaonnistui = endpoint("direct:valintatapajono_vienti_deadletterchannel");
		from(valintatapajonoVienti)
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
								1 + 1 +
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
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						HakukohdeNimiRDTO hnimi = hakukohdeTarjonnalta
								.haeHakukohdeNimi(hakukohdeOid);
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						// String tarjoajaOid = hnimi.getTarjoajaOid();
						String hakukohdeNimi = new Teksti(hnimi
								.getHakukohdeNimi()).getTeksti();
						// String tarjoajaNimi = new
						// Teksti(hnimi.getTarjoajaNimi()).getTeksti();
						//
						//
						//
						String hakukohde = hakukohdeOid(exchange);
						String valintatapajonoOid = valintatapajonoOid(exchange);

						final List<Hakemus> hakemukset;

						try {
							hakemukset = applicationResource
									.getApplicationsByOid(
											hakukohde,
											ApplicationResource.ACTIVE_AND_INCOMPLETE,
											Integer.MAX_VALUE);
							LOG.debug("Saatiin hakemukset {}",
									hakemukset.size());
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error("Hakemuspalvelun virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.HAKU,
											"Hakemuspalvelulta ei saatu hakemuksia hakukohteelle",
											""));
							throw e;
						}
						if (hakemukset.isEmpty()) {
							LOG.error("Nolla hakemusta!");

							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.HAKU,
											"Hakukohteella ei ole hakemuksia!",
											""));
							throw new RuntimeException(
									"Hakukohteelle saatiin tyhjä hakemusjoukko!");
						}
						final List<ValinnanvaiheDTO> valinnanvaiheet;
						try {
							valinnanvaiheet = hakukohdeResource
									.hakukohde(hakukohde);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error("Valinnanvaiheiden haku virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTALASKENTA,
											"Valintalaskennalta ei saatu valinnanvaiheita",
											""));
							throw e;
						}
						// if (valinnanvaiheet.isEmpty()) {
						// LOG.error("Nolla valinnanvaihetta!");
						//
						// dokumenttiprosessi(exchange)
						// .getPoikkeukset()
						// .add(new Poikkeus(
						// Poikkeus.VALINTALASKENTA,
						// "Hakukohteelle ei löytynyt valinnanvaiheita!",
						// ""));
						// throw new RuntimeException(
						// "Hakukohteelle ei löytynyt valinnanvaiheita!");
						// }
						InputStream xlsx;
						try {
							ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
									hakuOid, hakukohdeOid, valintatapajonoOid,
									hakuNimi, hakukohdeNimi,
									//
									valinnanvaiheet, hakemukset);
							xlsx = valintatapajonoExcel.getExcel().vieXlsx();
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error(
									"Valintatapajono excelin luonti virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.KOOSTEPALVELU,
											"Valintatapajono exceliä ei saatu luotua!",
											""));
							throw e;
						}
						try {
							String id = generateId();
							Long expirationTime = defaultExpirationDate()
									.getTime();
							List<String> tags = dokumenttiprosessi(exchange)
									.getTags();
							// LOG.error("Excelin tallennus");
							dokumenttiResource.tallenna(id,
									"valintatapajono.xlsx", expirationTime,
									tags, "application/octet-stream", xlsx);
							dokumenttiprosessi(exchange).setDokumenttiId(id);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error(
									"Dokumenttipalveluun vienti virhe: {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Dokumenttipalveluun ei saatu vietyä taulukkolaskentatiedostoa!",
											""));
							throw e;
						}
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
							syy = "Valintatapajonon taulukkolaskentaan vienti epäonnistui. Ota yheys ylläpitoon.";
						} else {
							syy = exchange.getException().getMessage();
						}
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.KOOSTEPALVELU,
										"Valintatapajonon vienti", syy));
					}
				})
				//
				.stop();
	}

	protected String valintatapajonoOid(Exchange exchange) {
		return exchange.getProperty(OPH.VALINTAPAJONOOID, String.class);
	}
}
