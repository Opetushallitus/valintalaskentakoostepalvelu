package fi.vm.sade.valinta.kooste.valintalaskentatulos.route.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.JalkiohjaustulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintalaskennanTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintalaskentaTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.JalkiohjaustulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.SijoittelunTulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.ValintakoekutsutExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.ValintalaskentaTulosExcelRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintalaskentaTulosRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaTulosRouteImpl.class);
	private final static String VAKIO_HAKUKOHTEEN_NIMI = "Hakukohteelle ei saatu haettua nimeä";
	private final static String VAKIO_HAUN_NIMI = "Haulle ei saatu haettua nimeä";
	private final static String HAKUKOHTEEN_NIMI = "hakukohteenNimi";
	private final static String PREFEROITUKIELIKOODI = "preferoitukielikoodi";
	private final static String TARJOAJA_NIMI = "haunNimi";
	private JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti;
	private SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti;
	private ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti;
	private ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti haeHakukohdeNimiTarjonnaltaKomponentti;
	private final DokumenttiResource dokumenttiResource;
	private final String valintakoekutsutXls;
	private final TilaResource tilaResource;

	@Autowired
	public ValintalaskentaTulosRouteImpl(
			TilaResource tilaResource,
			JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti,
			SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti,
			ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti,
			ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti,
			HaeHakukohdeNimiTarjonnaltaKomponentti haeHakukohdeNimiTarjonnaltaKomponentti,
			DokumenttiResource dokumenttiResource,
			@Value(ValintakoekutsutExcelRoute.SEDA_VALINTAKOE_EXCEL) String valintakoekutsutXls) {
		this.tilaResource = tilaResource;
		this.jalkiohjaustulosExcelKomponentti = jalkiohjaustulosExcelKomponentti;
		this.sijoittelunTulosExcelKomponentti = sijoittelunTulosExcelKomponentti;
		this.valintalaskennanTulosExcelKomponentti = valintalaskennanTulosExcelKomponentti;
		this.valintalaskentaTulosExcelKomponentti = valintalaskentaTulosExcelKomponentti;
		this.haeHakukohdeNimiTarjonnaltaKomponentti = haeHakukohdeNimiTarjonnaltaKomponentti;
		this.dokumenttiResource = dokumenttiResource;
		this.valintakoekutsutXls = valintakoekutsutXls;
	}

	private Processor haunJaHakukohteenNimet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				String hakukohteenNimi = VAKIO_HAKUKOHTEEN_NIMI;
				String haunNimi = VAKIO_HAUN_NIMI;
				String preferoitukielikoodi = KieliUtil.SUOMI;
				try {
					HakukohdeNimiRDTO dto = haeHakukohdeNimiTarjonnaltaKomponentti
							.haeHakukohdeNimi(hakukohdeOid(exchange));
					fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti hakukohdeTeksti = new fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti(
							dto.getHakukohdeNimi());
					hakukohteenNimi = hakukohdeTeksti.getTeksti();
					preferoitukielikoodi = hakukohdeTeksti.getKieli();
					haunNimi = new fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti(
							dto.getTarjoajaNimi()).getTeksti();

				} catch (Exception e) {

				}
				exchange.getOut().setHeader(HAKUKOHTEEN_NIMI, hakukohteenNimi);
				exchange.getOut().setHeader(TARJOAJA_NIMI, haunNimi);
				exchange.getOut().setHeader(PREFEROITUKIELIKOODI,
						preferoitukielikoodi);
			}
		};
	}

	@Override
	public void configure() throws Exception {
		from(JalkiohjaustulosExcelRoute.DIRECT_JALKIOHJAUS_EXCEL).bean(
				jalkiohjaustulosExcelKomponentti);
		from(SijoittelunTulosExcelRoute.SEDA_SIJOITTELU_EXCEL)
		//
				.errorHandler(
				//
						deadLetterChannel(
								"direct:sijoitteluntulokset_xls_deadletterchannel")
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.routeId("Sijoitteluntulosten taulukkolaskentatyöjono")
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(asetaKokonaistyo(1))
				//
				.process(haunJaHakukohteenNimet())
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String hakukohdeOid = hakukohdeOid(exchange);
						String hakuOid = hakuOid(exchange);
						String sijoitteluajoId = sijoitteluajoId(exchange);
						List<Valintatulos> tilat = Collections.emptyList();
						try {
							tilat = tilaResource.hakukohteelle(hakukohdeOid);
						} catch (Exception e) {

						}
						try {

							InputStream xls = sijoittelunTulosExcelKomponentti
									.luoXls(tilat,
											sijoitteluajoId,
											exchange.getIn().getHeader(
													PREFEROITUKIELIKOODI,
													String.class),
											exchange.getIn().getHeader(
													HAKUKOHTEEN_NIMI,
													String.class),
											exchange.getIn()
													.getHeader(TARJOAJA_NIMI,
															String.class),
											hakukohdeOid, hakuOid);
							String id = generateId();
							dokumenttiResource.tallenna(id, "sijoitteluntulos_"
									+ hakukohdeOid + ".xls",
									defaultExpirationDate().getTime(),
									dokumenttiprosessi(exchange).getTags(),
									"application/vnd.ms-excel", xls);

							dokumenttiprosessi(exchange).setDokumenttiId(id);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error("{} {}", e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.DOKUMENTTIPALVELU,
											"Dokumenttipalvelulle tallennus", e
													.getMessage()));
							throw e;
						}
					}
				})
				//
				.stop();

		from(ValintalaskentaTulosExcelRoute.DIRECT_VALINTALASKENTA_EXCEL)
		//
				.process(haunJaHakukohteenNimet())
				//
				.bean(valintalaskennanTulosExcelKomponentti);

		//
		//
		//
		from(valintakoekutsutXls)
		//
				.errorHandler(
				//
						deadLetterChannel(
								"direct:valintakoekutsut_xls_deadletterchannel")
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(haunJaHakukohteenNimet())
				//
				.bean(valintalaskentaTulosExcelKomponentti)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						try {
							dokumenttiprosessi(exchange).setKokonaistyo(1);
							InputStream xls = exchange.getIn().getBody(
									InputStream.class);
							// xls = valintalaskentaTulosExcelKomponentti
							// .luoTuloksetXlsMuodossa(hakukohdeOid,
							// valintakoeOids, hakemusOids);
							String id = generateId();
							try {
								dokumenttiResource.tallenna(id,
										"valintakoekutsut.xls",
										defaultExpirationDate().getTime(),
										dokumenttiprosessi(exchange).getTags(),
										"application/vnd.ms-excel", xls);
							} catch (Exception e) {
								LOG.error("{} {}", e.getMessage(),
										Arrays.toString(e.getStackTrace()));
								dokumenttiprosessi(exchange)
										.getPoikkeukset()
										.add(new Poikkeus(
												Poikkeus.DOKUMENTTIPALVELU,
												"Dokumenttipalvelulle tallennus",
												e.getMessage()));
								throw e;
							}
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
							dokumenttiprosessi(exchange).setDokumenttiId(id);
						} catch (Exception e) {
							LOG.error("{} {}", e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							throw e;
						}
					}
				})
				//
				.end();
		from("direct:valintakoekutsut_xls_deadletterchannel")
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String message = null;
						if (null != exchange.getException()) {
							message = exchange.getException().getMessage();
						}
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.VALINTALASKENTA,
										"Valintatiedotpalvelukutsu", message));
					}
				});
		from("direct:sijoitteluntulokset_xls_deadletterchannel")
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String message = null;
						if (null != exchange.getException()) {
							message = exchange.getException().getMessage();
						}
						dokumenttiprosessi(exchange)
								.getPoikkeukset()
								.add(new Poikkeus(
										Poikkeus.VALINTALASKENTA,
										"Sijoittelun tulosten muodostaminen epäonnistui!",
										message));
					}
				});
	}
}
