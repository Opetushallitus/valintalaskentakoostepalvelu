package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratHakemuksilleRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratSijoittelussaHyvaksytyilleRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Hyvaksyttyjen Osoitetarrat ja osoitetarrat koekutsua varten
 */
@Component
public class OsoitetarratRouteImpl extends SpringRouteBuilder {

	@Autowired
	private ViestintapalveluResource viestintapalveluResource;

	@Autowired
	private ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;

	@Autowired
	private HaeOsoiteKomponentti osoiteKomponentti;

	@Autowired
	private SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;

	private final SecurityPreprocessor security = new SecurityPreprocessor();

	public static class LuoOsoitteet {
		public Osoitteet luo(List<Osoite> osoitteet) {
			if (osoitteet == null || osoitteet.isEmpty()) {
				throw new ViestintapalveluException(
						"Yritetään luoda nolla kappaletta osoitetarroja!");
			}
			return new Osoitteet(osoitteet);
		}
	}

	@Override
	public void configure() throws Exception {
		configureHyvaksyttyjenOsoitetarrat();
		configureValintakokeenOsoitetarrat();
		configureHakemuksenOsoitetarrat();
	}

	private void configureHyvaksyttyjenOsoitetarrat() throws Exception {
		from(hyvaksyttyjenOsoitetarrat())
		//
				.choice()
				// Jos luodaan vain yksittaiselle hakemukselle...
				.when(property("hakemusOids").isNotNull())
				//
				.setBody(property("hakemusOids"))
				//
				.otherwise()
				//
				.bean(sijoitteluProxy)
				//
				.process(new Processor() {
					@SuppressWarnings("unchecked")
					@Override
					public void process(Exchange exchange) throws Exception {
						List<String> l = Lists.newArrayList();
						for (HakijaDTO hakija : (List<HakijaDTO>) exchange
								.getIn().getBody(List.class)) {
							l.add(hakija.getHakemusOid());
						}
						exchange.getOut().setBody(l);
					}
				})
				//
				.end()
				//
				.split(body(), osoiteAggregation())
				//
				.bean(osoiteKomponentti)
				//
				.end()
				// enrich to Osoitteet
				.bean(new LuoOsoitteet())
				//
				.bean(viestintapalveluResource, "haeOsoitetarrat");

	}

	private void configureHakemuksenOsoitetarrat() throws Exception {
		from(hakemustenOsoitetarrat())
		//
				.process(security)
				//
				.bean(osoiteKomponentti)
				//
				.end()
				// enrich to Osoitteet
				.bean(new LuoOsoitteet())
				//
				.bean(viestintapalveluResource, "haeOsoitetarrat");

	}

	private void configureValintakokeenOsoitetarrat() throws Exception {
		from(valintakokeenOsoitetarrat())
		//
				.choice()
				// Jos luodaan vain yksittaiselle hakemukselle...
				.when(property("hakemusOids").isNotNull())
				//
				.setBody(property("hakemusOids"))
				//
				.otherwise()
				//
				.bean(valintatietoHakukohteelleKomponentti)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						@SuppressWarnings("unchecked")
						List<HakemusOsallistuminenTyyppi> h = exchange.getIn()
								.getBody(List.class);
						List<String> hakemusOids = Lists.newArrayList();
						for (HakemusOsallistuminenTyyppi h0 : h) {
							for (ValintakoeOsallistuminenTyyppi o0 : h0
									.getOsallistumiset()) {
								if (Osallistuminen.OSALLISTUU.equals(o0
										.getOsallistuminen())) {
									// add
									hakemusOids.add(h0.getHakemusOid());
								}
							}
						}
						exchange.getOut().setBody(hakemusOids);
					}
				})
				//
				.end()
				//
				.split(body(), osoiteAggregation())
				//
				.bean(osoiteKomponentti)
				//
				.end()
				// enrich to Osoitteet
				.bean(new LuoOsoitteet())
				//
				.bean(viestintapalveluResource, "haeOsoitetarrat");

	}

	private FlexibleAggregationStrategy<Osoite> osoiteAggregation() {
		return new FlexibleAggregationStrategy<Osoite>().storeInBody()
				.accumulateInCollection(ArrayList.class);
	}

	private String hakemustenOsoitetarrat() {
		return OsoitetarratHakemuksilleRoute.DIRECT_OSOITETARRAT_HAKEMUKSILLE;
	}

	private String valintakokeenOsoitetarrat() {
		return OsoitetarratRoute.DIRECT_OSOITETARRAT;
	}

	private String hyvaksyttyjenOsoitetarrat() {
		return OsoitetarratSijoittelussaHyvaksytyilleRoute.DIRECT_HYVAKSYTTYJEN_OSOITETARRAT;
	}

}
