package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_DOKUMENTTI_ID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;

@Component
public class KelaFtpRouteImpl extends SpringRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KelaFtpRouteImpl.class);

    private final String ftpKelaSiirto;
    private final String kelaSiirto;
    private DokumenttiResource dokumenttiResource;

    /**
     * @param host   esim ftp://user@host:port
     * @param params esim passiveMode=true&password=...
     */
    @Autowired
    public KelaFtpRouteImpl(
            @Value(KelaRoute.KELA_SIIRTO) String kelaSiirto,
            @Value("${kela.ftp.protocol}://${kela.ftp.username}@${kela.ftp.host}:${kela.ftp.port}${kela.ftp.path}") final String host,
            @Value("password=${kela.ftp.password}${kela.ftp.parameters}") final String params,
            DokumenttiResource dokumenttiResource) {
        this.kelaSiirto = kelaSiirto;
        this.ftpKelaSiirto = host + "?" + params;
        this.dokumenttiResource = dokumenttiResource;
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
                // Hae dokumentti
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Response response = dokumenttiResource.lataa(dokumenttiId(exchange));
                        // Koitetaan parsia tiedostonimi, jolla tallennetaan Kelalle
                        String headerValue = response.getHeaderString("Content-Disposition");
                        if (headerValue != null && !headerValue.isEmpty()) {
                            String fileName = headerValue.substring(headerValue.indexOf("\"") + 1, headerValue.lastIndexOf("\""));
                            exchange.getOut().setHeader("CamelFileName", fileName);
                            LOG.debug("Kela-ftp siirron dokumenttinimi: " + fileName);
                        }
                        exchange.getOut().setBody(response.getEntity());
                    }
                })
                        // FTP-SIIRTO
                .to(ftpKelaSiirto());
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
