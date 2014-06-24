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

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoDataRiviKuuntelija;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoTuontiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;

public class ValintatapajonoTuontiRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoTuontiRouteImpl.class);

	private final ValintaperusteetResource valintaperusteetResource;
	private final ApplicationResource applicationResource;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta;
	private final HaeHakuTarjonnaltaKomponentti hakuTarjonnalta;
	private final HakukohdeResource hakukohdeResource;

	@Autowired
	public ValintatapajonoTuontiRouteImpl(
			ApplicationResource applicationResource,
			ValintaperusteetResource valintaperusteetResource,
			HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta,
			HaeHakuTarjonnaltaKomponentti hakuTarjonnalta,
			HakukohdeResource hakukohdeResource) {
		super();
		this.applicationResource = applicationResource;
		this.valintaperusteetResource = valintaperusteetResource;
		this.hakukohdeTarjonnalta = hakukohdeTarjonnalta;
		this.hakuTarjonnalta = hakuTarjonnalta;
		this.hakukohdeResource = hakukohdeResource;
	}

	@Override
	public void configure() throws Exception {
		Endpoint valintatapajonoTuonti = endpoint(ValintatapajonoTuontiRoute.SEDA_VALINTATAPAJONO_TUONTI);
		Endpoint luontiEpaonnistui = endpoint("direct:valintatapajono_tuonti_deadletterchannel");
		from(valintatapajonoTuonti)
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
						String valintatapajonoOid = valintatapajonoOid(exchange);
						if (hakukohdeOid == null || hakuOid == null
								|| valintatapajonoOid == null) {
							LOG.error(
									"Pakolliset tiedot reitille puuttuu hakuOid = {}, hakukohdeOid = {}, valintatapajonoOid = {}",
									hakuOid, hakukohdeOid, valintatapajonoOid);

							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Puutteelliset lähtötiedot"));
							throw new RuntimeException(
									"Pakolliset tiedot reitille puuttuu hakuOid, hakukohdeOid, valintatapajonoOid");
						}
						final List<Hakemus> hakemukset;

						try {
							hakemukset = applicationResource
									.getApplicationsByOid(
											hakukohdeOid,
											ApplicationResource.ACTIVE_AND_INCOMPLETE,
											ApplicationResource.MAX);
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
									.hakukohde(hakukohdeOid);
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
						try {
							ValintatapajonoDataRiviKuuntelija k = new ValintatapajonoDataRiviKuuntelija() {
								@Override
								public void valintatapajonoDataRiviTapahtuma(
										ValintatapajonoRivi valintatapajonoRivi) {

								}
							};
							ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
									hakuOid, hakukohdeOid, valintatapajonoOid,
									hakuNimi, hakukohdeNimi,
									//
									valinnanvaiheet, hakemukset, Arrays
											.asList(k));
							valintatapajonoExcel.getExcel()
									.tuoXlsx(
											exchange.getIn().getBody(
													InputStream.class));
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

							dokumenttiprosessi(exchange).setDokumenttiId(
									"valmis");
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

	}
}
