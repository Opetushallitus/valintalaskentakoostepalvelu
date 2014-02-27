package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Property;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

@Component
public class HakuImportRouteImpl extends SpringRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(HakuImportRouteImpl.class);

	private final SuoritaHakuImportKomponentti suoritaHakuImportKomponentti;
	private final SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti;
	private final SecurityPreprocessor securityProcessor;
	private final ValintaperusteService valintaperusteService;
	private final ExecutorService hakuImportThreadPool;

	@Autowired
	public HakuImportRouteImpl(
			@Value("${valintalaskentakoostepalvelu.hakuimport.threadpoolsize:10}") Integer hakuImportThreadpoolSize,
			SuoritaHakuImportKomponentti suoritaHakuImportKomponentti,
			ValintaperusteService valintaperusteService,
			SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti) {
		this.suoritaHakuImportKomponentti = suoritaHakuImportKomponentti;
		this.tarjontaJaKoodistoHakukohteenHakuKomponentti = tarjontaJaKoodistoHakukohteenHakuKomponentti;
		this.valintaperusteService = valintaperusteService;
		this.securityProcessor = new SecurityPreprocessor();
		this.hakuImportThreadPool = Executors
				.newFixedThreadPool(hakuImportThreadpoolSize);
	}

	public static class PrepareHakuImportProcessDescription {

		public Prosessi prepareProcess(
				@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS) String kuvaus,
				@Property(OPH.HAKUOID) String hakuOid) {
			return new HakuImportProsessi(kuvaus, hakuOid);
		}
	}

	@Override
	public void configure() throws Exception {
		/**
		 * Tanne tullaan jos retry:t ei riita importoinnin loppuun vientiin
		 */
		from("direct:tuoHakukohdeDead")
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Valintaperusteiden vienti epäonnistui hakukohteelle ${body}"))
				.to(fail())
				//
				.process(logFailedHakuImport());
		from("direct:hakuimport_epaonnistui")
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Tarjonnasta ei saatu hakua(${property.hakuOid}) tai haun hakukohteiden prosessointi ei mennyt oikein"))
				.to(fail())
				//
				.process(logFailedHakuImport());
		/**
		 * Erillinen reitti viennille(tuonnille). Reitilla oma errorhandler.
		 */

		from("direct:hakuimport_koostepalvelulta_valinnoille")
		//
				.errorHandler(
						deadLetterChannel("direct:tuoHakukohdeDead")
								.maximumRedeliveries(5)
								.redeliveryDelay(200L)
								//
								.logExhaustedMessageHistory(true)
								.logStackTrace(false).logExhausted(true)
								.logRetryStackTrace(false).logHandled(false))
				//
				.bean(valintaperusteService, "tuoHakukohde")
				//
				.process(logSuccessfulHakuImport());

		from("direct:hakuimport_tarjonnasta_koostepalvelulle")
		//
				.errorHandler(
						deadLetterChannel("direct:tuoHakukohdeDead")
								.maximumRedeliveries(5)
								.redeliveryDelay(200L)
								//
								.logExhaustedMessageHistory(true)
								.logStackTrace(false).logExhausted(true)
								.logRetryStackTrace(false).logHandled(false))
				//
				.bean(securityProcessor)
				//
				.bean(tarjontaJaKoodistoHakukohteenHakuKomponentti)
				//
				.process(logSuccessfulHakukohdeGet())
				//
				.to("direct:hakuimport_koostepalvelulta_valinnoille");

		from(hakuImport())
				.errorHandler(
						deadLetterChannel("direct:hakuimport_epaonnistui"))
				// .policy(admin)
				.bean(securityProcessor)
				//
				.setProperty(kuvaus(), constant("Haun importointi"))
				.setProperty(prosessi(),
						method(new PrepareHakuImportProcessDescription()))
				//
				.to(start())
				//
				.bean(suoritaHakuImportKomponentti)
				//
				.process(logSuccessfulHakuGet())
				//
				.split(body())
				//
				.executorService(hakuImportThreadPool)
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.stopOnException()
				//
				.to("direct:hakuimport_tarjonnasta_koostepalvelulle")
				//
				.end()
				//
				.to(finish());

	}

	private String hakuImport() {
		return HakuImportRoute.DIRECT_HAKU_IMPORT;
	}

	private static String fail() {
		return "bean:hakuImportValvomo?method=fail(*,*)";
	}

	private static String start() {
		return "bean:hakuImportValvomo?method=start(*)";
	}

	private static String finish() {
		return "bean:hakuImportValvomo?method=finish(*)";
	}

	private Processor logSuccessfulHakukohdeGet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakuImportProsessi prosessi = exchange.getProperty(
						PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
				int i = prosessi.lisaaImportoitu();
				if (i == prosessi.getHakukohteita()) {
					LOG.info("Kaikki hakukohteet ({}) importoitu!", i);
				}
			}
		};
	}

	private Processor logSuccessfulHakuGet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakuImportProsessi prosessi = exchange.getProperty(
						PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
				@SuppressWarnings("unchecked")
				Collection<String> hakukohdeOids = (Collection<String>) exchange
						.getIn().getBody(Collection.class);
				prosessi.setHakukohteita(hakukohdeOids.size());
				LOG.info("Hakukohteita importoitavana {}", hakukohdeOids.size());
			}
		};
	}

	private Processor logSuccessfulHakuImport() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakuImportProsessi prosessi = exchange.getProperty(
						PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
				int t = prosessi.lisaaTuonti();

				LOG.info("Hakukohde on tuotu onnistuneesti ({}/{}).",
						new Object[] { t, prosessi.getHakukohteita() });
			}
		};
	}

	private Processor logFailedHakuImport() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakuImportProsessi prosessi = exchange.getProperty(
						PROPERTY_VALVOMO_PROSESSI, HakuImportProsessi.class);
				prosessi.lisaaVirhe();
				LOG.error("Virhe (numero {}) hakukohteiden importoinnissa! {}",
						exchange.getException().getMessage());
			}
		};
	}
}
