package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import static org.apache.camel.builder.PredicateBuilder.not;

import java.util.Collection;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;

import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHaunHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.valintakokeet.route.ValintakoelaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintakoelaskentaMuistissaRouteImpl extends
		AbstractDokumenttiRoute {

	private final String valintakoelaskenta;
	private final HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti;

	public ValintakoelaskentaMuistissaRouteImpl(
			@Value(ValintakoelaskentaMuistissaRoute.SEDA_VALINTAKOELASKENTA_MUISTISSA) String valintakoelaskenta,
			HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti) {
		this.valintakoelaskenta = valintakoelaskenta;
		this.haeHaunHakemuksetKomponentti = haeHaunHakemuksetKomponentti;
	}

	@Override
	public void configure() throws Exception {
		//
		// Valintakoelaskenta muistissa reitti
		//
		from(valintakoelaskenta)
		//
				.doTry()
				//
				// Collection<String> hakemusOids
				//
				.to("direct:valintakoelaskentamuistissa_haun_hakemukset")
				//
				.doCatch(Exception.class)
				//
				.process(
						kirjaaPoikkeus(new Poikkeus(Poikkeus.HAKUOID,
								"Tarjontannalta hakemukset",
								"Ei saatu hakemuksia haulle tarjonnasta!")))
				//
				.end()
				//
				.choice()
				//
				.when(not(isEmpty(body())))
				//
				.process(new Processor() {

					public void process(Exchange exchange) throws Exception {
						Collection<String> hakemusOids = exchange.getIn()
								.getBody(Collection.class);

					}

				})
				//
				.otherwise()
				//
				.log("Valintakoelaskentaa ei voida suorittaa haulle jossa ei ole hakemuksia!")
				//
				.end();

		//
		// Tarjonnasta Haun Hakemukset vaihtoehtoisesti jos "hakemusOids"
		// property on määritelty niin käytetään sitä
		//
		from("direct:valintakoelaskentamuistissa_haun_hakemukset")
		//
				.onException(Exception.class)
				//
				.maximumRedeliveries(2).redeliveryDelay(1500L)
				//
				.choice()
				//
				.when(isEmpty(property("hakemusOids")))
				//
				.bean(haeHaunHakemuksetKomponentti)
				//
				.bean(new HakemusOidSplitter())
				//
				.otherwise()
				//
				.setBody(property("hakemusOids"))
				//
				.end();

	}

}
