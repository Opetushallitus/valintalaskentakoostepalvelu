package fi.vm.sade.valinta.kooste.kela.route.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KelaFtpRouteImpl implements KelaFtpRoute {
    private static final Logger LOG = LoggerFactory.getLogger(KelaFtpRouteImpl.class);
    private DokumenttiAsyncResource dokumenttiAsyncResource;
    private String host;
    private int port;
    private String path;
    private String userName;
    private String passWord;

    @Autowired
    public KelaFtpRouteImpl (
            @Value("${kela.ftp.host}") final String host,
            @Value("${kela.ftp.port}") final String port,
            @Value("${kela.ftp.path}") final String path,
            @Value("${kela.ftp.username}") final String userName,
            @Value("${kela.ftp.password}") final String passWord,
            DokumenttiAsyncResource dokumenttiAsyncResource) {
        this.dokumenttiAsyncResource = dokumenttiAsyncResource;
        this.host = host;
        this.port = Integer.parseInt(port);
        this.path = path;
        this.userName = userName;
        this.passWord = passWord;
    }

    private ChannelSftp setupJsch() throws JSchException {
       JSch jsch = new JSch();
       Session jschSession = jsch.getSession(userName, host, port);
       jschSession.setPassword(passWord);

       java.util.Properties config = new java.util.Properties();
       config.put("StrictHostKeyChecking", "no");
       jschSession.setConfig(config);

       jschSession.connect();
       return (ChannelSftp) jschSession.openChannel("sftp");
    }

    @Override
    public Boolean aloitaKelaSiirto(String dokumenttiId) throws InterruptedException, ExecutionException, TimeoutException {
        return dokumenttiAsyncResource.lataa(dokumenttiId)
                .thenApplyAsync(response -> {
            ChannelSftp channelSftp = new ChannelSftp();
            try {
                channelSftp = setupJsch();
                channelSftp.connect();
                InputStream kelaData = response.body();
                // Parsitaan tiedostonimi, jos ei löydy, käytetään oletusnimeä:
                String headerValue = response.headers().firstValue("Content-Disposition").orElse("");
                String fileName = "";
                if (headerValue != null && !headerValue.isEmpty()) {
                    fileName = headerValue.substring(headerValue.indexOf("\"") + 1, headerValue.lastIndexOf("\""));
                    LOG.debug("Kela-ftp siirron dokumenttinimi: " + fileName);
                } else {
                    fileName  = "DEFAULT_KELA_FILENAME" + System.currentTimeMillis();
                    LOG.debug("Kela-ftp siirron dokumenttinimeä ei saatu parsittua, käytetään oletusta: " + fileName);
                }

                LOG.debug("Aloitetaan Kela-ftpsiirto dokumentille: " + fileName);
                channelSftp.put(kelaData, path + fileName);

                LOG.info("Kela-ftp siirto suoritettiin onnistuneesti. Dokumentti-id: " + dokumenttiId + ", tiedostonimi: " + fileName);
                return true;
            } catch (Exception e) {
                LOG.error("Kela-ftp siirron dokumentin haku epäonnistui dokumentille: " + dokumenttiId + " Syy: ", e);
                return false;
            } finally {
                channelSftp.disconnect();
                channelSftp.exit();
            }
        }).get(1, TimeUnit.HOURS);

    }
}
