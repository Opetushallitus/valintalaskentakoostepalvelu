package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.proxy.HakemusProxy;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

import java.util.concurrent.ExecutionException;

/**
 * User: wuoti Date: 9.9.2013 Time: 13.14
 */
@Component("haeHakemusKomponentti")
public class HaeHakemusKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHakemusKomponentti.class);

//    private ApplicationResource applicationResource;
    private HakemusProxy hakemusProxy;
    private String applicationResourceUrl;

    @Autowired
    public HaeHakemusKomponentti(HakemusProxy hakemusProxy,
            @Value("${valintalaskentakoostepalvelu.hakemus.rest.url:''}") String applicationResourceUrl) {
        this.hakemusProxy = hakemusProxy;
        this.applicationResourceUrl = applicationResourceUrl;
    }

    public Hakemus haeHakemus(@Header(OPH.HAKEMUSOID) String hakemusOid) {
        LOG.info("Haetaan hakemus osoitteesta {}/applications/{}", new Object[] { applicationResourceUrl, hakemusOid });
        try {
            return hakemusProxy.haeHakemus(hakemusOid);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}
