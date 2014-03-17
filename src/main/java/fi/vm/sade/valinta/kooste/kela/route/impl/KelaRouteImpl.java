package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService.MESSAGE;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.fail;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.finish;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.start;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Property;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.PrepareKelaProcessDescription;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * @author Jussi Jartamo
 * 
 *         Route to Kela.
 */
@Component
public class KelaRouteImpl extends AbstractDokumenttiRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(KelaRouteImpl.class);
	private static final String ENSIMMAINEN_VIRHE = "ensimmainen_virhe_reitilla";
	private final KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;
	private final KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;
	private final SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;
	private final DokumenttiResource dokumenttiResource;
	private final PrepareKelaProcessDescription luoUusiProsessi;
	private final String kelaLuonti;
	private final SecurityPreprocessor security = new SecurityPreprocessor();

	@Autowired
	public KelaRouteImpl(
			@Value(KelaRoute.SEDA_KELA_LUONTI) String kelaLuonti,
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource,
			KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
			KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
			SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet) {
		this.kelaLuonti = kelaLuonti;
		this.luoUusiProsessi = new PrepareKelaProcessDescription();
		this.dokumenttiResource = dokumenttiResource;
		this.kelaHakijaKomponentti = kelaHakijaKomponentti;
		this.sijoitteluVastaanottaneet = sijoitteluVastaanottaneet;
		this.kelaDokumentinLuontiKomponentti = kelaDokumentinLuontiKomponentti;
	}

	public class SendKelaDocument {
		public void send(@Body InputStream filedata,
				@Property(OPH.HAKUOID) String hakuOid) {
			List<String> tags = Lists.newArrayList();
			tags.add(hakuOid);
			tags.add("kela");
			tags.add("valintalaskentakoostepalvelu");
			dokumenttiResource.tallenna(null,
					KelaUtil.createTiedostoNimiYhva14(new Date()), DateTime
							.now().plusDays(1).getMillis(), tags, "", filedata);
		}
	}

	/**
	 * Kela Camel Configuration: Siirto and document generation.
	 */
	public final void configure() {
		/**
		 * Kela-dokkarin luonti reitti
		 */
		from(kelaLuonti)
		//
				.errorHandler(
						deadLetterChannel(kelaFailed())
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(true).logRetryStackTrace(true)
								.logHandled(true))
				//
				.process(security)
				//
				// Start prosessi valvomoon dokumentin luonnin aloittamisesta
				.to(start())
				// ilmoitetaan dokumenttipalveluun aloitetusta luonnista
				// (informoi kayttajaa)
				//
				// haetaan sijoittelusta vastaanottaneet hakijat
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {

						Collection<HakijaDTO> hakijat;
						try {
							exchange.getOut()
									.setBody(
											hakijat = sijoitteluVastaanottaneet
													.vastaanottaneet(hakuOid(exchange)));

						} catch (Exception e) {
							e.printStackTrace();
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.SIJOITTELU,
											"vastaanottaneet", e.getMessage(),
											Poikkeus.hakuOid(hakuOid(exchange))));
							LOG.error(
									"Sijoittelusta ei saatu paikanvastaanottaneita {}",
									e.getMessage());
							throw e;
						}
						if (hakijat.isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.SIJOITTELU,
											"Ei paikan vastaanottaneita",
											"Ei paikan vastaanottaneita",
											Poikkeus.hakuOid(hakuOid(exchange))));
							throw new RuntimeException(
									"Ei paikan vastaanottaneita");
						}
						dokumenttiprosessi(exchange).setKokonaistyo(
								hakijat.size() + 1);
					}
				})

				// List<HakijaDTO> -->
				.split(body(), createAccumulatingAggregation())
				//
				.shareUnitOfWork()
				//

				//
				.parallelProcessing()
				//
				.stopOnException()
				//

				// HakijaDTO -->
				.to("direct:kela_yksittainen_rivi")
				//
				.end()
				// Collection<Collection<TKUVAYHVA>> ->
				.process(new Processor() { // FLATTEN
							@Override
							public void process(Exchange exchange)
									throws Exception {
								exchange.getOut().setBody(
										Iterables.concat((List<?>) exchange
												.getIn().getBody()));
							}
						})
				// Collection<TKUVAYHVA> ->
				.bean(kelaDokumentinLuontiKomponentti)
				// lahetetaan valmis inputstream dokumenttipalveluun kayttajan
				// ladattavaksi. Body == InputStream ->
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						InputStream filedata = exchange.getIn().getBody(
								InputStream.class);
						try {
							String id = generateId();
							dokumenttiResource.tallenna(id, KelaUtil
									.createTiedostoNimiYhva14(new Date()),
									DateTime.now().plusDays(1).getMillis(),
									Arrays.asList("kela"), "kela", filedata);
							dokumenttiprosessi(exchange).setDokumenttiId(id);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Dokumenttipalveluun tallennus epäonnistui: {} {}",
									e.getMessage(), e.getCause());
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Dokumentin tallennus", e
													.getMessage(), Poikkeus
													.hakuOid(hakuOid(exchange))));
							throw e;
						}
					}
				})
				// Done valvomoon
				.to(finish());
		from("direct:kela_yksittainen_rivi")
		//
				.errorHandler(
						deadLetterChannel(kelaFailed())
								// .useOriginalMessage()
								//
								// (kelaFailed())
								//
								.maximumRedeliveries(10)
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false)
				//
				)
				//
				.process(security)
				//
				.bean(kelaHakijaKomponentti)
				//
				.process(new Processor() {

					@Override
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
					}
				});

		from(kelaFailed())
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						AtomicBoolean onkoEnsimmainenVirhe = exchange
								.getProperty(ENSIMMAINEN_VIRHE,
										AtomicBoolean.class);
						if (onkoEnsimmainenVirhe != null
								&& onkoEnsimmainenVirhe.compareAndSet(true,
										false)) {
							dokumenttiResource.viesti(new Message(
									"Kela-dokumentin luonti epäonnistui.",
									Arrays.asList(
											"valintalaskentakoostepalvelu",
											"kela"), DateTime.now().plusDays(1)
											.toDate()));
						}
					}
				})
				// merkkaa prosessi failediksi

				// informoi kayttajaa
				.setHeader(MESSAGE,
						constant("Kela-dokumentin luonti epäonnistui."))
				//
				.to(fail());
		//
		// Vaan eka virhe logataan
		//
	}

	/**
	 * @return Arraylist aggregation strategy.
	 */
	private <T> AggregationStrategy createAccumulatingAggregation() {
		return new FlexibleAggregationStrategy<T>().storeInBody()
				.accumulateInCollection(ArrayList.class);
	}

	/**
	 * @return direct:kela_siirto
	 */
	private String kelaFailed() {
		return KelaRoute.DIRECT_KELA_FAILED;
	}

}
