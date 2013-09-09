package fi.vm.sade.valinta.kooste.hakemus.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * User: wuoti
 * Date: 9.9.2013
 * Time: 9.05
 */
@Component("haeHakukohteenHakemuksetKomponentti")
public class HaeHakukohteenHakemuksetKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HaeHakukohteenHakemuksetKomponentti.class);

    @Autowired
    private ApplicationResource applicationResource;

    public List<SuppeaHakemus> haeHaunHakemukset(@Simple("${property.hakukohdeOid}") String hakukohdeOid) {
        LOG.info("Haetaan hakukohteen " + hakukohdeOid + " hakemukset");
        HakemusList hakemusList = applicationResource.findApplications(null, null, null, null, null, hakukohdeOid, 0,
                Integer.MAX_VALUE);

        LOG.info("Haettiin {} kpl hakemuksia", hakemusList.getResults().size());
        return hakemusList.getResults();
    }
}
