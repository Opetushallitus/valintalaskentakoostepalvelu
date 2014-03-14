package fi.vm.sade.valinta.kooste.valintalaskentatulos.route.impl;

import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
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

	private JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti;
	private SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti;
	private ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti;
	private ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti;
	private final DokumenttiResource dokumenttiResource;
	private final String valintakoekutsutXls;

	@Autowired
	public ValintalaskentaTulosRouteImpl(
			JalkiohjaustulosExcelKomponentti jalkiohjaustulosExcelKomponentti,
			SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti,
			ValintalaskennanTulosExcelKomponentti valintalaskennanTulosExcelKomponentti,
			ValintalaskentaTulosExcelKomponentti valintalaskentaTulosExcelKomponentti,
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource,
			@Value(ValintakoekutsutExcelRoute.SEDA_VALINTAKOE_EXCEL) String valintakoekutsutXls) {
		this.jalkiohjaustulosExcelKomponentti = jalkiohjaustulosExcelKomponentti;
		this.sijoittelunTulosExcelKomponentti = sijoittelunTulosExcelKomponentti;
		this.valintalaskennanTulosExcelKomponentti = valintalaskennanTulosExcelKomponentti;
		this.valintalaskentaTulosExcelKomponentti = valintalaskentaTulosExcelKomponentti;
		this.dokumenttiResource = dokumenttiResource;
		this.valintakoekutsutXls = valintakoekutsutXls;
	}

	@Override
	public void configure() throws Exception {
		from(JalkiohjaustulosExcelRoute.DIRECT_JALKIOHJAUS_EXCEL).bean(
				jalkiohjaustulosExcelKomponentti);
		from(SijoittelunTulosExcelRoute.DIRECT_SIJOITTELU_EXCEL).bean(
				sijoittelunTulosExcelKomponentti);

		from(ValintalaskentaTulosExcelRoute.DIRECT_VALINTALASKENTA_EXCEL).bean(
				valintalaskennanTulosExcelKomponentti);

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
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).setKokonaistyo(1);
						InputStream i = pipeInputStreams(valintalaskentaTulosExcelKomponentti
								.luoTuloksetXlsMuodossa(hakukohdeOid(exchange),
										valintakoeOids(exchange),
										hakemusOids(exchange)));
						String id = generateId();
						dokumenttiResource.tallenna(id, "valintakoekutsut.xls",
								defaultExpirationDate().getTime(),
								dokumenttiprosessi(exchange).getTags(),
								"application/vnd.ms-excel", i);
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
						dokumenttiprosessi(exchange).setDokumenttiId(id);
					}
				})
				//
				.end();
		from("direct:valintakoekutsut_xls_deadletterchannel")
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.VALINTALASKENTA,
										"Valintatiedotpalvelukutsu", exchange
												.getException().getMessage()));
					}
				});
	}

}
