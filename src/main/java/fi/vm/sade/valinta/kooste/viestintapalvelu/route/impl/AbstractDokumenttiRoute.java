package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.poi.util.IOUtils;
import org.jgroups.util.UUID;
import org.joda.time.DateTime;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Dokumenttiprosessin käsittelyyn valmiit camel dsl:t
 *         dokumenttireittejä varten
 */
public abstract class AbstractDokumenttiRoute extends SpringRouteBuilder {

	private final Processor inkrementoiKokonaistyota;
	private final Processor inkrementoiTehtyjaToita;

	public AbstractDokumenttiRoute() {
		this.inkrementoiKokonaistyota = new Processor() {
			public void process(Exchange exchange) throws Exception {
				dokumenttiprosessi(exchange).inkrementoiKokonaistyota();
			}
		};
		this.inkrementoiTehtyjaToita = new Processor() {
			public void process(Exchange exchange) throws Exception {
				dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
			}
		};
	}

	protected Processor merkkaaTyoTehdyksi() {
		return this.inkrementoiTehtyjaToita;
	}

	protected Processor inkrementoiKokonaistyota() {
		return this.inkrementoiKokonaistyota;
	}

	protected Processor asetaKokonaistyo(final int kokonaistoidenMaara) {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				dokumenttiprosessi(exchange)
						.setKokonaistyo(kokonaistoidenMaara);
			}
		};
	}

	protected DokumenttiProsessi dokumenttiprosessi(Exchange exchange) {
		return exchange.getProperty(
				ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI,
				DokumenttiProsessi.class);
	}

	protected Predicate prosessiOnKeskeytetty() {
		return new Predicate() {
			public boolean matches(Exchange exchange) {
				return dokumenttiprosessi(exchange).isKeskeytetty();
			}
		};
	}

	@SuppressWarnings("unchecked")
	protected List<String> valintakoeOids(Exchange exchange) {
		return exchange.getProperty("valintakoeOid",
				Collections.<String> emptyList(), List.class);
	}

	protected String hakukohdeOid(Exchange exchange) {
		return exchange.getProperty(OPH.HAKUKOHDEOID, String.class);
	}

	protected String hakuOid(Exchange exchange) {
		return exchange.getProperty(OPH.HAKUOID, String.class);
	}

	protected Date defaultExpirationDate() {
		return DateTime.now().plusHours(2).toDate();
	}

	protected String generateId() {
		return UUID.randomUUID().toString();
	}

	protected InputStream pipeInputStreams(InputStream incoming)
			throws IOException {
		byte[] dokumentti = IOUtils.toByteArray(incoming);
		if (dokumentti == null || dokumentti.length == 0) {
			throw new RuntimeException(
					"Viestintäpalvelu palautti tyhjän dokumentin!");
		}
		InputStream p = new ByteArrayInputStream(dokumentti);
		incoming.close();
		return p;
	}
}
