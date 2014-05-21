package fi.vm.sade.valinta.kooste.valintalaskentatulos.route.impl;

import java.io.InputStream;
import java.util.Arrays;

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
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
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
	private JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti;
	private SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti;
	private ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti;
	private ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti haeHakukohdeNimiTarjonnaltaKomponentti;
	private final SecurityPreprocessor security = new SecurityPreprocessor();
	private final DokumenttiResource dokumenttiResource;
	private final String valintakoekutsutXls;

	@Autowired
	public ValintalaskentaTulosRouteImpl(
			JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti,
			SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti,
			ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti,
			ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti,
			HaeHakukohdeNimiTarjonnaltaKomponentti haeHakukohdeNimiTarjonnaltaKomponentti,
			DokumenttiResource dokumenttiResource,
			@Value(ValintakoekutsutExcelRoute.SEDA_VALINTAKOE_EXCEL) String valintakoekutsutXls) {
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
				try {
					HakukohdeNimiRDTO dto = haeHakukohdeNimiTarjonnaltaKomponentti
							.haeHakukohdeNimi(hakukohdeOid(exchange));
					hakukohteenNimi = new fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti(
							dto.getHakukohdeNimi()).getTeksti();
					haunNimi = new fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti(
							dto.getTarjoajaNimi()).getTeksti();
				} catch (Exception e) {

				}
				exchange.getOut().setHeader("hakukohteenNimi", hakukohteenNimi);
				exchange.getOut().setHeader("haunNimi", haunNimi);
			}
		};
	}

	@Override
	public void configure() throws Exception {
		from(JalkiohjaustulosExcelRoute.DIRECT_JALKIOHJAUS_EXCEL).bean(
				jalkiohjaustulosExcelKomponentti);
		from(SijoittelunTulosExcelRoute.DIRECT_SIJOITTELU_EXCEL).bean(
				sijoittelunTulosExcelKomponentti);

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
				.process(security)
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
	}
}
