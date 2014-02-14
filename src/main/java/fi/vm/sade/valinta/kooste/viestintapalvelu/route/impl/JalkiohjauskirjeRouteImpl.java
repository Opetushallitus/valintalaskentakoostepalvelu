package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;

@Component
public class JalkiohjauskirjeRouteImpl extends SpringRouteBuilder {

	@Autowired
	private ViestintapalveluResource viestintapalveluResource;

	@Autowired
	private JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;

	@Autowired
	private SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluProxy;

	@Autowired
	private SijoitteluResource sijoitteluResource;

	@Override
	public void configure() throws Exception {
		from(jalkiohjauskirjeet())
		//
				.bean(new SecurityPreprocessor())
				//
				.choice().when(property("hakemusOidit").isNotNull())
				//
				// Yksittaisille hakemuksille jalkiohjauskirje
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						final String hakuOid = exchange.getProperty(
								OPH.HAKUOID, String.class);
						@SuppressWarnings("unchecked")
						List<String> hakemusOidit = exchange.getProperty(
								"hakemusOidit", List.class);
						final List<HakijaDTO> hyvaksymattomatHakijat = Lists
								.newArrayList();
						for (String hakemusOid : hakemusOidit) {
							hyvaksymattomatHakijat.add(sijoitteluResource
									.hakemus(hakuOid,
											SijoitteluResource.LATEST,
											hakemusOid));
						}
						exchange.getOut().setBody(hyvaksymattomatHakijat);
					}
				})
				//
				.otherwise()
				//
				// Kaikille sijoittelun antamille valitsemattomille
				// jalkiohjauskirje
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						final List<HakijaDTO> hyvaksymattomatHakijat = sijoitteluProxy
								.ilmankoulutuspaikkaa(exchange.getProperty(
										OPH.HAKUOID, String.class),
										SijoitteluResource.LATEST);
						exchange.getOut().setBody(hyvaksymattomatHakijat);
					}

				})
				//
				.end()
				//
				// TODO: Hae osoitteet erikseen
				// TODO: Cache ulkopuolisiin palvelukutsuihin
				.bean(jalkiohjauskirjeetKomponentti)
				//
				.bean(viestintapalveluResource, "haeJalkiohjauskirjeet");
	}

	private String jalkiohjauskirjeet() {
		return JalkiohjauskirjeRoute.DIRECT_JALKIOHJAUSKIRJEET;
	}
}
