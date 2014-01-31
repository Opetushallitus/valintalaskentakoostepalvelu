package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KoekutsukirjeRouteImpl extends SpringRouteBuilder {

	private final ViestintapalveluResource viestintapalveluResource;
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;
	private final ApplicationResource applicationResource;

	@Autowired
	public KoekutsukirjeRouteImpl(
			ViestintapalveluResource viestintapalveluResource,
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti,
			ApplicationResource applicationResource) {
		this.viestintapalveluResource = viestintapalveluResource;
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.valintatietoHakukohteelleKomponentti = valintatietoHakukohteelleKomponentti;
		this.applicationResource = applicationResource;
	}

	@Override
	public void configure() throws Exception {
		//
		from(koekutsukirjeet())
		//
				.bean(valintatietoHakukohteelleKomponentti)
				//
				.split(body(), hakemusAggregation())
				//
				// .filter(osallistuu())
				// HakemusOsallistuminenTyyppi -> Hakemus
				// Pitaisiko tehda oma convertteri yleisille kaannoksille?
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						HakemusOsallistuminenTyyppi osallistuminen = exchange
								.getIn().getBody(
										HakemusOsallistuminenTyyppi.class);
						exchange.getOut().setBody(
								applicationResource
										.getApplicationByOid(osallistuminen
												.getHakemusOid()));
					}
				})
				//
				.end()
				//
				.bean(koekutsukirjeetKomponentti)
				//
				.bean(viestintapalveluResource, "vieKoekutsukirjeet");
	}

	private String koekutsukirjeet() {
		return KoekutsukirjeRoute.DIRECT_KOEKUTSUKIRJEET;
	}

	private FlexibleAggregationStrategy<Hakemus> hakemusAggregation() {
		return new FlexibleAggregationStrategy<Hakemus>().storeInBody()
				.accumulateInCollection(ArrayList.class);
	}

	private Predicate osallistuu() {
		return new Predicate() {
			public boolean matches(Exchange exchange) {
				HakemusOsallistuminenTyyppi o = exchange.getIn().getBody(
						HakemusOsallistuminenTyyppi.class);
				for (ValintakoeOsallistuminenTyyppi o1 : o.getOsallistumiset()) {
					if (Osallistuminen.OSALLISTUU
							.equals(o1.getOsallistuminen())) {
						return true;
					}
				}
				return false;
			}
		};
	}
}
