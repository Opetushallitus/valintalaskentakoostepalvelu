package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.InputStream;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;

@Component
public class JalkiohjauskirjeRouteImpl extends AbstractDokumenttiRoute {
	private final static Logger LOG = LoggerFactory
			.getLogger(JalkiohjauskirjeRouteImpl.class);
	private final ViestintapalveluResource viestintapalveluResource;
	private final JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;
	private final SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluProxy;
	private final DokumenttiResource dokumenttiResource;
	private final SijoitteluResource sijoitteluResource;
	private final String jalkiohjauskirjeet;

	@Autowired
	public JalkiohjauskirjeRouteImpl(
			@Value(JalkiohjauskirjeRoute.SEDA_JALKIOHJAUSKIRJEET) String jalkiohjauskirjeet,
			ViestintapalveluResource viestintapalveluResource,
			JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti,
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource,
			SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluProxy,
			SijoitteluResource sijoitteluResource) {
		super();
		this.dokumenttiResource = dokumenttiResource;
		this.viestintapalveluResource = viestintapalveluResource;
		this.jalkiohjauskirjeetKomponentti = jalkiohjauskirjeetKomponentti;
		this.sijoitteluProxy = sijoitteluProxy;
		this.sijoitteluResource = sijoitteluResource;
		this.jalkiohjauskirjeet = jalkiohjauskirjeet;
	}

	@Override
	public void configure() throws Exception {
		from(jalkiohjauskirjeet)
				//
				.bean(new SecurityPreprocessor())
				//
				.choice()
				//
				.when(prosessiOnKeskeytetty())
				//
				.log(LoggingLevel.WARN,
						"Ohitetaan prosessi ${property.property_valvomo_prosessi} koska se on merkitty keskeytetyksi!")
				//
				.otherwise()
				//
				.to("direct:jalkiohjauskirjeet_jatketaan")
				//
				.end();

		from("direct:jalkiohjauskirjeet_jatketaan")
		//
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
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						List<HakijaDTO> hyvaksymattomatHakijat = exchange
								.getIn().getBody(List.class);
						dokumenttiprosessi(exchange).setKokonaistyo(3);
						try {
							exchange.getOut()
									.setBody(
											jalkiohjauskirjeetKomponentti
													.teeJalkiohjauskirjeet(hyvaksymattomatHakijat));
						} catch (Exception e) {
							LOG.error(
									"Jälkiohjauskirjeitä ei saatu muodostettua: {}\r\n{}",
									e.getMessage(), e.getCause());
							e.printStackTrace();
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.HAKU,
											"Hakemusten haku",
											"hakemusten haku epäonnistui!"));
							throw e;
						}
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
					}
				})
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						DokumenttiProsessi prosessi = dokumenttiprosessi(exchange);
						InputStream pdf;
						try {

							// LOG.error(
							// "\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create()
							// .toJson(koekutsukirjeet(exchange)));
							pdf = pipeInputStreams(viestintapalveluResource
									.haeJalkiohjauskirjeetSync(jalkiohjauskirjeet(exchange)));
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();

						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Viestintäpalvelulta pdf:n haussa tapahtui virhe: {}",
									e.getMessage());
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VIESTINTAPALVELU,
											"Koekutsukirjeiden synkroninen haku",
											e.getMessage()));
							throw e;
						}
						try {
							String id = generateId();
							dokumenttiResource.tallenna(id,
									"jalkiohjauskirjeet.zip",
									defaultExpirationDate().getTime(),
									prosessi.getTags(), "application/zip", pdf);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
							prosessi.setDokumenttiId(id);
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Dokumenttipalvelulle tiedonsiirrossa tapahtui virhe: {}",
									e.getMessage());
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.DOKUMENTTIPALVELU,
											"Dokumentin tallennus", e
													.getMessage()));
							throw e;
						}
					}
				});
	}

	@SuppressWarnings("unchecked")
	private Kirjeet<Kirje> jalkiohjauskirjeet(Exchange exchange) {
		return exchange.getIn().getBody(Kirjeet.class);
	}
}
