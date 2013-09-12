package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import java.util.List;

import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;

/**
 * User: wuoti Date: 9.9.2013 Time: 9.05
 */
@Component("haeHakukohteenHakemuksetKomponentti")
public class HaeHakukohteenHakemuksetKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaeHakukohteenHakemuksetKomponentti.class);

    @Autowired
    private ApplicationResource applicationResource;
    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String hakuAppResourceUrl;

    public List<SuppeaHakemus> haeHakukohteenHakemukset(@Property("hakukohdeOid") String hakukohdeOid) {
        assert (SecurityContextHolder.getContext().getAuthentication() != null); // <-
                                                                                 // helps
                                                                                 // finding
                                                                                 // really
                                                                                 // difficult
                                                                                 // multithread
                                                                                 // bugs
        LOG.info("Haetaan hakukohteen {} hakemukset! Osoitteesta {}", new Object[] { hakukohdeOid, hakuAppResourceUrl });
        HakemusList hakemusList = applicationResource.findApplications(null, null, null, null, null, hakukohdeOid, 0,
                Integer.MAX_VALUE);

        LOG.info("Haettiin {} kpl hakemuksia", hakemusList.getResults().size());
        return hakemusList.getResults();
    }
}
