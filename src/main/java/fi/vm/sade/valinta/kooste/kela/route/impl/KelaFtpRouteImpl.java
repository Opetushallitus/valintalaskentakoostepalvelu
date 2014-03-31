package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_DOKUMENTTI_ID;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.finish;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.start;

import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Property;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.PrepareKelaProcessDescription;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KelaFtpRouteImpl extends SpringRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(KelaFtpRouteImpl.class);

	@Autowired
	private SendMessageToDocumentService messageService;

	private final String ftpKelaSiirto;
	private PrepareKelaProcessDescription luoUusiProsessi;
	private final String kelaSiirto;
	private DokumenttiResource dokumenttiResource;

	/**
	 * @param host
	 *            esim ftp://user@host:port
	 * @param params
	 *            esim passiveMode=true&password=...
	 */
	@Autowired
	public KelaFtpRouteImpl(
			@Value(KelaRoute.KELA_SIIRTO) String kelaSiirto,
			@Value("${kela.ftp.protocol}://${kela.ftp.username}@${kela.ftp.host}:${kela.ftp.port}${kela.ftp.path}") final String host,
			@Value("password=${kela.ftp.password}&ftpClient.dataTimeout=30000&passiveMode=true") final String params,
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource) {
		StringBuilder builder = new StringBuilder();
		builder.append(host).append("?").append(params);
		this.kelaSiirto = kelaSiirto;
		this.ftpKelaSiirto = builder.toString();
		this.luoUusiProsessi = new PrepareKelaProcessDescription();
		this.dokumenttiResource = dokumenttiResource;
	}

	public class DownloadDocumentWithDocumentId {

		public InputStream download(
				@Property(PROPERTY_DOKUMENTTI_ID) String documentId) {
			return (InputStream) dokumenttiResource.lataa(documentId)
					.getEntity();
		}
	}

	private String dokumenttiId(Exchange exchange) {
		return exchange.getProperty(PROPERTY_DOKUMENTTI_ID, String.class);
	}

	@Override
	public void configure() throws Exception {
		/**
		 * Kela-dokkarin siirto ftp:lla Kelalle
		 */
		from(kelaSiirto)
				// prosessin kuvaus
				.setProperty(kuvaus(), constant("Kela-siirto"))
				.setProperty(prosessi(), method(luoUusiProsessi))
				// Start prosessi valvomoon dokumentin luonnin aloittamisesta
				.to(start())
				// Hae dokumentti
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						exchange.getOut().setBody(
								dokumenttiResource
										.lataa(dokumenttiId(exchange))
										.getEntity());
					}
				})
				// FTP-SIIRTO
				.to(ftpKelaSiirto())
				// Done valvomoon
				.to(finish());
	}

	/**
	 * @return ftps://...
	 */
	private String ftpKelaSiirto() {
		return ftpKelaSiirto;
	}

	public String getFtpKelaSiirto() {
		return ftpKelaSiirto;
	}
}
