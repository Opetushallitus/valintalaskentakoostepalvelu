package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Collections2;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
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
				.bean(new SecurityPreprocessor())
				//
				.bean(valintatietoHakukohteelleKomponentti)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						List<HakemusOsallistuminenTyyppi> unfiltered = (List<HakemusOsallistuminenTyyppi>) exchange
								.getIn().getBody();
						exchange.getOut()
								.setBody(
										Collections2
												.filter(unfiltered,
														new com.google.common.base.Predicate<HakemusOsallistuminenTyyppi>() {
															public boolean apply(
																	HakemusOsallistuminenTyyppi o) {
																for (ValintakoeOsallistuminenTyyppi o1 : o
																		.getOsallistumiset()) {
																	if (Osallistuminen.OSALLISTUU
																			.equals(o1
																					.getOsallistuminen())) {
																		return true;
																	}
																}
																return false;
															}
														}));

					}
				})
				//
				.split(body(), hakemusAggregation())
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

}
