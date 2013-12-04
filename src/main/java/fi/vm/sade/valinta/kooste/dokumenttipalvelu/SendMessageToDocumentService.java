package fi.vm.sade.valinta.kooste.dokumenttipalvelu;

import java.util.List;

import org.apache.camel.Header;
import org.apache.camel.Property;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;

@Component
public class SendMessageToDocumentService {

    public final static String MESSAGE = "message";
    private static final Logger LOG = LoggerFactory.getLogger(SendMessageToDocumentService.class);

    @Autowired
    private DokumenttiResource dokumenttiResource;

    public void start(@Header(MESSAGE) String message, @Property(OPH.HAKUOID) String hakuOid) {
        List<String> tags = Lists.newArrayList();
        tags.add(hakuOid);
        tags.add("kela");
        tags.add("valintalaskentakoostepalvelu");
        try {
            dokumenttiResource.viesti(new Message(message, tags, DateTime.now().plusDays(1).toDate()));
        } catch (Exception e) {
            LOG.error("Viestin ilmoittaminen käyttäjälle dokumenttipalvelun välityksellä epäonnistui! Viesti: {}",
                    message);
        }
    }
}
