package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_DOKUMENTTI_ID;

import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
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

import java.io.ObjectInputStream;

@Component
public class KelaFtpRouteImpl extends SpringRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KelaFtpRouteImpl.class);

    private final String ftpKelaSiirto;
    private final String kelaSiirto;
    private DokumenttiAsyncResource dokumenttiAsyncResource;

    /**
     * @param host   esim ftp://user@host:port
     * @param params esim passiveMode=true&password=...
     */
    @Autowired
    public KelaFtpRouteImpl(
            @Value(KelaRoute.KELA_SIIRTO) String kelaSiirto,
            @Value("${kela.ftp.protocol}://${kela.ftp.username}@${kela.ftp.host}:${kela.ftp.port}${kela.ftp.path}") final String host,
            @Value("password=${kela.ftp.password}${kela.ftp.parameters}") final String params,
            DokumenttiAsyncResource dokumenttiAsyncResource) {
        this.kelaSiirto = kelaSiirto;
        this.ftpKelaSiirto = host + "?" + params;
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
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
                        dokumenttiAsyncResource.lataa(dokumenttiId(exchange)).whenComplete(
                                (response, throwable) -> {
                                    // Koitetaan parsia tiedostonimi, jolla tallennetaan Kelalle
                                    String headerValue = response.headers().firstValue("Content-Disposition").get();
                                    if (headerValue != null && !headerValue.isEmpty()) {
                                        String fileName = headerValue.substring(headerValue.indexOf("\"") + 1, headerValue.lastIndexOf("\""));
                                        exchange.getOut().setHeader("CamelFileName", fileName);
                                        LOG.debug("Kela-ftp siirron dokumenttinimi: " + fileName);
                                    }
                                    try {
                                        ObjectInputStream objectInputStream = new ObjectInputStream(response.body());
                                        exchange.getOut().setBody(objectInputStream.readObject());
                                    } catch (Exception e) {
                                        LOG.error("Kela-ftp siirron dokumentin haku epäonnistui " + dokumenttiId(exchange));
                                    }

                                }
                        );
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
