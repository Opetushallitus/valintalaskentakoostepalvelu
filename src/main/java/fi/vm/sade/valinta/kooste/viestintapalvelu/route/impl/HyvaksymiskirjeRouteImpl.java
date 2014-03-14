package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;

@Component
public class HyvaksymiskirjeRouteImpl extends AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(HyvaksymiskirjeRouteImpl.class);
	private final ViestintapalveluResource viestintapalveluResource;
	private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
	private final String hyvaksymiskirjeet;
	private final SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;
	private final DokumenttiResource dokumenttiResource;
	private final SecurityPreprocessor security = new SecurityPreprocessor();

	@Autowired
	public HyvaksymiskirjeRouteImpl(
			@Value(HyvaksymiskirjeRoute.SEDA_HYVAKSYMISKIRJEET) String hyvaksymiskirjeet,
			ViestintapalveluResource viestintapalveluResource,
			HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
			SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy,
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource) {
		super();
		this.dokumenttiResource = dokumenttiResource;
		this.sijoitteluProxy = sijoitteluProxy;
		this.hyvaksymiskirjeet = hyvaksymiskirjeet;
		this.viestintapalveluResource = viestintapalveluResource;
		this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
	}

	@Override
	public void configure() throws Exception {
		from(hyvaksymiskirjeet)
				// TODO: Hae osoitteet erikseen
				.process(security)
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
				.to("direct:hyvaksymiskirjeet_jatketaan")
				//
				.end();

		from("direct:hyvaksymiskirjeet_jatketaan")
		// TODO: Cache ulkopuolisiin palvelukutsuihin
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						List<String> hakemusOids = hakemusOids(exchange);
						if (hakemusOids == null) {
							hakemusOids = Collections.<String> emptyList();
						}
						//
						//
						//
						Collection<HakijaDTO> hakukohteenHakijat;
						try {
							hakukohteenHakijat = sijoitteluProxy
									.koulutuspaikalliset(hakuOid(exchange),
											hakukohdeOid(exchange),
											SijoitteluResource.LATEST);
							final Set<String> whitelist = Sets
									.newHashSet(hakemusOids);
							if (!whitelist.isEmpty()) {
								hakukohteenHakijat = Collections2.filter(
										hakukohteenHakijat,
										new Predicate<HakijaDTO>() {
											public boolean apply(HakijaDTO input) {
												return whitelist.contains(input
														.getHakemusOid());
											}
										});
							}
							exchange.getOut().setBody(hakukohteenHakijat);
							dokumenttiprosessi(exchange).setKokonaistyo(2);
						} catch (Exception e) {
							e.printStackTrace();
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.hakuOid(hakuOid(exchange)), Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Sijoittelusta ei saatu haettua hyväksyttyjä hakijoita",
											e.getMessage(), oidit));
							throw new RuntimeException(
									"Sijoittelusta hyväksyttyjen hakijoiden haku epäonnistui",
									e);
						}
						if (hakukohteenHakijat.isEmpty()) {
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.hakuOid(hakuOid(exchange)), Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Sijoittelussa ei ollut yhtään hyväksyttyä hakijaa!",
											"", oidit));
							throw new RuntimeException(
									"Sijoittelussa ei ollut yhtään hyväksyttyä hakijaa!");
						}
					}

				})
				//
				.bean(hyvaksymiskirjeetKomponentti)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						DokumenttiProsessi prosessi = dokumenttiprosessi(exchange);
						Kirjeet<Kirje> kirjeet = exchange.getIn().getBody(
								Kirjeet.class);

						InputStream pdf;
						try {

							// LOG.error("\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create().toJson(osoitteet));
							pdf = pipeInputStreams(viestintapalveluResource
									.haeHyvaksymiskirjeetSync(kirjeet));

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
											"Osoitteet pdf:n synkroninen haku viestintäpalvelulta",
											e.getMessage()));
							throw e;
						}
						String id = generateId();
						Long expirationTime = defaultExpirationDate().getTime();
						List<String> tags = prosessi.getTags();
						if (id == null || expirationTime == null
								|| tags == null || pdf == null) {
							String tila = new StringBuilder().append(id)
									.append(expirationTime).append(" tags=")
									.append(tags == null).append(" pdf=")
									.append(pdf == null).toString();
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Dokumenttipalvelun kutsumisen esiehdot ei täyty!",
											tila));
							throw new RuntimeException(
									"Dokumenttipalvelun kutsumisen esiehdot ei täyty!"
											+ tila);
						}
						try {

							dokumenttiResource.tallenna(id,
									"hyvaksymiskirjeet.pdf", expirationTime,
									tags, "application/pdf", pdf);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
							prosessi.setDokumenttiId(id);
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Dokumenttipalvelulle tiedonsiirrossa tapahtui virhe: {}",
									e.getMessage());
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Osoitteet pdf:n tallennus dokumenttipalvelulle",
											e.getMessage()));
							throw e;
						}
					}
				});
		// .bean(viestintapalveluResource, "haeHyvaksymiskirjeet");
	}

}
