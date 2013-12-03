package fi.vm.sade.valinta.kooste.kela.route.impl;

import static fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService.MESSAGE;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.finish;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.kuvaus;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.prosessi;
import static fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.start;

import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private static final Logger LOG = LoggerFactory.getLogger(KelaFtpRouteImpl.class);

    @Autowired
    private SendMessageToDocumentService messageService;

    private final String ftpKelaSiirto;
    private PrepareKelaProcessDescription luoUusiProsessi;

    /**
     * @param host
     *            esim ftp://user@host:port
     * @param params
     *            esim passiveMode=true&password=...
     */
    @Autowired
    public KelaFtpRouteImpl(
            @Value("${kela.ftp.protocol}://${kela.ftp.username}@${kela.ftp.host}:${kela.ftp.port}${kela.ftp.path}") final String host,
            @Value("password=${kela.ftp.password}&ftpClient.dataTimeout=30000&passiveMode=true") final String params) {
        StringBuilder builder = new StringBuilder();
        builder.append(host).append("?").append(params);
        this.ftpKelaSiirto = builder.toString();
        this.luoUusiProsessi = new PrepareKelaProcessDescription();
    }

    @Override
    public void configure() throws Exception {
        /**
         * Kela-dokkarin siirto ftp:lla Kelalle
         */
        from(kelaSiirto())
        // prosessin kuvaus
                .setHeader(kuvaus(), constant("Kela-siirto")).setProperty(prosessi(), method(luoUusiProsessi))
                // Start prosessi valvomoon dokumentin luonnin aloittamisesta
                .wireTap(start()).end()
                // Kayttajalle ilmoitus
                .setHeader(MESSAGE, constant("Kela-dokumentin siirto aloitettu.")).bean(messageService)
                // FTP-SIIRTO
                .log("SIIRTO")
                // .to(ftpKelaSiirto())
                // Done valvomoon
                .wireTap(finish()).end();
    }

    /**
     * @return ftps://...
     */
    private String ftpKelaSiirto() {
        return ftpKelaSiirto;
    }

    /**
     * @return direct:kela_siirto
     */
    private String kelaSiirto() {
        return KelaRoute.DIRECT_KELA_SIIRTO;
    }
}
