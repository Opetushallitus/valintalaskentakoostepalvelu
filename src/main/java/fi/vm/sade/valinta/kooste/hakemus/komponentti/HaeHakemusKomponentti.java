package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * User: wuoti Date: 9.9.2013 Time: 13.14
 */
@Component("haeHakemusKomponentti")
public class HaeHakemusKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHakemusKomponentti.class);

    private ApplicationResource applicationResource;
    private String applicationResourceUrl;

    @Autowired
    public HaeHakemusKomponentti(ApplicationResource applicationResource,
            @Value("${valintalaskentakoostepalvelu.hakemus.rest.url:''}") String applicationResourceUrl) {
        this.applicationResource = applicationResource;
        this.applicationResourceUrl = applicationResourceUrl;
    }

    public Hakemus haeHakemus(@Property(OPH.HAKEMUSOID) String hakemusOid) {
        LOG.info("Haetaan hakemus osoitteesta {}/applications/{}", new Object[] { applicationResourceUrl, hakemusOid });
        return applicationResource.getApplicationByOid(hakemusOid);
    }
}
